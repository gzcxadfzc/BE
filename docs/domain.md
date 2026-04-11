# Domain Bounded Contexts

## Overview

| Bounded Context | Package | Storage | Key Entity |
|---|---|---|---|
| Member | `domain.member` | RDS | Member |
| Book | `domain.book` | RDS | Book |
| BookProgress | `domain.bookprogress` | Redis | BookInProgress |
| Character | `domain.character` | RDS | BookCharacter |
| AI | `domain.ai` | - | (interfaces only) |
| Image | `domain.image` | S3 | - |

---

## 1. Member Context

**Responsibility:** 회원 가입, 인증 자격 관리

### Entities
```
Member (record)
  - Long id
  - String username
  - String password (hashed)
  - Role role         ← GUEST | MEMBER | ADMIN
```

### Value Objects
```
Actor (record)        ← 요청 컨텍스트에 실려 다니는 인증 정보
  - Long id
  - Role role
```

### Commands
```
SignUpCommand (record)
  - String username   ← 비어있으면 MemberException.invalidCommand()
  - String password   ← 비어있으면 MemberException.invalidCommand()
```

### Repository
```java
Member register(Member member)
```

### Domain Rules
- username은 고유해야 함 (`duplicatedUsername` E409)
- password는 저장 전 반드시 해싱 (`PasswordHasher`)

### Exceptions
| Method | Code | 설명 |
|---|---|---|
| `notFound(username)` | E404 | 존재하지 않는 회원 |
| `invalidPassword()` | E404 | 비밀번호 불일치 |
| `duplicatedUsername(username)` | E409 | 중복 username |
| `invalidCommand(message)` | E400 | 유효하지 않은 커맨드 |

---

## 2. Book Context

**Responsibility:** 완성된 책 저장 및 조회

### Entities
```
Book (record)
  - String id           ← compact UUID
  - Long memberId
  - List<BookPage> bookPages
  - String title
  - String author
  - BookCharacter character
```

### Value Objects
```
BookPage (record)
  - String context       ← 페이지 본문
  - String imageUrl      ← 삽화 URL
  - int pageNumber       ← 0-based

BookThumbnail (record)
  - String bookId
  - String title
  - String author
  - String coverImageUrl
  - LocalDateTime createdAt
```

### Factory Method
```java
// Book.java
static Book completeFromCommand(BookInProgress bip, CompleteBookCommand cmd)
  // Validation: bip.status == PENDING         → BookProgressException.bookNotCompleted()
  // Validation: actor == owner || ADMIN       → BookException.notAuthorizedBookCreationFrom()
```

### Repository
```java
Book saveFrom(BookInProgress bip, Function<BookInProgress, Book> converter)
Book save(Book book)
Book retrieveById(String bookId)
List<BookThumbnail> retrieveThumbnailsByUser(Actor currentUser)
PageResult<BookThumbnail> retrieveThumbnails(BookRetrieveQuery query)
```

### Domain Rules
- `BookInProgress` 상태가 **PENDING**일 때만 `Book`으로 완성 가능
- 완성은 소유자 또는 ADMIN만 가능
- 첫 페이지의 imageUrl이 커버 이미지

### Exceptions
| Method | Code | 설명 |
|---|---|---|
| `notFound(bookId)` | E404 | 존재하지 않는 책 |
| `notAuthorizedBookCreationFrom(bip)` | E403 | 권한 없음 |

---

## 3. BookProgress Context

**Responsibility:** 진행 중인 책의 생성·페이지 추가·완성 처리 (핵심 도메인)

### Entity
```
BookInProgress (record)
  - String id               ← compact UUID
  - Long ownerId
  - String backgroundInfo
  - BookCharacter character
  - List<BookPage> previousPages
  - Status status           ← IN_PROGRESS | PENDING | COMPLETED
```

### Status State Machine

**현재 (v2) — completeBook 내에서 원자적 전환**
```
              initBook()                    generateWithAi()
   ──────────────────────────► IN_PROGRESS ──────────────────────► IN_PROGRESS
                                    │          (LLM 직접 호출,
                                    │           페이지 추가 후 유지)
                        completeBook │ markAsPending()
                                    ▼
                                 PENDING
                                    │
                      Book 저장 후  │ markAsCompleted()
                                    ▼
                                COMPLETED (불변)
```

