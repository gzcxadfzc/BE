# API 문서

## 개요
LittleWriter API는 AI 기반 동화책 생성 플랫폼의 백엔드 API입니다.

Base URL: `/api/v1`

## 인증
대부분의 API는 JWT 토큰 기반 인증이 필요합니다.
- 로그인/회원가입 시 발급받은 토큰을 `Authorization: Bearer {token}` 헤더에 포함하여 요청합니다.

---

## 1. 인증 (Authentication)

### 1.1 로그인
**Endpoint:** `POST /api/v1/auth/signin`

**Request Body:**
```json
{
  "username": "string (필수)",
  "password": "string (필수)"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "token": "string"
  }
}
```

**설명:** 사용자 인증 후 JWT 토큰을 발급합니다.

---

### 1.2 회원가입
**Endpoint:** `POST /api/v1/auth/signup`

**Request Body:**
```json
{
  "username": "string (필수)",
  "password": "string (필수)"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "token": "string"
  }
}
```

**설명:** 새로운 사용자를 등록하고 JWT 토큰을 발급합니다.

---

## 2. 책 (Book)

### 2.1 책 상세 조회
**Endpoint:** `GET /api/v1/book/board/{bookId}`

**Path Parameters:**
- `bookId` (string): 조회할 책의 ID

**Response:**
```json
{
  "success": true,
  "data": {
    "bookId": "string",
    "title": "string",
    "author": "string",
    "pages": [
      {
        "pageNumber": "number",
        "content": "string",
        "imageUrl": "string"
      }
    ]
  }
}
```

**설명:** 특정 책의 상세 정보를 조회합니다.

---

### 2.2 책 목록 조회 (페이징)
**Endpoint:** `GET /api/v1/book/board/all`

**Query Parameters:**
- `index` (number, optional): 페이지 인덱스 (기본값: 0)
- `size` (number, optional): 페이지 크기 (기본값: 10)
- `sort` (string, optional): 정렬 옵션 (기본값: createdAtAsc)
  - `createdAtAsc`: 생성일 오름차순
  - `createdAtDesc`: 생성일 내림차순
  - `titleAsc`: 제목 오름차순
  - `titleDesc`: 제목 내림차순

**요청 예시:**
```
GET /api/v1/book/board/all?index=0&size=20&sort=createdAtDesc
```

**Response:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "bookId": "string",
        "title": "string",
        "author": "string",
        "thumbnailUrl": "string"
      }
    ],
    "totalElements": "number",
    "totalPages": "number",
    "currentPage": "number"
  }
}
```

**설명:** 전체 책 목록을 페이징하여 조회합니다. 페이지 크기, 인덱스, 정렬 옵션을 지정할 수 있습니다.

---

### 2.3 내 책 조회
**Endpoint:** `GET /api/v1/book/my`

**Headers:**
- `Authorization: Bearer {token}` (필수)

**Response:**
```json
{
  "success": true,
  "data": {
    "books": [
      {
        "bookId": "string",
        "title": "string",
        "author": "string",
        "thumbnailUrl": "string"
      }
    ]
  }
}
```

**설명:** 현재 로그인한 사용자가 작성한 책 목록을 조회합니다.

---

## 3. 캐릭터 (Character)

### 3.1 내 캐릭터 조회
**Endpoint:** `GET /api/v1/character/my`

**Headers:**
- `Authorization: Bearer {token}` (필수)

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": "number",
      "name": "string",
      "personality": "string",
      "userDescription": "string",
      "appearanceDescription": "string",
      "imageUrl": "string"
    }
  ]
}
```

**설명:** 현재 로그인한 사용자가 생성한 캐릭터 목록을 조회합니다.

---

### 3.2 캐릭터 상세 조회
**Endpoint:** `GET /api/v1/character/board/{id}`

**Path Parameters:**
- `id` (number): 조회할 캐릭터의 ID

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "number",
    "name": "string",
    "personality": "string",
    "userDescription": "string",
    "appearanceDescription": "string",
    "imageUrl": "string"
  }
}
```

**설명:** 특정 캐릭터의 상세 정보를 조회합니다.

---

### 3.3 캐릭터 생성
**Endpoint:** `POST /api/v1/character/create`

**Headers:**
- `Authorization: Bearer {token}` (필수)

**Request Body:**
```json
{
  "name": "string (필수)",
  "personality": "string (필수)",
  "userDescription": "string (필수)",
  "appearanceDescription": "string (필수)"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "number",
    "name": "string",
    "personality": "string",
    "userDescription": "string",
    "appearanceDescription": "string",
    "imageUrl": "string"
  }
}
```

**설명:** 새로운 캐릭터를 생성합니다. AI를 통해 캐릭터 이미지가 생성됩니다.

---

## 4. 책 진행 (Book Progress)

### 4.1 책 초기화
**Endpoint:** `POST /api/v1/book/progress/init`

**Headers:**
- `Authorization: Bearer {token}` (필수)

**Request Body:**
```json
{
  "characterId": "number (필수)",
  "backgroundInfo": "string (필수)",
  "userInput": "string (필수)"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "bookId": "string",
    "pageContent": "string",
    "imageUrl": "string"
  }
}
```

**설명:** 새로운 책을 초기화하고 첫 페이지를 AI로 생성합니다.

---

### 4.2 책 완성
**Endpoint:** `POST /api/v1/book/progress/{id}/complete`

**Headers:**
- `Authorization: Bearer {token}` (필수)

**Path Parameters:**
- `id` (string): 완성할 책의 진행 ID

**Request Body:**
```json
{
  "title": "string (필수)",
  "author": "string (필수)"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "bookId": "string",
    "title": "string",
    "author": "string",
    "pages": [
      {
        "pageNumber": "number",
        "content": "string",
        "imageUrl": "string"
      }
    ]
  }
}
```

**설명:** 진행 중인 책을 완성하여 정식 책으로 등록합니다.

---

### 4.3 페이지 생성
**Endpoint:** `POST /api/v1/book/progress/{id}`

**Headers:**
- `Authorization: Bearer {token}` (필수)

**Path Parameters:**
- `id` (string): 진행 중인 책의 ID

**Request Body:**
```json
{
  "characterId": "number (필수)",
  "backgroundInfo": "string (필수)",
  "userInput": "string (필수)"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "bookId": "string",
    "pageContent": "string",
    "imageUrl": "string"
  }
}
```

**설명:** 진행 중인 책에 새로운 페이지를 AI로 생성합니다.

---

### 4.4 진행 중인 책 조회
**Endpoint:** `GET /api/v1/book/progress/{id}`

**Headers:**
- `Authorization: Bearer {token}` (필수)

**Path Parameters:**
- `id` (string): 조회할 진행 중인 책의 ID

**Response:**
```json
{
  "success": true,
  "data": {
    "bookId": "string",
    "pages": [
      {
        "pageNumber": "number",
        "content": "string",
        "imageUrl": "string"
      }
    ]
  }
}
```

**설명:** 진행 중인 책의 현재 상태를 조회합니다.

---

## 5. 공통 응답 형식

### 성공 응답
```json
{
  "success": true,
  "data": { }
}
```

### 에러 응답
```json
{
  "success": false,
  "error": {
    "code": "string",
    "message": "string"
  }
}
```

---
## 참고사항

1. 모든 날짜/시간은 ISO 8601 형식을 사용합니다.
2. 모든 요청/응답은 UTF-8 인코딩을 사용합니다.
3. Content-Type은 `application/json`을 사용합니다.
4. AI 기반 생성 작업(캐릭터 생성, 페이지 생성)은 처리 시간이 다소 소요될 수 있습니다.
