# Little Writer

AI 기반 어린이 동화책 생성 서비스

## 프로젝트 개요

Little Writer는 AI를 활용하여 어린이들이 자신만의 캐릭터를 만들고, 그 캐릭터가 등장하는 동화책을 생성할 수 있는 서비스입니다.

### 주요 기능
- **캐릭터 생성**: AI 이미지 생성을 통한 맞춤형 캐릭터 제작
- **동화책 생성**: 사용자 입력을 기반으로 AI가 스토리와 삽화를 생성
- **진행 중인 책 관리**: 책 생성 과정을 단계별로 진행하고 저장
- **완성된 책 보관**: 완성된 책을 사용자 라이브러리에 저장

### API문서
- [API DOCS](DOCS.md)

## 아키텍처

이 프로젝트는 **도메인 주도 설계에 기반한 멀티 모듈 구조**를 사용하여 아래와 같이 설계되었습니다.

```
┌─────────────────────────────────────────────────────────────┐
│                         API Layer                           │
│  (Controllers, Request/Response DTOs, Exception Handlers)   │
└────────────────────────┬────────────────────────────────────┘
                         │
                         │      
┌────────────────────────▼────────────────────────────────────┐
│                 Application/Domain Layer                    │
│             (Entities, Aggregates, Services)                │
└─────────────────────────────────────────────────────────────┘
                         │
 InrfraStructure Layer   │
        ┌────────────────┼────────────────┐
        │                │                │
┌───────▼───────┐  ┌─────▼──────┐  ┌──────▼──────┐
│   Storage     │  │     AI     │  │    Auth     │
│  (Adapters)   │  │ (Adapters) │  │  (Adapters) │
│               │  │            │  │             │
│ - JPA/Redis   │  │ - OpenAI   │  │ - JWT       │
│ - S3          │  │            │  │             │
└───────────────┘  └────────────┘  └─────────────┘
```

## 패키지 구조
```
littleWriter/
├── api/                          # REST API 계층
│   ├── controller/
│   │   ├── book/                 # 완성된 책 API
│   │   ├── bookprogress/         # 책 생성 진행 API
│   │   └── character/            # 캐릭터 API
│   └── test/                     # API 통합 테스트
│
├── domain/                       # 도메인 계층 
│   ├── book/                     # Book Aggregate
│   ├── bookprogress/             # BookInProgress Aggregate
│   ├── character/                # Character Aggregate
│   ├── member/                   # Member Aggregate
│   ├── ai/                       # AI 포트 인터페이스
│   ├── image/                    # 이미지 포트 인터페이스
│   └── exception/                # 도메인 예외
│
├── storage/                      # 영속화 계층 (어댑터)
│   ├── jpa/                      # JPA 구현
│   ├── redis/                    # Redis 캐시 & 락
│   └── s3/                       # S3 이미지 저장
│
├── ai/                           # AI 계층 (어댑터)
│   ├── openai/                   # OpenAI API 통합
│   └── core/                     # AI 호출 체인
│
├── auth/                         # 인증/인가 계층
│   ├── authentication/           # JWT 인증
│   └── support/                  # 인증 지원 유틸
│
└── core/                         # 공통 계층
    ├── config/                   # 공통 설정
    └── common/                   # 공통 유틸리티
```

## 모듈 구조

### 1. Domain 모듈
핵심 비즈니스 로직과 도메인 모델을 포함합니다.
Domain 계층과 Application 계층이 속하는 모듈입니다.

```
domain/
├── book/              # 완성된 책 
├── bookprogress/      # 진행 중인 책 
├── character/         # 캐릭터 
├── member/            # 회원 
├── ai/                # AI 관련 포트 인터페이스
├── image/             # 이미지 관리 포트 인터페이스
└── exception/         # 도메인 예외
```

**주요 책임**:
- 비즈니스 규칙 및 정책 정의
- 도메인 엔티티 및 값 객체 관리
- 외부 의존성을 위한 포트(인터페이스) 정의


