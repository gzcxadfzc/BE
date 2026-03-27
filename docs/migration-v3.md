# LittleWriter v3 마이그레이션 계획

> **목적**: LLM 호출 파이프라인의 안정성 확보, 사용자 경험 개선, Scale-out 대응, 멀티 프로바이더 LLM Gateway 전환
> **기반 문서**: `lock-network-io-performance-analysis.md`
> **작성일**: 2026-03-27

---

## 1. 현재 상태 요약 및 문제점

### 1.1 핵심 문제

| # | 문제 | 영향 |
|---|------|------|
| P1 | Redis Lock을 LLM 호출 동안(최대 30초) 보유 | HTTP 커넥션 블로킹, 클라이언트 타임아웃 |
| P2 | Lock TTL(120초) 초과 시 동시 처리 가능 | 데이터 덮어쓰기, 페이지 중복 |
| P3 | Lock 해제 로직 non-atomic (GET → DEL) | Race condition 발생 가능 |
| P4 | completeBook: DB 커밋 후 Redis 업데이트 실패 | DB/Redis 상태 불일치 |
| P5 | 이미지 업로드 이벤트 실패 시 복구 없음 | 이미지 영구 손실 가능 |
| P6 | LLM 재시도 시 멱등성 없음 | 페이지 중복 생성 |
| P7 | Scale-out 시 실시간 알림 라우팅 불가 | 멀티 인스턴스 환경에서 클라이언트 알림 불가 |
| P8 | LLM 호출이 OpenAI 단일 프로바이더에 하드코딩 | 장애 시 전체 서비스 중단, 프로바이더 교체 불가 |

### 1.2 근본 원인

`BookInProgress`에는 지켜져야 할 불변식이 있다:

```
"같은 책에 대해 한 번에 하나의 페이지만 생성되어야 한다"
```

현재 이 불변식 보호 책임이 **Redis Lock(인프라)** 에 위임되어 있다.
인프라는 언제든 실패할 수 있으므로, Redis Lock의 결함(TTL 만료, non-atomic 해제, 장시간 보유)이
도메인 불변식 위반으로 직결된다.

```
도메인 불변식 보호
        │
        ▼
    Redis Lock        ← 인프라
        │
   ┌────┴─────────────────────┐
   │ 실패 가능한 지점들        │
   │  - TTL 만료 (P2)         │
   │  - non-atomic GET→DEL(P3)│
   │  - LLM 30초 보유 (P1)    │
   └────┬─────────────────────┘
        │ 실패 시
        ▼
  도메인 불변식 위반
  (페이지 중복, 상태 오염)
```

v3 마이그레이션의 본질은 성능/안정성 개선이 아니라,
**인프라에 위임된 도메인 불변식 보호 책임을 도메인 레이어로 되돌리는 것**이다.

```
v2: 불변식 = Redis Lock이 보호  → Lock 실패 = 불변식 위반 가능

v3: 불변식 = PENDING 상태가 표현 → 도메인 상태 자체가 중복 처리 차단
    SQS FIFO                     → 인프라 보조 (직렬 큐잉)
    Redis                        → 캐시/신호 역할로 격리
```

추가적인 구조적 문제:

```
HTTP 연결  = LLM 처리 시간만큼 유지 (동기 응답)
LLM 어댑터 = OpenAiApi 직접 의존, 프로바이더 교체 불가
```

필요: 비동기 작업 위임 + 도메인 상태 기반 멱등성 + 알림 추상화 + LLM Gateway 추상화

---

## 2. v3 목표 아키텍처

### 2.1 전체 구조

