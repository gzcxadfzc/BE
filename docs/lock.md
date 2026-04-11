# v3 Lock 제거 고민

## 배경

Phase 3 계획(3-F)에서 `generateWithAi`의 Redis Lock을 제거하고 SQS FIFO + PENDING 상태 체크로 대체하려 했음.

---

## Lock이 하던 역할

```
SET NX → 원자적 획득
         실패 시 즉시 409 CONFLICT 반환
```

- 동시 요청 중 정확히 하나만 통과
- 나머지 호출자에게 즉시 피드백

---

## v3 대체 전략 분석

### 순차 요청 (일반 케이스)

```
요청 A (POST /{id}) → PENDING 저장 → SQS 발행 → 202
요청 B (POST /{id}) → status == PENDING 읽음 → 409 CONFLICT ✓
```

PENDING 상태 체크만으로 충분히 동작함.

### 동시 요청 (race condition)

```
요청 A ─┐
요청 B ─┘  둘 다 IN_PROGRESS 읽음
           둘 다 PENDING 체크 통과
           둘 다 SQS 발행 (dedup ID = {bipId}-{pageIndex} 동일)
           SQS FIFO: dedup → B 메시지 드롭
           둘 다 202 반환
```

**문제**: B는 202를 받았지만 자신의 userInput은 실제로 처리되지 않음.
B가 폴링해도 결과는 오지만 A의 처리 결과임.
SQS dedup은 중복 처리를 막을 뿐, 호출자에게 중복을 알려주지 않음.

### SQS dedup 기준

`MessageDeduplicationId = {bipId}-{pageIndex}`

Lambda 처리 완료 전에는 페이지가 BIP에 추가되지 않으므로
`previousPages.size()`가 동일 → 같은 dedup ID → SQS가 드롭.

Lambda 완료 후 폴링이 페이지를 추가하면 `previousPages.size()`가 증가하여
다음 요청은 새 dedup ID로 정상 처리됨.

---

## 결론

| 케이스 | Lock 없이 가능? | 이유 |
|---|---|---|
| 순차 중복 요청 | ✅ | PENDING 상태 체크로 즉시 409 반환 |
| 동시 중복 요청 | ❌ | 둘 다 202 반환, B의 요청은 SQS에서 무통보 드롭 |
| 처리 중복 방지 | ✅ | SQS FIFO dedup이 하드 보장 |

race condition에서 B의 `userInput`이 A와 다를 수 있음.
SQS dedup이 처리 중복을 막아도 B 입장에서는 **자신이 원한 내용이 아닌 페이지**가 생성됨.
단순 중복이 아니라 **잘못된 결과**이므로 Lock 유지가 맞음.

---

## 결정

**`generateWithAi` Lock → 유지**
- `BookInProgressLockExecutor.updateWithLock()` 제거 안 함
- `BookInProgressLockExecutorAdapter` 제거 안 함

**`completeBook` Lock → 별도 검토 필요**
- 동시 완성 요청 시 Book 중복 생성 위험
- `saveWithLock()` 제거 여부 미결

---

## 3-F 계획 수정 사항

| 항목 | 기존 계획 | 수정 |
|---|---|---|
| `BookInProgressLockExecutor` (인터페이스) | 삭제 | **유지** |
| `BookInProgressLockExecutorAdapter` | 삭제 | **유지** |
| `generateWithAi` Lock 제거 | 예정 | **취소** |
| `BookInProgressLockExecutorAdapterTest` | 정리 | **유지** |
| `completeBook` Lock (`saveWithLock`) | 삭제 예정 | 별도 검토