### 2. API 모듈
REST API 엔드포인트와 HTTP 요청/응답을 처리합니다.

```
api/
├── controller/
│   ├── book/              # 책 관련 API
│   ├── bookprogress/      # 책 생성 진행 API
│   └── character/         # 캐릭터 관련 API
├── ApiControllerAdvice    # 전역 예외 처리
└── TestApplication        # 테스트용 애플리케이션
```

**주요 책임**:
- HTTP 요청/응답 처리
- DTO 변환 (Request/Response)
- API 문서화
- 전역 예외 처리

### 3. Storage 모듈
데이터 영속화 및 외부 스토리지 통합을 담당합니다.

```
storage/
├── jpa/                # JPA 엔티티 및 리포지토리
├── redis/              # Redis 캐시 및 락 제어
└── s3/                 # S3 이미지 업로드
```

**주요 책임**:
- 데이터베이스 영속화 (JPA)
- Redis를 통한 진행 중인 책 캐싱
- 특정 자원에 대한 락 제어(SETNX)
- S3 이미지 업로드

**구현 어댑터**:
- `BookRepositoryAdapter` → `BookRepository` (Domain Port)
- `BookInProgressRedisRepository` → `BookInProgressRepository`
- `RedisLockManager` → 락 관리
- `S3BucketUtils` → `ImageRepository`

### 4. AI 모듈
OpenAI API를 활용한 AI 기능을 제공합니다.

```
ai/
├── openai/
│   ├── BookPageGeneratorAdapter      # 페이지 생성 어댑터
│   └── BookCharacterGeneratorAdapter # 캐릭터 이미지 생성 어댑터
└── core/
    └── CallStepChain                 # AI API 호출 체인
```

**주요 책임**:
- OpenAI API 통합
- 스토리 및 삽화 생성
- 캐릭터 이미지 생성
- 프롬프트 엔지니어링

**구현 어댑터**:
- `BookPageGeneratorAdapter` → `BookPageGenerator` (Domain Port)
- `BookCharacterGeneratorAdapter` → `BookCharacterGenerator` (Domain Port)

### 5. Auth 모듈
인증 및 자격증명 관리를 담당합니다.

```
auth/
├── authentication/
│   ├── token/             # JWT 토큰 처리
│   └── core/              # 인증 코어 로직
└── support/
    └── Authenticated      # 인증 어노테이션
```

**주요 책임**:
- JWT 토큰 생성 및 검증
- 사용자 인증

**Authenticator**
- JwtAuthenticator를 통해 인증된 사용자 정보를 저장하여 @Authenticated 어노테이션에 매핑됩니다


### 6. Core 모듈
공통 유틸리티 및 설정을 제공합니다.

```
core/
├── config/               # 공통 설정 (Redis, S3 등)
└── common/              # 공통 유틸리티
```

**주요 책임**:
- 공통 설정 관리
- 유틸리티 함수
- 공통 인프라 설정

**의존성**: 없음

## 도메인 모델

### Aggregates

#### 1. Book (완성된 책)
**루트 엔티티**: `Book`

```java
public record Book(
    String id,
    Long memberId,
    List<BookPage> bookPages,
    String title,
    String author,
    BookCharacter character
)
```

**설명**: 사용자가 완성한 동화책을 나타냅니다.

**주요 행위**:
- `completeFromCommand()`: `BookInProgress`에서 완성된 `Book` 생성

**불변 규칙**:
- 완성된 책은 수정할 수 없음
- 최소 1개 이상의 페이지 필요

#### 2. BookInProgress (진행 중인 책)
**루트 엔티티**: `BookInProgress`

```java
public record BookInProgress(
    String id,
    Long ownerId,
    String backgroundInfo,
    BookCharacter character,
    List<BookPage> previousPages,
    Status status
)
```