```
                         ┌─────────────────────────────────────┐
                         │           Client                     │
                         └────┬──────────────┬─────────────────┘
                              │ POST          │ SSE [선택] 또는 폴링
                              ▼              ▼
                    ┌─────────────────────────────────────┐
                    │         Spring Boot API              │
                    │  - REST endpoint (즉시 202 반환)     │
                    │  - GET 폴링 endpoint (상태 조회)      │
                    │  - SSE endpoint [선택]               │
                    │  - BookProgressNotifier 구독          │
                    └──────┬──────────────────────────────┘
                           │ SQS Publish
                           ▼
                    ┌──────────────────┐
                    │   SQS FIFO Queue │  MessageGroupId = bipId
                    │  (MessageDedup)  │  MessageDeduplicationId = bipId+pageIdx
                    └──────┬───────────┘
                           │
                           ▼
                    ┌──────────────────────────────────┐
                    │  Java Lambda Consumer             │
                    │  - 멱등성 검증                    │
                    │  - LLMGateway.chatWithFallback()  │  ← LLM Gateway
                    │  - 이미지 업로드                  │
                    │  - 결과 저장                      │
                    └──────┬───────────────────────────┘
                           │                │ fallback
                           │         ┌──────▼──────────────────────────┐
                           │         │  LLM Gateway                    │
                           │         │  OpenAiLLMProvider (primary)    │
                           │         │  ClaudeLLMProvider  (fallback)  │
                           │         └─────────────────────────────────┘
                           │ Redis PUBLISH
                           ▼
                    ┌──────────────────┐     ┌──────────────────────┐
                    │      Redis        │────▶│  Spring Boot (모든   │
                    │  Pub/Sub channel  │     │  인스턴스가 구독)     │
                    │  Idempotency Key  │     └──────────────────────┘
                    │  BookInProgress   │              │ SSE push
                    └──────────────────┘              ▼
                                                   Client
```

### 2.2 LLM Gateway 설계

#### 공통 모델 (프로바이더 중립)

```java
public record LLMRequest(List<LLMMessage> messages, LLMOptions options) {}
public record LLMResponse(String content, TokenUsage usage) {}
public record LLMMessage(String role, String content) {}
public record TokenUsage(int promptTokens, int completionTokens) {}
```

OpenAI 전용인 `ChatRequest` / `ChatResponse`는 각 프로바이더 구현체 내부로 격리.

#### LLMProvider 인터페이스

```java
public interface LLMProvider {
    LLMResponse chat(LLMRequest request);
    String name(); // "openai", "claude"
}
```

#### 구현체

| 구현체 | 역할 |
|--------|------|
| `OpenAiLLMProvider` | 기존 `OpenAiApi` 래핑, `ChatRequest` ↔ `LLMRequest` 변환 |
| `ClaudeLLMProvider` | Claude API 연동 (Phase 2 이후 추가 가능) |

#### LLMGateway (단일 진입점)

```java
@Component
public class LLMGateway {
    // 특정 프로바이더 지정
    public LLMResponse chat(LLMRequest request, String providerName) { ... }

    // 폴백 체인: 앞 프로바이더 실패 시 순서대로 시도
    public LLMResponse chatWithFallback(LLMRequest request, List<String> fallbackChain) {
        for (String name : fallbackChain) {
            try { return providers.get(name).chat(request); }
            catch (Exception e) { /* 다음 프로바이더로 */ }
        }
        throw new LLMGatewayException("All providers failed");
    }
}
```

Lambda에서의 호출 예시:

```java
// OpenAI 실패 시 Claude로 자동 폴백
LLMResponse response = llmGateway.chatWithFallback(request, List.of("openai", "claude"));
```

#### 기존 도메인 어댑터 변경 방향

```java
// Before: OpenAiApi 직접 의존
public class OpenAiContextQuestionGenerator {
    private final OpenAiApi openAiApi;
}

// After: LLMGateway 경유
public class ContextQuestionGenerator {
    private final LLMGateway llmGateway;
}
```

### 2.3 알림 추상화 설계 (BookProgressNotifier)

SSE를 나중에 교체/추가할 수 있도록 알림 메커니즘을 인터페이스로 분리한다.

```java
public interface BookProgressNotifier {
    void notify(String bipId, BookProgressResult result);
}
```

#### Phase 3 기본 구현 (폴링 기반)

```java
@Component
public class NoOpBookProgressNotifier implements BookProgressNotifier {
    // Lambda가 Redis PUBLISH는 항상 수행함
    // Spring Boot에서는 아무것도 하지 않음 → 클라이언트가 폴링으로 확인
    public void notify(String bipId, BookProgressResult result) {}
}
```

클라이언트 폴링 흐름:

