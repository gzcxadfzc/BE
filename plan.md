# 아티클 분석 및 적용 계획

> 참고 아티클: [SQS 기반 알림톡 처리에서 발생한 DB 커넥션 데드락 분석기](https://oliveyoung.tech/2025-12-30/alimtalk_improve_event_driven_architecture/)

---

## 아티클 요약

올리브영이 SQS 기반 비동기 알림 시스템에서 겪은 **DB 커넥션 데드락** 장애 분석 및 해결기.

**원인은 세 요소의 결합:**
| 요소 | 내용 |
|------|------|
| SQS 병렬 처리 | 다수의 스레드가 동시에 메시지 처리 |
| HikariCP 풀 제한 | 가용 커넥션 수 부족 |
| `REQUIRES_NEW` 전파 | 스레드당 커넥션 2개 필요 (부모 + 자식 트랜잭션) |

**데드락 공식:** `Tn × (Cm - 1) + 1`
- `Tn` = 동시 처리 스레드 수
- `Cm` = 스레드당 필요 커넥션 수
- 예: 스레드 10개, 커넥션 2개/스레드 → 최소 커넥션 19개 필요

**해결책:** REQUIRES_NEW 제거, SQS 배치 크기 조정, 이벤트 기반으로 알림 로직 분리

---

## 현재 프로젝트 유사 구조 분석

### 유사한 패턴 존재 여부: **YES**

현재 프로젝트도 **비동기 스레드 풀 + HikariCP + 클래스 레벨 @Transactional** 조합을 사용 중이며,
아티클의 데드락 시나리오와 구조적으로 동일한 위험 요소를 내포하고 있다.

### 핵심 코드: `BookRepositoryAdapter`

```java
// storage/src/main/java/com/pkg/jpa/BookRepositoryAdapter.java

@Component
@Transactional(transactionManager = "storageTransactionManager") // ← 클래스 레벨 @Transactional
public class BookRepositoryAdapter implements BookRepository {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async(value = "transaction-event")                            // ← 비동기 스레드 풀에서 실행
    public void handle(ImageUploadEvent event) {
        for (PreAssignedUrl url : event.jobs()) {
            imageUploader.copyToBookStorage(url);                  // ← S3 작업만 수행 (DB 없음)
        }
    }
}
```

### 문제점 분석

#### 1. 클래스 레벨 `@Transactional`이 `handle()`에 적용됨
- `handle()`은 S3 복사만 수행하는데, 클래스 레벨 `@Transactional` 때문에
  Spring AOP가 메서드 실행 시 트랜잭션(= DB 커넥션)을 획득하려 한다.
- Hibernate의 lazy connection acquisition 설정에 따라 실제로 커넥션이 낭비될 수 있음.
- 현재는 우연히 문제 없지만, **의도하지 않은 커넥션 점유 가능성**이 있다.

#### 2. `transaction-event` 스레드 풀 크기 vs HikariCP 풀 크기 불균형
```yaml
# S3AsyncConfig.java
transaction-event:
  corePoolSize: 4
  maxPoolSize: 40      # ← 최대 40개 스레드
  queueCapacity: 100

# storage.yml (prod 포함 전체 프로파일)
# maximum-pool-size 설정 없음 → HikariCP 기본값: 10
```

아티클 공식 적용:
- 스레드 40개 × 커넥션 1개/스레드 = **최소 40개 커넥션 필요**
- 현재 HikariCP 기본값: **10개**
- `handle()`이 클래스 레벨 `@Transactional`로 인해 커넥션을 점유한다면
  부하 상황에서 **커넥션 풀 고갈 → 데드락** 재현 가능

#### 3. prod/load-test 프로파일에 HikariCP 설정 없음
```yaml
# prod 프로파일 storage.yml
storage:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    jdbcUrl: ${JDBC_URL}
    username: ${MYSQL_USERNAME}
    password: ${MYSQL_PASSWORD}
    # maximum-pool-size, minimum-idle, connection-timeout 등 미설정
```
운영 환경에서 커넥션 풀 크기가 완전히 기본값(10)에 의존 중.

---

## 적용 계획

### Task 1: `handle()` 메서드의 불필요한 트랜잭션 제거 (위험도: 높음)

**방법 A (권장):** `handle()`을 별도 컴포넌트로 분리
- `BookRepositoryAdapter`가 `@Transactional` 클래스여야 하는 이유는 DB 접근 메서드들 때문임
- S3 이벤트 핸들러는 DB 접근이 전혀 없으므로 **다른 클래스로 분리**하는 것이 설계상 올바름

```java
// 새 클래스: ImageUploadEventHandler.java (storage 모듈)
@Component
public class ImageUploadEventHandler {

    private final AsyncBucketImageUploader imageUploader;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async(value = "transaction-event")
    public void handle(ImageUploadEvent event) {
        for (PreAssignedUrl url : event.jobs()) {
            imageUploader.copyToBookStorage(url);
        }
    }
}
```

**방법 B:** `handle()`에 `@Transactional(propagation = NOT_SUPPORTED)` 추가
- 클래스 레벨 트랜잭션을 메서드 레벨에서 명시적으로 비활성화

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
@Async(value = "transaction-event")
@Transactional(propagation = Propagation.NOT_SUPPORTED)  // 트랜잭션 없이 실행
public void handle(ImageUploadEvent event) { ... }
```

---

### Task 2: HikariCP 풀 크기 명시적 설정 (위험도: 중간)

아티클의 공식을 이 프로젝트에 맞게 적용:

**필요 커넥션 계산:**
- 메인 요청 처리 스레드 (Tomcat 기본 200) 중 DB를 사용하는 동시 요청 수
- `transaction-event` 비동기 스레드가 DB를 사용한다면 최대 40개 추가 필요
- Task 1 완료 후, `transaction-event`는 DB 커넥션 불필요 → Tomcat 동시 처리 기준만 고려

부하 테스트 결과 기반 pool 크기 결정:

```
RDS max connections: 60
메인 앱 pool:        50  (포화점 140 VU 기준 최적값)
버퍼:               10  (관리/모니터링용)
```

```yaml
# storage.yml (prod 프로파일)
storage:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    jdbcUrl: ${JDBC_URL}
    username: ${MYSQL_USERNAME}
    password: ${MYSQL_PASSWORD}
    maximum-pool-size: 50
    minimum-idle: 5
    connection-timeout: 30000
    idle-timeout: 600000
    max-lifetime: 1800000
```

```yaml
# storage.yml (load-test 프로파일)
storage:
  datasource:
    maximum-pool-size: 50
    minimum-idle: 10
    connection-timeout: 30000
```

---

### Task 3: `transaction-event` 스레드 풀 재조정 (위험도: 낮음)

Task 1로 `handle()`에서 DB 사용이 없어지면, 스레드 풀 크기 제한의 의미가 바뀜.
DB가 아닌 **S3 I/O 병목** 기준으로 재조정:

```java
// S3AsyncConfig.java
@Bean(name = "transaction-event")
public ThreadPoolTaskExecutor transactionEvent() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(4);
    executor.setMaxPoolSize(20);   // 40 → 20 (S3 I/O 기준, DB 무관)
    executor.setQueueCapacity(200); // 큐는 여유 있게
    executor.setThreadNamePrefix("transaction-event-");
    executor.initialize();
    return executor;
}
```

---

### Task 4: LLM 비동기 처리 아키텍처 (1단계 - 폴링 기반)

#### 배경

LLM 호출은 수 초~수십 초 소요되는 블로킹 I/O. 현재 구조는 Tomcat 스레드가 LLM 응답을 기다리는 동안 점유 → 포화점 저하. Lambda로 위임하여 메인 앱 부하 분산.

**Lambda → RDS 직접 접근 배제 이유:**
- Lambda는 수평 확장 → 커넥션 수 예측 불가
- RDS max 60 중 50을 메인 앱이 사용 → Lambda 여유분 없음
- RDS Proxy 미사용 결정

#### 채택 아키텍처

```
[Client]
   │ POST /api/v1/book/progress/init
   ▼
