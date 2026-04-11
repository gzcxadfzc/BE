# Phase 3 구현 계획: 비동기 LLM 처리

> **목적**: initBook / generateWithAi LLM 호출을 SQS + Lambda로 위임, 클라이언트 폴링 기반 완료 감지
> **전제**: SQS FIFO + DLQ, Python Lambda(Mock), Lambda 인프라 모두 완료
> **작성일**: 2026-04-10

---

## 현재 상태 vs 목표

### 현재 (v2 동기)
```
POST /init        → BIP 생성 → LLM 호출(~30초) → 이미지 업로드 → 200 반환
POST /{id}        → Lock 획득 → LLM 호출(~30초) → 이미지 업로드 → 200 반환
completeBook      → Lock 획득 → DB 저장 → 200 반환
```

### 목표 (v3 비동기)
```
POST /init        → BIP 생성 → SQS 발행 → PENDING → 202 즉시 반환 (bipId)
POST /{id}        → PENDING 설정 → SQS 발행 → 202 즉시 반환 (bipId)
GET  /{id}/status → Redis 폴링 → PENDING / COMPLETED(페이지 데이터 포함)
completeBook      → PENDING 상태 체크 → DB 저장 → 200 반환 (Lock 제거)
```

---

## 전제 확인

| 항목 | 상태 | 비고 |
|------|------|------|
| SQS FIFO `littlewriter-bip-generation.fifo` | ✅ 완료 | |
| DLQ `littlewriter-bip-dlq.fifo` | ✅ 완료 | |
| Python Lambda 핸들러 (`lambda/handler.py`) | ✅ 완료 | Mock LLM (sleep + 고정 응답) |
| Lambda IAM / S3 아티팩트 / Event Source Mapping | ✅ 완료 | |
| `BookInProgress.Status.PENDING` (도메인) | ✅ 존재 | `markAsPending()` 메서드 포함 |
| `BookInProgressRedisEntity.Status.PENDING` (Redis) | ❌ 미구현 | **3-A** |
| 메인 앱 SQS 발행 코드 | ❌ 미구현 | **3-B** |
| 서비스 비동기 전환 | ❌ 미구현 | **3-C** |
| 폴링 엔드포인트 | ❌ 미구현 | **3-D** |
| Controller 202 반환 | ❌ 미구현 | **3-E** |
| 기존 코드 정리 | ❌ 미구현 | **3-F** |

---

## Lambda ↔ 메인 앱 계약

### SQS 메시지 페이로드 (메인 앱 → Lambda)
```json
{
  "bipId": "abc123",
  "pageIndex": 0,
  "userInput": "오늘 숲에서 토끼를 만났어",
  "characterName": "토끼 토리",
  "characterDescription": "숲속에 사는 착한 토끼",
  "backgroundInfo": "숲속 친구들의 모험 이야기"
}
```
- `MessageGroupId = bipId` (같은 책 직렬 처리)
- `MessageDeduplicationId = {bipId}-{pageIndex}` (중복 방지)

### Redis 결과 스키마 (Lambda → 메인 앱)
```
key: bip:result:{bipId}
TTL: 600초
value: {
  "pageIndex": 0,
  "context": "...",
  "imageUrl": "https://s3.../bip/{bipId}/page-0.png",
  "questions": ["질문1", "질문2", "질문3"]
}
```

### Pending Guard (메인 앱 설정 / Lambda 해제)
```
key: bip:pending-guard:{bipId}
TTL: Lambda 최대 처리 시간 + 버퍼 (예: 600초)
value: 1  (존재 여부만 확인)
```
- **메인 앱** (`generateWithAi` 임계 구역 내): PENDING 상태 저장 시 guard 키 함께 SET
- **Lambda**: 처리 완료(성공/실패) 후 guard 키 DEL
- **stale 판정**: `status == PENDING` AND `guard 없음` → Lambda 하드 크래시 등 비정상 종료 → IN_PROGRESS 취급

---

## 단계별 구현

### 3-A. `BookInProgressRedisEntity.Status` PENDING 추가

**파일**: `storage/src/main/java/com/pkg/redis/BookInProgressRedisEntity.java`

**변경 내용**:
```java
public enum Status {
    IN_PROGRESS,
    PENDING,      // 추가
    COMPLETED;

    public static Status fromDomain(BookInProgress.Status status) {
        return switch (status) {
            case COMPLETED -> COMPLETED;
            case PENDING   -> PENDING;
            default        -> IN_PROGRESS;
        };
    }

    public static BookInProgress.Status toDomain(Status status) {
        return switch (status) {
            case COMPLETED -> BookInProgress.Status.COMPLETED;
            case PENDING   -> BookInProgress.Status.PENDING;
            default        -> BookInProgress.Status.IN_PROGRESS;
        };
    }
}
```