```
POST → 202 수신
  → GET /api/v1/book/progress/{id} 를 N초 간격으로 폴링
      PENDING     → 계속 폴링
      IN_PROGRESS → 페이지 추가됨, 다음 단계 진행
```

#### Phase 4 SSE 구현 [선택]

```java
@Component
@ConditionalOnProperty("feature.sse.enabled")
public class SseBookProgressNotifier implements BookProgressNotifier {
    private final SseEmitterRegistry registry;

    public void notify(String bipId, BookProgressResult result) {
        registry.findEmitter(bipId)
                .ifPresent(emitter -> emitter.send(result));
    }
}
```

`BookProgressNotifier` Bean을 교체하는 것만으로 SSE 활성화. 폴링 엔드포인트는 그대로 유지.

#### Lambda는 항상 Redis PUBLISH (Phase 3에 포함)

Lambda Step 10의 `Redis PUBLISH bip:result:{bipId}`는 SSE 여부와 무관하게 Phase 3에서 구현한다.

```
Lambda (Phase 3)
  └─ Redis PUBLISH bip:result:{bipId}   ← 항상 수행

Spring Boot (Phase 3)
  └─ NoOpBookProgressNotifier           ← Redis 메시지 무시, 클라이언트는 폴링

Spring Boot (Phase 4, 선택)
  └─ SseBookProgressNotifier            ← Redis 구독 → SSE push
```

이 구조 덕분에 Phase 4 추가 시 **Lambda 코드 변경 없음**.

---

### 2.5 BookInProgress 상태 머신 (변경)

```
현재:  IN_PROGRESS ──────────────────────────────▶ COMPLETED

v3:    IN_PROGRESS ──[SQS 발행]──▶ PENDING
                                       │
                                       │ Lambda 처리 완료
                                       ▼
                               IN_PROGRESS (페이지 추가됨)
                                       │
                                       │ completeBook
                                       ▼
                                   COMPLETED
```

`PENDING` 상태는 이미 도메인에 존재하므로 모델 변경 없음.

### 2.6 멱등성 설계

```
idempotency:{messageId} 상태값:

NOT EXISTS  → 최초 처리
"IN_FLIGHT" → Lambda 진입 후 LLM 호출 직전 마킹 (TTL 5분)
{llmResult} → LLM 완료, 저장 전 크래시 대비 캐싱 (TTL 1시간)
"DONE"      → 완전히 처리됨 (TTL 24시간)
```

Lambda retry 시 흐름:

```
Lambda 진입
  ├─ DONE       → ack, 종료
  ├─ {llmResult} → LLM 스킵, 이미지 업로드부터 재개
  ├─ IN_FLIGHT   → FIFO이므로 발생하지 않음 (방어 목적)
  └─ NOT EXISTS  → 정상 처리 시작
```

---

## 3. 마이그레이션 단계

### Phase 1: 즉시 안정성 수정 (SQS 없이 적용 가능)

> 목표: 현재 아키텍처 내 P3, P4, P5 해결

#### 1-1. Lock 해제 atomic 처리

`RedisLockManager.releaseLock()` 의 GET → DEL 패턴을 Lua script로 교체.

```lua
-- 조건부 삭제: 내 lockId일 때만 DEL
if redis.call("GET", KEYS[1]) == ARGV[1] then
    return redis.call("DEL", KEYS[1])
else
    return 0
end
```

- 적용 범위: `RedisLockManager` 내 `releaseLock()` 메서드 1곳
- 주의: lock 보유 중 Lua 실행이 아닌 해제 시점에만 사용 → 병목 없음

#### 1-2. completeBook DB/Redis 정합성

현재: DB 커밋 → Redis 업데이트 실패 시 불일치

Redis는 Spring 트랜잭션에 참여하지 않으므로 `@Transactional` 내부에서 직접 호출하면
DB 롤백 시에도 Redis는 되돌아가지 않는다. `@TransactionalEventListener(AFTER_COMMIT)`을
사용해 DB 커밋 이후에만 Redis를 호출한다.