> **v3 목표 — generateWithAi 비동기 전환**
> `generateWithAi()` 호출 시 즉시 LLM 호출하지 않고, SQS에 메시지 발행 후 PENDING으로 전환.
> Lambda 처리 완료 → Redis 저장 → 폴링 엔드포인트가 감지 → 페이지 DB 저장 → IN_PROGRESS 복귀.
> `completeBook`의 PENDING 전환은 v2/v3 모두 동일하게 유지.

### State Transition Methods
```java
// BookInProgress.java

markAsPending()
  // 전제: status != COMPLETED (위반 시 alreadyCompleted())
  // 결과: status → PENDING

markAsCompleted()
  // 전제: status != COMPLETED (위반 시 alreadyCompleted())
  // 결과: status → COMPLETED

addBookPage(BookPage page)
  // 전제: status != COMPLETED (위반 시 bookNotCompleted())  ← 예외명 주의
  // 결과: 페이지 추가, status → IN_PROGRESS

appendPageFrom(CreateOnePageCommand cmd, String context, String imageUrl)
  // 전제: status != COMPLETED (위반 시 bookNotCompleted())
  // 전제: actor == owner || ADMIN (위반 시 forbiddenResource())
  // 결과: 페이지 추가 (pageNumber = previousPages.size()), status → IN_PROGRESS

changeBookPages(UnaryOperator<List<BookPage>> modifier)
  // 전제: status != COMPLETED (위반 시 bookNotCompleted())

changeBookPage(UnaryOperator<BookPage> modifier)
  // 전제: status != COMPLETED (위반 시 alreadyCompleted())
```

> **예외명 주의**: `bookNotCompleted()`는 이름과 달리 "이미 완료됨" 상황에서 발생하는 경우도 있음. 실제로는 "페이지 추가 불가 상태" 의미.

### Commands
```
BookInitCommand
  - Long characterId
  - String background
  - Actor currentUser
  - String userInput

CreateOnePageCommand (ai 패키지)
  - String bipId
  - String userInput
  - Actor currentUser

CompleteBookCommand
  - Actor actor
  - String bookInProgressId
  - String title
  - String author
```

### Repository
```java
List<BookInProgress> retrieveByMemberId(Long memberId)
BookInProgress retrieveById(String id)
BookInProgress save(BookInProgress bip)
BookInProgress addPageTo(String id, BookPage page)
```

### Services

**BookProgressService** (v2 현재 구현)
```java
// initBook: BIP 생성 → LLM 직접 호출 → 이미지 임시 업로드 → 페이지 저장
AiGenerateResult initBook(BookInitCommand command)

// generateWithAi: Redis Lock → LLM 직접 호출 → 페이지 추가
AiGenerateResult generateWithAi(CreateOnePageCommand command)

// 소유자 또는 ADMIN만 조회 가능
BookInProgress retrieveById(Actor user, String bipId)
```

> **v3 목표**
> ```java
> // initBook: BIP 생성 → SQS 발행 → PENDING 전환 → 202 반환
> // generateWithAi: PENDING 전환 → SQS 발행 → 202 반환 (Lock 제거)
> ```

**BookCompleteExecutor** (v2 현재 구현)
```java
// completeBook 흐름:
// 1. lockExecutor.saveWithLock(bipId, ...)
// 2. bip.markAsPending()                           IN_PROGRESS → PENDING
// 3. bookRepository.saveFrom(bip, ...)             Book.completeFromCommand() 호출 (PENDING 검증)
// 4. bookInProgressRepository.save(bip.markAsCompleted())  PENDING → COMPLETED
Book completeBook(CompleteBookCommand command)
```

> **v3 목표**: Lock 제거. PENDING 상태 자체가 중복 완성 방지 역할.

**BookInProgressLockExecutor** (interface, Redis 구현)
```java
AiGenerateResult updateWithLock(String lockKey, Supplier<AiGenerateResult> generator)
Book saveWithLock(String lockKey, Supplier<Book> generator)
```

Lock Key: `bip:lock:{bipId}`, TTL: 120000ms

> **v3**: generateWithAi Lock → SQS FIFO MessageGroupId로 대체. completeBook Lock → 제거.

### SQS 메시지 스키마 (v3 목표)

**메인 앱 → SQS 발행 페이로드**
```json
{
  "bipId": "string",
  "pageIndex": 0,
  "userInput": "string",
  "characterName": "string",
  "characterDescription": "string",
  "backgroundInfo": "string"
}
```
- MessageGroupId: `{bipId}`
- MessageDeduplicationId: `{bipId}-{pageIndex}`