**설명**: AI를 통해 생성 중인 책을 나타냅니다. Redis에 캐싱되어 빠른 액세스를 제공합니다.

**주요 행위**:
- `addBookPage()`: 새로운 페이지 추가
- `appendPageFrom()`: 명령으로부터 페이지 생성 및 추가
- `changeBookPages()`: 페이지 일괄 변경 (이미지 URL 변경 등)
- `markAsCompleted()`: 상태를 완료로 변경

**상태 전이**:
- `IN_PROGRESS` → `COMPLETED`

**불변 규칙**:
- 소유자만 수정 가능
- 완료된 책은 다시 진행 중 상태로 변경 불가

**동시성 제어**:
- Redis 락을 통한 동시 페이지 생성 방지
- 같은 책에 대한 동시 저장 방지

#### 3. BookCharacter (캐릭터)
**루트 엔티티**: `BookCharacter`

```java
public record BookCharacter(
    Long id,
    Long userId,
    String name,
    String appearanceKeywords,
    String personality,
    String description,
    String imageUrl
)
```

**설명**: AI로 생성된 동화책 캐릭터를 나타냅니다.

**주요 행위**:
- AI 이미지 생성 및 저장

**불변 규칙**:
- 생성 후 이미지 URL 변경 불가
- 소유자 정보 불변

#### 4. Member (회원)
**루트 엔티티**: `Member`/`Actor`

**설명**: 서비스 사용자를 나타냅니다.

**역할**:
- `MEMBER`: 일반 사용자
- `ADMIN`: 관리자

### 도메인 서비스

#### BookProgressService
진행 중인 책 생성 및 관리를 조율합니다.

**주요 메서드**:
- `initBook()`: 책 생성 초기화 (첫 페이지 생성)
- `generateWithAi()`: AI를 통한 페이지 생성 (락 적용)
- `retrieveById()`: 진행 중인 책 조회 (권한 검증)

**협력 객체**:
- `BookPageGenerator`: AI 페이지 생성
- `BookInProgressRepository`: 데이터 영속화
- `BookInProgressLockExecutor`: 락 실행

#### BookCompleteExecutor
진행 중인 책을 완성된 책으로 변환합니다.

**주요 메서드**:
- `completeBook()`: 책 완료 처리 (락 적용)

**처리 과정**:
1. 락 획득
2. 임시 이미지를 영구 스토리지로 복사
3. 책 상태를 `COMPLETED`로 변경
4. `Book` 엔티티 생성 및 저장
5. 락 해제

#### BookCharacterService
캐릭터 생성 및 조회를 담당합니다.

**주요 메서드**:
- `create()`: AI 이미지 생성 및 캐릭터 저장
- `retrieveById()`: 캐릭터 조회
- `retrieveByUser()`: 사용자의 캐릭터 목록 조회

### Repository Ports (인터페이스)

#### BookRepository
완성된 책의 영속화를 담당합니다.

```java
public interface BookRepository {
    Book saveFrom(BookInProgress bookInProgress, Function<BookInProgress, Book> factory);
    Book retrieveById(String bookId);
    List<Book> retrieveByMemberId(Long memberId);
}
```

#### BookInProgressRepository
진행 중인 책의 영속화를 담당합니다 (Redis 캐싱).

```java
public interface BookInProgressRepository {
    void save(BookInProgress bookInProgress);
    BookInProgress retrieveById(String bookInProgressId);
    BookInProgress addPageTo(String bookInProgressId, BookPage bookPage);
    List<BookInProgress> findByMemberId(Long memberId);
}
```

#### BookCharacterRepository
캐릭터의 영속화를 담당합니다.

```java
public interface BookCharacterRepository {
    BookCharacter createFrom(BookCharacterCreateCommand command);
    BookCharacter retrieveById(Long id);
    List<BookCharacter> retrieveByUser(Actor actor);
}
```

### 동시성 제어