```java
@Transactional
public void completeBook(String bipId) {
    bookRepository.save(completed);
    eventPublisher.publishEvent(new BookCompletedEvent(bipId));
    // Redis는 여기서 호출하지 않음
}

@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onBookCompleted(BookCompletedEvent event) {
    try {
        redis.markAsCompleted(event.getBipId());
    } catch (Exception e) {
        // Redis는 캐시 역할이므로 실패 시 다음 조회에서 DB 기반으로 자연 복구
        log.warn("Redis sync failed for {}, will recover on next read", event.getBipId());
    }
}
```

```
DB 커밋 성공
    ↓ (AFTER_COMMIT 이후 실행)
redis.markAsCompleted()   ← DB 커밋 보장된 상태

DB 커밋 실패
    → 이벤트 실행 자체가 없음 → Redis 호출 없음
```

Redis가 순수 캐시(source of truth = DB)이므로 Redis 반영 실패 시
다음 조회에서 DB를 읽어 자연 복구된다. Outbox Pattern은 이 케이스에 과잉.

#### 1-3. 이미지 업로드 이벤트 재시도

`@TransactionalEventListener` 실패 시 복구 로직 추가.

```
실패 시 → Redis에 재시도 대상 URL 저장
         → 스케줄러(1분 간격)가 미처리 URL 재처리
         → 최대 3회 재시도 후 알림 로그
```

---

### Phase 2: LLM Gateway 구현 (Phase 2 선행 작업)

> 목표: P8 해결. Lambda 구현 전 Spring Boot `ai` 모듈 내 리팩터링으로 완료.
> Lambda 코드는 이 모듈을 그대로 포팅하므로 선행 완료 필수.

#### 2-0-1. 공통 모델 추가

`ai` 모듈 내 `com.pkg.core` 패키지에 추가:

```
LLMRequest, LLMResponse, LLMMessage, LLMOptions, TokenUsage
```

#### 2-0-2. LLMProvider 인터페이스 및 구현체

```
LLMProvider (interface)
  └─ OpenAiLLMProvider   ← 기존 OpenAiApi 래핑
  └─ ClaudeLLMProvider   ← 추후 추가
```

#### 2-0-3. LLMGateway 구현

`chatWithFallback()` 포함. Spring Bean으로 등록.

#### 2-0-4. 기존 도메인 어댑터 전환

| 어댑터 | 변경 내용 |
|--------|---------|
| `OpenAiContextQuestionGenerator` | `LLMGateway` 사용으로 전환, 클래스명에서 OpenAi 제거 |
| `OpenAiContextDepictGenerator` | 동일 |
| `OpenAiBookPageGenerator` | 동일 |
| `OpenAIBookCharacterGenerator` | ImageProvider 인터페이스로 분리 (이미지는 별도) |

---

### Phase 3: 비동기 LLM 처리 (핵심 변경)

> 목표: P1, P2, P6 해결. Redis Lock 제거.

#### 3-1. SQS FIFO 큐 설정

```
Queue 이름: littlewriter-bip-generation.fifo
MessageGroupId: {bipId}           ← 같은 책의 페이지 직렬 처리
MessageDeduplicationId: {bipId}-{pageIndex}  ← 클라이언트 중복 요청 방지
Visibility Timeout: 300초         ← LLM 최대 처리 시간 + 여유
DLQ: littlewriter-bip-dlq.fifo    ← 3회 실패 시 이동
```

#### 3-2. API 변경 (generatePage)

```
현재: POST /api/v1/book/progress/{id}
      → Lock 획득 → LLM 호출(30초) → 응답

v3:   POST /api/v1/book/progress/{id}
      → BookInProgress 상태를 PENDING으로 변경
      → SQS 발행
      → 202 Accepted 즉시 반환 (jobId 포함)
```

`initBook` 도 동일하게 비동기 처리.

#### 3-3. Java Lambda Consumer 처리 흐름

> **런타임**: Java 21 (SnapStart 활성화 권장, 콜드 스타트 최소화)
> **의존성**: Phase 2에서 완성된 `ai` 모듈을 그대로 포팅