### Redis 결과 스키마 (v3 목표, Lambda가 저장)
```
Key: bip:result:{bipId}        TTL: 600s
Value: {
  "pageIndex": 0,
  "context": "page content",
  "imageUrl": "https://...",
  "questions": ["q1", "q2", "q3"]
}

Key: idempotency:{messageId}   TTL: 86400s
Value: IN_FLIGHT | {llmResult} | DONE
```

### 폴링 엔드포인트 응답 (v3 목표)
```
GET /api/v1/book/progress/{id}/status

없음 → { status: "PENDING" }
있음 → DB 페이지 저장 → Redis DEL → IN_PROGRESS 전환
     → { status: "COMPLETED", page: { context, imageUrl, questions } }
```

> 응답 `"COMPLETED"` = 페이지 생성 완료(BIP.Status.IN_PROGRESS)이며, 책 완성(BIP.Status.COMPLETED)과 무관.

### Exceptions
| Method | Code | 설명 |
|---|---|---|
| `notFound(resource)` | E404 | 리소스 없음 |
| `forbiddenResource()` | E403 | 권한 없음 |
| `bookPageAlreadyGenerating(bipId)` | E409 | 이미 생성 중 (Lock 충돌) |
| `bookPageAlreadySaving(bipId)` | E409 | 이미 저장 중 (Lock 충돌) |
| `bookNotCompleted(bipId)` | E409 | COMPLETED 상태에서 페이지 추가 시도 |
| `alreadyCompleted(bipId)` | E409 | 이미 완성됨 |

---

## 4. Character Context

**Responsibility:** 캐릭터 생성 및 조회

### Entity
```
BookCharacter (record)
  - Long id
  - Long userId
  - String name
  - String appearanceKeywords
  - String personality
  - String description
  - String imageUrl
```

### Commands / Requests
```
BookCharacterCreateCommand
  - String name, personality, description, appearanceKeywords
  - String imageUrl
  - Long userId

BookCharacterGenerateRequest
  - Actor creator
  - String name, appearanceKeywords, personality, description
  → toCommand(imageUrl): BookCharacterCreateCommand

BookCharacterImageRequest
  - String description, appearance
```

### Repository
```java
BookCharacter retrieveById(Long characterId)
List<BookCharacter> retrieveByUser(Actor user)
BookCharacter createFrom(BookCharacterCreateCommand command)
```

### Service: BookCharacterService
```java
retrieveById(Long characterId)          // null이면 notFound()
create(BookCharacterGenerateRequest)    // AI 이미지 생성 → S3 업로드 → DB 저장
retrieveByUser(Actor currentUser)
```

### Domain Rules
- 캐릭터는 소유자(userId)에 귀속됨
- `BookInProgress` 생성 시 캐릭터 존재 여부 검증 필요 (BookProgressService에서 수행)
- 캐릭터 이미지는 S3 영구 저장소(`character/` 접두사)에 저장

### Exceptions
| Method | Code | 설명 |
|---|---|---|
| `notFound(Long)` | E404 | 존재하지 않는 캐릭터 |
| `forbidden()` | E403 | 권한 없음 |

---

## 5. AI Context

**Responsibility:** LLM 호출 추상화 (인터페이스만 존재, 구현은 외부)

> **v3**: 이 컨텍스트의 BookPageGenerator 구현체는 Python Lambda로 이전 예정.
> Spring 앱에서는 SQS 발행만 수행하고 직접 호출하지 않음.
> BookCharacterGenerator(캐릭터 이미지)는 계속 Spring 앱에서 직접 호출.

### Interfaces
```java
// 페이지 콘텐츠 + 삽화 생성
BookPageGenerator
  BookPageGenerated generatePageFrom(BookToProgress bookToProgress)

// 캐릭터 초상화 생성 (v3에서도 Spring 앱 직접 호출 유지)
BookCharacterGenerator
  String generateImageFrom(BookCharacterGenerateRequest request)
```

### Value Objects
```
BookPageGenerated (record)
  - String generatedIllustrationUrl
  - String context
  - List<String> questions

BookToProgress (record)
  - BookInProgress bookInProgress
  - String userInput
```

---

## 6. Image Context

**Responsibility:** 이미지 업로드 및 영구 저장소 이전

