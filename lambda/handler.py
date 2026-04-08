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
    bip_id = body["bipId"]
    page_index = body.get("pageIndex", 0)

    redis_host = os.environ["REDIS_HOST"]
    redis_port = int(os.environ.get("REDIS_PORT", "6379"))

    idempotency_key = f"idempotency:{message_id}"

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


def redis_exec(host, port, cmd):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.settimeout(5)
        s.connect((host, port))
        s.sendall(cmd.encode())
        return s.recv(1024).decode()