```
1. SQS 메시지 수신 (messageId, bipId, pageIndex, userInput)

2. idempotency:{messageId} 확인
   ├─ DONE       → ack 후 종료
   └─ {llmResult} → 3번 스킵, 4번부터

3. SET idempotency:{messageId} "IN_FLIGHT" NX EX 300

4. BookInProgress 조회 및 상태 검증
   - PENDING이 아니면 ack 후 종료 (이미 다른 경로로 처리됨)

5. LLMGateway.chatWithFallback(request, ["openai", "claude"])
   - OpenAI 실패 시 Claude로 자동 폴백
   - 폴백 성공 시 DLQ 진입 없이 처리 완료
   - 양쪽 모두 실패 시에만 Lambda retry → DLQ 진입

6. SET idempotency:{messageId} {llmText, tempImageUrl} EX 3600
   (크래시 대비 LLM 결과 즉시 저장)

7. 이미지 업로드 (S3 deterministic key: {bipId}/{messageId}.png)
   (같은 키로 재업로드해도 무해 → 자연스러운 멱등)

8. BookInProgress에 페이지 추가 + status = IN_PROGRESS

9. SET idempotency:{messageId} "DONE" EX 86400

10. Redis PUBLISH bip:result:{bipId} {result}

11. SQS ack (메시지 삭제)
```

#### 3-4. Redis Lock 제거 범위

| 현재 Lock | v3 대체 수단 | 제거 여부 |
|-----------|-------------|---------|
| generateWithAi Lock | SQS FIFO MessageGroupId | 제거 |
| completeBook Lock | PENDING 상태 체크 | 제거 (상태 기반 방어로 대체) |

`BookInProgressLockExecutor` 인터페이스는 유지하되 구현체를 no-op으로 교체 후 점진적 제거.

---

### Phase 4: 실시간 UX — SSE [선택]

> 목표: P7 해결, 사용자 경험 개선
> **전제**: Phase 3 완료 후 독립적으로 추가 가능. 미적용 시 클라이언트는 폴링으로 동작.
> **확장 방식**: `NoOpBookProgressNotifier` → `SseBookProgressNotifier` Bean 교체만으로 활성화. Lambda 변경 없음.

#### 4-1. SSE 엔드포인트

```
GET /api/v1/book/progress/{id}/stream
  → SseEmitter 생성 (timeout: 35초)
  → emitterRegistry.register(bipId, emitter)
  → Redis channel bip:result:{bipId} 구독
  → Lambda 완료 시 push → SSE 이벤트 전송
```

#### 4-2. SseEmitter 레지스트리

```
ConcurrentHashMap<String, SseEmitter>
  - key: bipId
  - 연결 끊김 시 onCompletion/onTimeout 으로 자동 제거
  - 스레드: Tomcat NIO가 관리 (스레드 점유 없음)
```

#### 4-3. 멀티 인스턴스 라우팅 (Scale-out 핵심)

```
Lambda → Redis PUBLISH bip:result:{bipId} {result}

Spring Boot Instance A (emitter 있음)
  → 구독 중 → emitter.send(result) → Client

Spring Boot Instance B (emitter 없음)
  → 구독 중 → emitter 없음 → 무시
```

Redis Pub/Sub이 인스턴스 간 라우팅을 자동 처리. 추가 인프라 불필요.

#### 4-4. 연결 끊김 처리

```
SSE 연결 끊김 (네트워크, 앱 백그라운드)
  → 클라이언트 재연결 시 GET /api/v1/book/progress/{id} 폴링
  → PENDING이면 재구독, IN_PROGRESS(페이지 추가됨)이면 결과 수신
```

Lambda 처리는 SSE 연결과 무관하게 계속 진행됨.

---

## 4. 컴포넌트별 변경 범위

