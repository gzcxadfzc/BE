import json
import os
import time
import socket


def handler(event, context):
    failures = []

    for record in event["Records"]:
        try:
            body = json.loads(record["body"])
            process(record["messageId"], body)
        except Exception as e:
            print(f"Failed to process record: {e}")
            failures.append({"itemIdentifier": record["messageId"]})

    return {"batchItemFailures": failures}


def process(message_id, body):
    msg_type = body.get("type", "PAGE")
    if msg_type == "CHARACTER":
        process_character(message_id, body)
    else:
        process_page(message_id, body)


def process_page(message_id, body):
    bip_id = body["bipId"]
    page_index = body.get("pageIndex", 0)

    redis_host = os.environ["REDIS_HOST"]
    redis_port = int(os.environ.get("REDIS_PORT", "6379"))

    idempotency_key = f"idempotency:{message_id}"
    pending_guard_key = f"bip:pending-guard:{bip_id}"

    try:
        # 1. 멱등성 검증
        current = redis_get(redis_host, redis_port, idempotency_key)
        if current in ("DONE", "IN_FLIGHT"):
            print(f"Skip: idempotency={current}, messageId={message_id}")
            return

        # 2. IN_FLIGHT 마킹 (NX: 최초 한 번만)
        acquired = redis_setnx(redis_host, redis_port, idempotency_key, "IN_FLIGHT", ttl=300)
        if not acquired:
            print(f"Skip: failed to acquire IN_FLIGHT lock, messageId={message_id}")
            return

        # 3. LLM 호출 (Mock: sleep)
        sleep_ms = int(os.environ.get("MOCK_SLEEP_MS", "3000"))
        time.sleep(sleep_ms / 1000)

        result = json.dumps({
            "pageIndex": page_index,
            "context": f"mock context for page {page_index}",
            "imageUrl": f"https://mock-s3-url/bip/{bip_id}/page-{page_index}.png",
            "questions": ["질문1", "질문2", "질문3"]
        }, ensure_ascii=False)

        # 4. LLM 결과 캐싱 (crash 대비)
        redis_setex(redis_host, redis_port, idempotency_key, 3600, result)

        # 5. 폴링용 결과 저장
        redis_setex(redis_host, redis_port, f"bip:result:{bip_id}", 600, result)

        # 6. 완료 마킹
        redis_setex(redis_host, redis_port, idempotency_key, 86400, "DONE")

        print(f"Done: bipId={bip_id}, pageIndex={page_index}, messageId={message_id}")

    finally:
        # 7. Pending Guard 해제 (성공/실패 무관하게 항상 삭제)
        try:
            redis_del(redis_host, redis_port, pending_guard_key)
        except Exception as e:
            print(f"Warning: failed to delete pending guard for bipId={bip_id}: {e}")


def process_character(message_id, body):
    cip_id = body["cipId"]
    name = body.get("name", "")

    redis_host = os.environ["REDIS_HOST"]
    redis_port = int(os.environ.get("REDIS_PORT", "6379"))

    idempotency_key = f"idempotency:char:{message_id}"

    try:
        # 1. 멱등성 검증
        current = redis_get(redis_host, redis_port, idempotency_key)
        if current in ("DONE", "IN_FLIGHT"):
            print(f"Skip: idempotency={current}, messageId={message_id}")
            return

        acquired = redis_setnx(redis_host, redis_port, idempotency_key, "IN_FLIGHT", ttl=300)
        if not acquired:
            print(f"Skip: failed to acquire IN_FLIGHT lock, messageId={message_id}")
            return

        # 2. 이미지 생성 (Mock: sleep)
        sleep_ms = int(os.environ.get("MOCK_SLEEP_MS", "3000"))
        time.sleep(sleep_ms / 1000)

        result = json.dumps({
            "imageUrl": f"https://mock-s3-url/characters/{cip_id}.png"
        }, ensure_ascii=False)

        # 3. 폴링용 결과 저장
        redis_setex(redis_host, redis_port, f"char:result:{cip_id}", 600, result)

        # 4. 완료 마킹
        redis_setex(redis_host, redis_port, idempotency_key, 86400, "DONE")

        print(f"Done: cipId={cip_id}, name={name}, messageId={message_id}")

    except Exception as e:
        print(f"Error processing character cipId={cip_id}: {e}")
        raise


# ─── Redis raw socket 유틸 ────────────────────────────────────────────────────

def redis_get(host, port, key):
    cmd = f"*2\r\n$3\r\nGET\r\n${len(key)}\r\n{key}\r\n"
    response = redis_exec(host, port, cmd)
    if response.startswith("$-1"):
        return None
    lines = response.split("\r\n")
    return lines[1] if len(lines) > 1 else None


def redis_setnx(host, port, key, value, ttl):
    """SET key value NX EX ttl → True if acquired"""
    cmd = (
        f"*6\r\n"
        f"$3\r\nSET\r\n"
        f"${len(key)}\r\n{key}\r\n"
        f"${len(value)}\r\n{value}\r\n"
        f"$2\r\nNX\r\n"
        f"$2\r\nEX\r\n"
        f"${len(str(ttl))}\r\n{ttl}\r\n"
    )
    response = redis_exec(host, port, cmd)
    return response.startswith("+OK")


def redis_setex(host, port, key, ttl, value):
    cmd = (
        f"*4\r\n"
        f"$5\r\nSETEX\r\n"
        f"${len(key)}\r\n{key}\r\n"
        f"${len(str(ttl))}\r\n{ttl}\r\n"
        f"${len(value.encode())}\r\n{value}\r\n"
    )
    response = redis_exec(host, port, cmd)
    if not response.startswith("+OK"):
        raise Exception(f"Redis SETEX failed: {response}")


def redis_del(host, port, key):
    cmd = f"*2\r\n$3\r\nDEL\r\n${len(key)}\r\n{key}\r\n"
    redis_exec(host, port, cmd)


def redis_exec(host, port, cmd):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.settimeout(5)
        s.connect((host, port))
        s.sendall(cmd.encode())
        return s.recv(1024).decode()