**테스트**:
- `BookInProgressRedisEntity.Status.fromDomain(PENDING)` → `PENDING`
- `BookInProgressRedisEntity.Status.toDomain(PENDING)` → `BookInProgress.Status.PENDING`
- `BookInProgressRepositoryAdapterTest`: PENDING 상태 저장/조회 검증

**커밋**: `feat(storage): BookInProgressRedisEntity.Status PENDING 추가`

---

### 3-B. SQS 클라이언트 Bean + 발행 포트 구현

#### 3-B-1. 도메인 포트 정의

**신규 파일**: `domain/src/main/java/com/pkg/domain/bookprogress/BookPageQueuePublisher.java`
```java
public interface BookPageQueuePublisher {
    void publish(BookPageQueueMessage message);
}
```

**신규 파일**: `domain/src/main/java/com/pkg/domain/bookprogress/BookPageQueueMessage.java`
```java
public record BookPageQueueMessage(
    String bipId,
    int pageIndex,
    String userInput,
    String characterName,
    String characterDescription,
    String backgroundInfo
) {}
```

#### 3-B-2. 의존성 추가

**파일**: `storage/build.gradle`
```groovy
implementation platform('software.amazon.awssdk:bom:2.25.0')
implementation 'software.amazon.awssdk:sqs'
```

#### 3-B-3. SQS 설정

**신규 파일**: `storage/src/main/java/com/pkg/sqs/SqsConfig.java`
```java
@Configuration
public class SqsConfig {
    @Value("${sqs.region:ap-northeast-2}")
    private String region;

    @Bean
    public SqsClient sqsClient() {
        return SqsClient.builder()
                .region(Region.of(region))
                .build();
    }
}
```

**파일**: `storage/src/main/resources/application.yml` (또는 secret)
```yaml
sqs:
  queue-url: ${SQS_QUEUE_URL}
  region: ap-northeast-2
```

#### 3-B-4. 발행 구현체

**신규 파일**: `storage/src/main/java/com/pkg/sqs/BookPageSqsPublisher.java`
```java
@Component
public class BookPageSqsPublisher implements BookPageQueuePublisher {

    private final SqsClient sqsClient;

    @Value("${sqs.queue-url}")
    private String queueUrl;

    public void publish(BookPageQueueMessage message) {
        String body = toJson(message);
        sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(body)
                .messageGroupId(message.bipId())
                .messageDeduplicationId(message.bipId() + "-" + message.pageIndex())
                .build());
    }
}
```

**테스트**: `BookPageSqsPublisherTest` (단위 테스트, `SqsClient` Mock)

**커밋**: `feat(storage): SQS 클라이언트 Bean + BookPageQueuePublisher 구현`

---

### 3-C. `BookProgressService` 비동기 전환

#### 신규 반환 타입

**신규 파일**: `domain/src/main/java/com/pkg/domain/bookprogress/BookPageAccepted.java`
```java
public record BookPageAccepted(String bipId) {}
```

#### `initBook` 변경

```
기존: BIP 생성 → LLM 직접 호출 → 이미지 업로드 → addPage → AiGenerateResult 반환
v3:  BIP 생성 → BIP 저장(IN_PROGRESS) → SQS 발행(pageIndex=0) → BookPageAccepted(bipId) 반환
```

```java
public BookPageAccepted initBook(BookInitCommand command) {
    BookCharacter character = bookCharacterRepository.retrieveById(command.characterId());
    if (character == null) throw BookProgressException.notFound(...);
    BookInProgress bip = BookInProgress.fromCommand(command, character);
    bookInProgressRepository.save(bip);
    queuePublisher.publish(new BookPageQueueMessage(
            bip.id(), 0, command.userInput(),
            character.name(), character.description(), command.background()));
    return new BookPageAccepted(bip.id());
}
```

#### `generateWithAi` 변경

```
기존: lockExecutor.updateWithLock() → LLM 직접 호출 → 이미지 업로드 → addPage
v3:  BIP 조회 → PENDING 상태 확인(중복 방지) → PENDING 저장 → SQS 발행 → BookPageAccepted(bipId) 반환
```