[Main App]
   │ bookInProgress 생성 (status: PENDING)
   │ SQS 메시지 발행 (bookInProgressId, 입력값)
   │ 즉시 응답 반환 (bookInProgressId)
   ▼
[SQS]
   ▼
[Lambda]
   │ LLM API 호출
   │ Redis에 결과 저장 (key: bookInProgress:{id}, TTL: 10분)
   ▼
[Redis t3.micro]  ← 커넥션 제한 없음 (max 10,000)

[Client]
   │ GET /api/v1/book/progress/{id}/status  (폴링, 2~3초 간격)
   ▼
[Main App]
   │ Redis에서 {id} 조회
   │ 없음 → 200 { status: "PENDING" }
   │ 있음 → DB 저장, Redis 삭제 → 200 { status: "COMPLETED", ... }
```

#### Redis 고려사항

- **커넥션**: t3.micro Redis max 10,000 → Lambda 수백 동시 실행도 문제 없음
- **메모리**: t3.micro 1GB, LLM 결과 건당 수 KB → TTL 필수 (10분 권장)
- **내구성**: 인메모리 → 재시작 시 유실 위험. TTL 내 처리 완료 보장 필요
  - 미처리 시 Client 재요청 or 별도 재처리 정책 필요

#### 폴링 흐름

```
Client                Main App            Redis
  │── POST init ──────▶│
  │◀─ { id: 123 } ─────│── SET key 없음
  │
  │── GET /123/status ─▶│── GET book:123 → nil
  │◀─ { PENDING } ──────│
  │   (2초 대기)
  │── GET /123/status ─▶│── GET book:123 → 결과 있음
  │                      │── DB 저장
  │                      │── DEL book:123
  │◀─ { COMPLETED } ─────│