### Repository
```java
ImageUploadResult uploadTemporary(String url)         // → temp/ 접두사
ImageUploadResult uploadCharacterImage(String url)    // → character/ 접두사
Map<String, ImageUploadResult> copyAllToPermanentStorage(List<String> urls)  // → book/ 접두사
```

### Value Objects
```
ImageUploadResult (record)
  - String originUrl
  - String newUrl
```

### Domain Rules
- AI 생성 이미지는 임시 저장소(`temp/`)에 먼저 저장
- Book 완성 시 모든 페이지 이미지를 영구 저장소(`book/`)로 복사 (비동기)
- 캐릭터 이미지는 생성 시점에 영구 저장소(`character/`)에 저장

### Events
```java
ImageUploadEvent             // Book 저장 후 비동기 S3 이전 트리거
  → ImageUploadEventHandler  // @TransactionalEventListener AFTER_COMMIT
                             // @Async("transaction-event")
```

### Exceptions
| Method | Code | 설명 |
|---|---|---|
| `uploadFailed(message)` | E500 | 업로드 실패 |

---

## 7. Storage Implementation

### Redis Entities

**BookInProgressRedisEntity**
```
Key: book:inprogress:{id}    TTL: 360000s (100h)
Fields: id, memberId, backgroundInfo, character, storyLength, status

Status: IN_PROGRESS | PENDING | COMPLETED
```

**BookPageRedisEntity**
```
Key: book:pages:{bookId}     TTL: 3600s (1h)
Type: Redis List
Fields: bookInProgressId, context, imageUrl, pageNumber
```

**Member Index**
```
Key: member:{memberId}:bip   → Set of BIP IDs
```

### Redis Lock
```java
RedisLockManager
  <T> execute(String key, long expireMillis, Supplier<T> action)
  // SET NX EX → 성공 시 action 실행 → Lua script로 atomic 해제
  // 실패 시 RedisLockException

Lock Keys:
  bip:lock:{bipId}    TTL: 120000ms (2min)
```

---

## Dependency Graph

```
BookProgressService
  ├── BookCharacterRepository     (Character ctx)
  ├── BookInProgressRepository    (BookProgress ctx / Redis)
  ├── BookPageGenerator           (AI ctx / v3: Lambda로 이전)
  ├── ImageRepository             (Image ctx)
  └── BookInProgressLockExecutor  (Redis / v3: 제거 예정)

BookCompleteExecutor
  ├── BookInProgressRepository
  ├── BookCharacterRepository
  ├── BookRepository              (Book ctx)
  └── BookInProgressLockExecutor  (Redis / v3: 제거 예정)

BookCharacterService
  ├── BookCharacterRepository
  ├── BookCharacterGenerator      (AI ctx / v3에서도 직접 호출 유지)
  └── ImageRepository
```

---

## API Endpoints

| Method | Path | Handler | 설명 |
|---|---|---|---|
| POST | `/api/v1/member/signup` | MemberController | 회원가입 |
| POST | `/api/v1/member/login` | MemberController | 로그인 |
| GET | `/api/v1/book/board/{bookId}` | BookController | 책 상세 |
| GET | `/api/v1/book/board/all` | BookController | 책 목록 (페이징·정렬) |
| GET | `/api/v1/book/my` | BookController | 내 책 목록 |
| POST | `/api/v1/book/progress/init` | BookProgressController | 책 작성 시작 |
| POST | `/api/v1/book/progress/{id}` | BookProgressController | 페이지 생성 (AI) |
| POST | `/api/v1/book/progress/{id}/complete` | BookProgressController | 책 완성 |
| GET | `/api/v1/book/progress/{id}` | BookProgressController | 진행 상황 조회 |
| GET | `/api/v1/book/progress/{id}/status` | BookProgressController | **[v3 신규]** 생성 완료 여부 폴링 |
| GET | `/api/v1/character/my` | CharacterController | 내 캐릭터 목록 |
| GET | `/api/v1/character/board/{id}` | CharacterController | 캐릭터 상세 |
| POST | `/api/v1/character/create` | CharacterController | 캐릭터 생성 |

---

## Exception Code Reference

| Code | HTTP | 의미 |
|---|---|---|
| E400 | 400 | Bad Request (유효성 오류) |
| E401 | 401 | Unauthorized (인증 실패) |
| E403 | 403 | Forbidden (권한 없음) |
| E404 | 404 | Not Found |
| E409 | 409 | Conflict (상태 충돌, 중복) |
| E500 | 500 | Internal Server Error |