```java
public BookPageAccepted generateWithAi(CreateOnePageCommand command) {
    BookInProgress bip = bookInProgressRepository.retrieveById(command.bipId());
    if (bip == null) throw BookProgressException.notFound(command.bipId());
    if (bip.status() == BookInProgress.Status.PENDING)
        throw BookProgressException.alreadyPending(command.bipId());
    validateOwner(bip, command.currentUser());
    int nextPageIndex = bip.previousPages().size();
    bookInProgressRepository.save(bip.markAsPending());
    queuePublisher.publish(new BookPageQueueMessage(
            bip.id(), nextPageIndex, command.userInput(),
            bip.character().name(), bip.character().description(), bip.backgroundInfo()));
    return new BookPageAccepted(bip.id());
}
```

#### 예외 추가

**파일**: `domain/src/main/java/com/pkg/domain/bookprogress/BookProgressException.java`
```java
public static BookProgressException alreadyPending(String bipId) {
    return new BookProgressException(HttpStatus.CONFLICT, "already pending: " + bipId);
}
```

#### 의존성 정리

`BookProgressService` 생성자에서 제거:
- `BookPageGenerator bookPageGenerator`
- `ImageRepository imageRepository`

유지:
- `BookInProgressLockExecutor lockExecutor` ← race condition 시 userInput이 달라질 수 있으므로 Lock 유지 (docs/lock.md 참고)

추가:
- `BookPageQueuePublisher queuePublisher`

**테스트**:
- `initBook`: SQS 발행 검증, BIP 상태 IN_PROGRESS로 저장됨
- `generateWithAi`: PENDING 상태 전환, SQS 발행, 중복 요청 시 CONFLICT
- `BookPageGenerator`, `ImageRepository` Mock 제거

**커밋**: `feat(domain): BookProgressService 비동기 SQS 전환`

---

### 3-D. 폴링 엔드포인트 구현

#### 3-D-1. 도메인 포트

**신규 파일**: `domain/src/main/java/com/pkg/domain/bookprogress/BookPageResultRepository.java`
```java
public interface BookPageResultRepository {
    Optional<BookPageResult> findResult(String bipId);
    void deleteResult(String bipId);
}
```

**신규 파일**: `domain/src/main/java/com/pkg/domain/bookprogress/BookPageResult.java`
```java
public record BookPageResult(
    int pageIndex,
    String context,
    String imageUrl,
    List<String> questions
) {}
```

#### 3-D-2. 인프라 구현체

**신규 파일**: `storage/src/main/java/com/pkg/redis/BookPageResultRepositoryAdapter.java`
```java
@Component
public class BookPageResultRepositoryAdapter implements BookPageResultRepository {
    // Redis GET bip:result:{bipId} → JSON 역직렬화 → Optional<BookPageResult>
    // Redis DEL bip:result:{bipId}
}
```

#### 3-D-3. 서비스 메서드

**파일**: `domain/src/main/java/com/pkg/domain/bookprogress/BookProgressService.java`
```java
public BookPagePollResult pollPageResult(Actor user, String bipId) {
    BookInProgress bip = retrieveById(user, bipId);
    Optional<BookPageResult> result = bookPageResultRepository.findResult(bipId);
    if (result.isEmpty()) {
        return BookPagePollResult.pending();
    }
    BookPageResult page = result.get();
    BookInProgress updated = bip.addBookPage(
            new BookPage(page.context(), page.imageUrl(), page.pageIndex()));
    bookInProgressRepository.save(updated);      // DB 저장 먼저
    bookPageResultRepository.deleteResult(bipId); // 성공 후 DEL
    return BookPagePollResult.completed(page);
}
```

**신규 파일**: `domain/src/main/java/com/pkg/domain/bookprogress/BookPagePollResult.java`
```java
public record BookPagePollResult(
    String status,   // "PENDING" | "COMPLETED"
    BookPageResult page  // COMPLETED일 때만 non-null
) {
    public static BookPagePollResult pending() {
        return new BookPagePollResult("PENDING", null);
    }
    public static BookPagePollResult completed(BookPageResult page) {
        return new BookPagePollResult("COMPLETED", page);
    }
}
```

**멱등성 처리**: DB 저장 성공 후 DEL. 저장 실패 시 Redis 키 유지 → 다음 폴링에서 재시도.

#### 3-D-4. Controller 엔드포인트

```java
// GET /api/v1/book/progress/{id}/status
@GetMapping("/{id}/status")
public ResponseEntity<ApiResponse<BookPageStatusResponse>> getPageStatus(
        @Authenticated Actor currentUser,
        @PathVariable String id) {
    BookPagePollResult result = bookProgressService.pollPageResult(currentUser, id);
    return ResponseEntity.ok(ApiResponse.success(BookPageStatusResponse.from(result)));
}
```