```

---

## 우선순위 및 판단

| 작업 | 우선순위 | 이유 |
|------|----------|------|
| Task 1: `handle()` 분리 또는 NOT_SUPPORTED | **높음** | 현재 코드의 설계 결함. 부하 시 실제 데드락 유발 가능 |
| Task 2: HikariCP 명시적 설정 (pool 50) | **완료** | 부하 테스트 기반 최적값 적용 (prod + load-test 프로파일) |
| Task 3: 스레드 풀 재조정 | **낮음** | Task 1 완료 후 수치 측정 기반으로 결정 |
| Task 4: LLM 비동기 위임 (SQS → Lambda → Redis → 폴링) | **높음** | LLM I/O가 Tomcat 스레드 점유 → 포화점 저하 방지 |

---

## 결론

아티클의 문제 구조는 현재 프로젝트에 **직접적으로 적용 가능**하다.

- 아티클: SQS 스레드 + REQUIRES_NEW + HikariCP 부족 → 데드락
- 이 프로젝트: `transaction-event` 스레드(max 40) + 클래스 레벨 `@Transactional` + HikariCP 기본값(10) → **동일한 구조적 위험**

현재는 `handle()` 내부에서 실제 DB 쿼리를 실행하지 않아 문제가 드러나지 않지만,
이는 **우연한 안전**이지 **설계적 안전**이 아니다.

**완료된 작업:**
- Task 2: HikariCP pool 50 적용 (부하 테스트 기반, 포화점 140 VU @ 1,059 req/s)

**추가된 아키텍처 결정:**
- LLM I/O를 Lambda로 위임 (SQS → Lambda → Redis)
- Lambda는 RDS 직접 접근 금지 (커넥션 제어 불가)
- 완료 감지는 1단계 폴링 방식 채택
- 2단계에서 SSE + Redis Pub/Sub 전환 예정 (이때 VT 도입 유효)