#### BookInProgressLockExecutor
Redis 락을 사용하여 동시 작업을 제어합니다.

**구현 방식**:
- `RedisLockManager`: Redis SET NX를 사용한 락
- 락 키: `bip:lock:{bookInProgressId}`
- 타임아웃: 100초

**보호 대상 작업**:
- 페이지 생성 (`updateWithLock`)
- 책 완료 처리 (`saveWithLock`)

**동작 과정**:
```
1. 락 획득 시도 (SET NX)
2. 락 획득 실패 → RedisLockException (409 Conflict)
3. 락 획득 성공 → 작업 실행
4. 작업 완료 → 락 해제 (DEL)
```

**예외 처리**:
- `RedisLockException`: 다른 작업이 진행 중일 때 발생
- HTTP 409 Conflict로 응답

## API 문서

### 캐릭터 관련 API

#### 내 캐릭터 목록 조회
```http
GET /api/v1/character/my
Authorization: Bearer {token}
```

#### 캐릭터 ID로 조회
```http
GET /api/v1/character/board/{id}
```

#### 캐릭터 생성
```http
POST /api/v1/character/create
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "Alice",
  "personality": "curious and brave",
  "userDescription": "A young girl who loves adventure",
  "appearanceDescription": "blonde hair, blue eyes"
}
```

### 책 생성 API

#### 책 생성 초기화
```http
POST /api/v1/book/progress/init
Authorization: Bearer {token}
Content-Type: application/json

{
  "characterId": 1,
  "backgroundInfo": "A magical forest",
  "userInput": "Alice discovers a hidden treasure"
}
```

**응답**: 첫 페이지가 생성된 진행 중인 책

#### 페이지 추가 생성
```http
POST /api/v1/book/progress/{bookInProgressId}
Authorization: Bearer {token}
Content-Type: application/json

{
  "userInput": "Alice opens the mysterious door"
}
```

**동시성 제어**: 동일한 책에 대한 동시 요청 시 409 Conflict 반환

#### 진행 중인 책 조회
```http
GET /api/v1/book/progress/{bookInProgressId}
Authorization: Bearer {token}
```

#### 책 완료
```http
POST /api/v1/book/progress/{bookInProgressId}/complete
Authorization: Bearer {token}
Content-Type: application/json

{
  "title": "Alice's Adventure",
  "author": "John Doe"
}
```

**처리**:
- 임시 이미지를 영구 스토리지로 이동
- `BookInProgress` → `Book` 변환
- 락 적용

### 완성된 책 API

#### 내 책 목록 조회
```http
GET /api/v1/book/my
Authorization: Bearer {token}
```

#### 책 상세 조회
```http
GET /api/v1/book/board/{bookId}
```

### 응답 형식

#### 성공 응답
```json
{
  "result": "SUCCESS",
  "data": { ... }
}
```

#### 에러 응답
```json
{
  "code": "E404",
  "message": "Resource not found"
}
```

#### HTTP 상태 코드
- `200`: 성공
- `400`: 잘못된 요청
- `401`: 인증 필요
- `403`: 권한 없음
- `404`: 리소스 없음
- `409`: 충돌 (동시성 제어)
- `500`: 서버 오류

## 동시성 처리 예시

### 시나리오: 동시에 같은 책에 페이지 생성 요청

```
Thread 1: POST /api/v1/book/progress/123
Thread 2: POST /api/v1/book/progress/123 (100ms 후)

Result:
- Thread 1: 200 OK (페이지 생성 성공)
- Thread 2: 409 Conflict (작업 진행 중)
```

### 시나리오: 페이지 생성 중 완료 요청

```
Thread 1: POST /api/v1/book/progress/123 (페이지 생성)
Thread 2: POST /api/v1/book/progress/123/complete (100ms 후)

Result:
- Thread 1: 200 OK (페이지 생성 성공)
- Thread 2: 409 Conflict (페이지 생성 중)
```