| 컴포넌트 | 변경 수준 | 내용 |
|----------|---------|------|
| `LLMProvider` (신규) | 신규 | 프로바이더 중립 인터페이스, `OpenAiLLMProvider` 구현 |
| `LLMGateway` (신규) | 신규 | 단일 진입점, `chatWithFallback()` 폴백 체인 |
| `LLMRequest/LLMResponse` (신규) | 신규 | 프로바이더 중립 공통 모델 |
| 기존 도메인 어댑터 | 중 | `OpenAiApi` 직접 의존 → `LLMGateway` 경유로 전환 |
| `BookProgressController` | 중 | generatePage → 202 반환, SSE 엔드포인트 추가 |
| `BookProgressService` | 대 | SQS 발행 로직, PENDING 상태 처리 |
| `BookInProgressLockExecutorAdapter` | 소 | no-op 구현으로 교체 후 제거 |
| `RedisLockManager` | 소 | releaseLock Lua 적용 (Phase 1) |
| `BookCompleteExecutor` | 소 | Lock 제거, 상태 검증으로 대체 |
| `BookProgressNotifier` (신규) | 신규 | 알림 추상화 인터페이스, Phase 3에서 NoOp 구현 |
| Java Lambda (신규) | 신규 | SQS consumer, `LLMGateway` 사용, 멱등성 처리, Redis PUBLISH |
| `SseEmitterRegistry` (신규, 선택) | 신규 | emitter 관리 — Phase 4에서만 구현 |
| `SseBookProgressNotifier` (신규, 선택) | 신규 | Redis Pub/Sub 구독 → SSE push — Phase 4에서만 구현 |
| `BookInProgress` | 없음 | PENDING 상태 이미 존재 |

---

## 5. 단계별 적용 순서 및 우선순위

```
Phase 1 (즉시, 독립 적용 가능)
  ├─ Lock 해제 atomic 처리     ← 현재 race condition 방지
  ├─ completeBook 정합성 수정  ← 데이터 불일치 방지
  └─ 이미지 업로드 재시도      ← 이미지 손실 방지

Phase 2 (Lambda 구현 선행 조건, Phase 1과 병행 가능)
  ├─ 공통 LLM 모델 추가 (LLMRequest, LLMResponse)
  ├─ LLMProvider 인터페이스 + OpenAiLLMProvider 구현
  ├─ LLMGateway 구현 (chatWithFallback 포함)
  └─ 기존 도메인 어댑터 LLMGateway 전환

Phase 3 (핵심, 이것만으로 기능 완성)
  ├─ BookProgressNotifier 인터페이스 + NoOpBookProgressNotifier 구현
  ├─ SQS FIFO 큐 생성
  ├─ Java Lambda consumer 구현 (Phase 2 ai 모듈 포팅 + Redis PUBLISH 포함)
  ├─ API 202 변환
  └─ 멱등성 키 설계 및 구현

Phase 4 [선택, 언제든 독립 추가 가능]
  ├─ SseEmitterRegistry 구현
  ├─ SseBookProgressNotifier 구현 (Bean 교체)
  ├─ SSE 엔드포인트 추가
  └─ Redis Pub/Sub 구독 연동
       ※ Lambda 재배포 불필요
```

Phase 1은 현재 시스템 위에서 독립적으로 적용 가능.
Phase 2는 Spring Boot `ai` 모듈 내 작업으로 Phase 1과 병행 가능.
Phase 3은 Phase 2 완료 후 진행. Phase 3만으로 기능 완성 (클라이언트 폴링).
Phase 4는 선택사항. Phase 3 완료 후 언제든 독립적으로 추가 가능. Lambda 재배포 불필요.

---

## 6. 미결 판단 사항 (피드백 필요)

| # | 질문 | 선택지 |
|---|------|--------|
| Q1 | initBook도 비동기로 처리할지 | 동기 유지 (첫 페이지는 즉각 응답) vs 비동기 |
| Q2 | Lambda DLQ 도착 시 처리 방법 | LLM Gateway 폴백으로 진입 빈도 감소 예상. 도달 시 운영자 알림 + 수동 재처리 vs 자동 복구 |
| Q3 | idempotency key 저장소 | Redis (현재 인프라 재사용) vs DynamoDB |
| Q4 | SSE 연결 없이 처리 완료된 경우 | Phase 3 기본: 클라이언트 폴링. Phase 4 적용 시 SSE로 자동 전환 |
| Q5 | completeBook은 동기 유지할지 | 처리 시간이 짧으므로(60ms) 동기 유지 권장 |
| Q6 | LLM Gateway 폴백 프로바이더 우선순위 | OpenAI → Claude (비용 기준) vs 응답속도 기준 라우팅 |