**응답 DTO**: `BookPageStatusResponse`
```json
// PENDING
{ "status": "PENDING" }

// COMPLETED
{
  "status": "COMPLETED",
  "page": {
    "context": "...",
    "imageUrl": "https://s3.../bip/{bipId}/page-0.png",
    "questions": ["질문1", "질문2", "질문3"]
  }
}
```

**테스트**:
- 결과 없음 → PENDING 반환
- 결과 있음 → DB addPage 후 Redis DEL, COMPLETED 반환
- DB 저장 실패 시 Redis 키 유지 (멱등성 검증)

**커밋**: `feat: 폴링 엔드포인트 구현 (GET /{id}/status)`

---

### 3-E. Controller 202 반환 전환

**파일**: `api/src/main/java/com/pkg/controller/bookprogress/BookProgressController.java`

```java
// initBook: 202 Accepted
@PostMapping("/init")
public ResponseEntity<ApiResponse<BookPageAcceptedResponse>> initBook(...) {
    BookPageAccepted accepted = bookProgressService.initBook(command);
    return ResponseEntity.accepted()
            .body(ApiResponse.success(BookPageAcceptedResponse.from(accepted)));
}

// generatePage: 202 Accepted
@PostMapping("/{id}")
public ResponseEntity<ApiResponse<BookPageAcceptedResponse>> generatePage(...) {
    BookPageAccepted accepted = bookProgressService.generateWithAi(command);
    return ResponseEntity.accepted()
            .body(ApiResponse.success(BookPageAcceptedResponse.from(accepted)));
}
```

**신규 DTO**: `BookPageAcceptedResponse(String bipId)` (클라이언트가 폴링에 사용)

**영향 없는 엔드포인트**: `completeBook` (200 유지), `retrieveBookInProgress` (200 유지)

**커밋**: 3-C와 동일 커밋 또는 별도 커밋 선택

---

### 3-G. Pending Guard 구현

Lambda 하드 크래시 시 BIP가 PENDING에 영구 고착되는 문제를 TTL 기반 guard 키로 방지.

#### 설계 원칙
- guard 키는 **Lock 임계 구역 내부**에서만 설정 → Lock 보유자만 접근 가능
- guard 키 설정과 PENDING 상태 저장은 동일 임계 구역에서 순차 실행 (별도 명령, Lua 불필요)
  - 둘 사이 크래시: PENDING O + guard X → stale 판정으로 자동 복구
- Lambda 명시적 처리(try/finally)가 1차 방어, TTL 만료가 백스톱

#### 3-G-1. Guard 관리 포트 정의

**신규 파일**: `domain/src/main/java/com/pkg/domain/bookprogress/BookInProgressPendingGuard.java`
```java
public interface BookInProgressPendingGuard {
    void set(String bipId);
    void delete(String bipId);
    boolean exists(String bipId);
}
```

#### 3-G-2. 인프라 구현체

**신규 파일**: `storage/src/main/java/com/pkg/redis/BookInProgressPendingGuardAdapter.java`
```java
@Component
public class BookInProgressPendingGuardAdapter implements BookInProgressPendingGuard {

    private static final String GUARD_KEY_PREFIX = "bip:pending-guard:";
    private static final long GUARD_TTL_SEC = 600L; // Lambda max + buffer

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void set(String bipId) {
        redisTemplate.opsForValue()
                .set(GUARD_KEY_PREFIX + bipId, "1", GUARD_TTL_SEC, TimeUnit.SECONDS);
    }

    @Override
    public void delete(String bipId) {
        redisTemplate.delete(GUARD_KEY_PREFIX + bipId);
    }

    @Override
    public boolean exists(String bipId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(GUARD_KEY_PREFIX + bipId));
    }
}
```

#### 3-G-3. `generateWithAi` 내 guard 적용

**파일**: `domain/src/main/java/com/pkg/domain/bookprogress/BookProgressService.java`

```java
// 변경 전 (PENDING 체크만)
if (bip.status() == BookInProgress.Status.PENDING) {
    throw BookProgressException.alreadyPending(command.bipId());
}
bookInProgressRepository.save(bip.markAsPending());

// 변경 후 (stale PENDING 자동 복구 + guard 설정)
if (bip.status() == BookInProgress.Status.PENDING) {
    if (pendingGuard.exists(bip.id())) {
        throw BookProgressException.alreadyPending(command.bipId()); // 진짜 처리 중
    }
    // guard 없음 → stale PENDING → IN_PROGRESS 취급, 진행
}
bookInProgressRepository.save(bip.markAsPending());
pendingGuard.set(bip.id()); // Lock 임계 구역 내부, PENDING 저장 직후
```

#### 3-G-4. Lambda 계약 변경 (Python)

Lambda는 처리 완료(성공/실패) 후 반드시 guard 키를 삭제:
```python
try:
    result = generate_page(event)
    update_bip_result(bip_id, result)       # bip:result:{bipId} SET
finally:
    redis.delete(f"bip:pending-guard:{bip_id}")  # guard 무조건 삭제
```

#### 테스트
- guard 있을 때 PENDING → 409
- guard 없을 때 PENDING (stale) → IN_PROGRESS 취급, 정상 진행
- PENDING 저장 후 guard SET 확인
- Lambda 완료 후 guard DEL 확인

**커밋**: `feat(domain+storage): Pending Guard로 stale PENDING 자동 복구`

---

### 3-F. 기존 코드 정리

#### 삭제 대상

| 파일 | 이유 |
|------|------|
| `storage/.../s3/ImageUploadEventHandler.java` | Lambda가 이미지 직접 처리 (S3 deterministic key) |
| `storage/.../s3/ImageUploadEvent.java` | 발행처 포함 삭제 |
| `domain/.../bookprogress/BookPageGenerator.java` (인터페이스) | 서비스에서 미사용 |
| `domain/.../image/ImageRepository.java` (인터페이스) | 서비스에서 미사용 |
| `ai/` 모듈 내 `BookPageGeneratorAdapter` 등 | 사용처 없어지면 삭제 |

> `BookInProgressLockExecutor` 및 `BookInProgressLockExecutorAdapter`는 **유지** (docs/lock.md 참고).
> `ai/` 모듈 전체 삭제 여부는 `build.gradle` 의존 관계 확인 후 결정.

#### 수정 대상

| 파일 | 변경 |
|------|------|
| `storage/.../s3/S3AsyncConfig.java` | `transaction-event` Bean 제거 (사용처 없음) |
| `storage/.../redis/BookCompleteEventHandler.java` | `@Async("transaction-event")` 제거 → 동기 실행 (Redis SET은 빠름) |
| `storage/.../jpa/BookRepositoryAdapter.java` | `ImageUploadEvent` 발행 코드 제거, `PreAssignedUrl` URL 변환 로직 제거 (Lambda가 S3 직접 씀) |
| `domain/.../bookprogress/BookCompleteExecutor.java` | `saveWithLock()` 제거 여부 별도 검토 (docs/lock.md 참고) |

#### `BookRepositoryAdapter.saveFrom()` 단순화

```
기존: PreAssignedUrl 변환 → ImageUploadEvent 발행 → BookCompleteEvent 발행 → DB 저장
v3:  BookCompleteEvent 발행 → DB 저장
     (PreAssignedUrl 변환, ImageUploadEvent 발행 제거 — Lambda가 S3에 직접 씀)
```

**테스트**: 삭제된 클래스를 참조하는 기존 테스트 정리, 통합 테스트 전체 재검증

**커밋**: `refactor: ImageUploadHandler 제거, SQS 전환 후 미사용 코드 정리`

---

## 커밋 순서 요약

```
커밋 1 (storage)            3-A — BookInProgressRedisEntity.Status PENDING 추가
커밋 2 (domain+storage)     3-B — SQS 포트 정의 + 클라이언트 Bean + 발행 구현체
커밋 3 (domain+api)         3-C + 3-E — 서비스 비동기 전환 + Controller 202 반환
커밋 4 (domain+storage+api) 3-D — 폴링 엔드포인트
커밋 5 (domain+storage)     3-G — Pending Guard (stale PENDING 자동 복구)
커밋 6 (전 모듈)            3-F — 미사용 코드 정리
```

각 커밋은 단위 테스트 + 통합 테스트를 통과한 상태로 유지.
3-F는 모든 기능 커밋(3-A~3-E) 완료 후 진행.

---

## 미결 사항

| # | 질문 | 결정 필요 |
|---|------|---------|
| M1 | `AiGenerateResult` 기존 타입 유지 vs 삭제 | `BookPageAccepted`로 완전 대체 시 AI 모듈 API 테스트 영향 확인 필요 |
| M2 | `ai/` 모듈 전체 삭제 여부 | `BookPageGenerator` 인터페이스 사용처 모두 제거 후 결정 |
| M3 | `completeBook`의 PENDING 상태 체크 방식 | PENDING이면 CONFLICT(409) 반환 or 완료 대기? |
| M4 | 폴링 응답의 HTTP 상태 코드 | PENDING → 200 or 202 반환 (클라이언트 계약 확인) |
