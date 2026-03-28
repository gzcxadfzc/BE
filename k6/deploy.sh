#!/bin/bash
# 사용법: ./k6/deploy.sh <EC2_IP> <PEM_FILE> [--init]
# --init : 최초 배포 시 사용 (RDS 스키마 생성, ddl-auto=create)
#
# EC2에 사전 설정 필요한 환경변수 (/etc/environment 또는 ~/.bashrc):
#   JDBC_URL, MYSQL_USERNAME, MYSQL_PASSWORD
#   REDIS_HOST, REDIS_PORT
#   AWS_ACCESS_KEY, AWS_SECRET_KEY
#   JWT_CRYPTO_PRIVATE_KEY, JWT_CRYPTO_PUBLIC_KEY
#
# 예시:
#   최초 배포: ./k6/deploy.sh 1.2.3.4 ~/keys/littlewriter-keypair.pem --init
#   이후 배포: ./k6/deploy.sh 1.2.3.4 ~/keys/littlewriter-keypair.pem

set -e

EC2_IP="${1:?'EC2 IP를 첫 번째 인자로 전달하세요'}"
PEM_FILE="${2:?'pem 파일 경로를 두 번째 인자로 전달하세요'}"
INIT_MODE="${3:-}"
JAR_PATH=$(ls api/build/libs/api-*.jar 2>/dev/null | head -1)
if [[ -z "$JAR_PATH" ]]; then
  echo "JAR 파일을 찾을 수 없습니다. 먼저 빌드하세요: ./gradlew :api:bootJar -x test"
  exit 1
fi
SSH="ssh -i $PEM_FILE -o StrictHostKeyChecking=no ec2-user@$EC2_IP"

DDL_AUTO="validate"
if [[ "$INIT_MODE" == "--init" ]]; then
  DDL_AUTO="create"
  echo ">>> 초기화 모드: ddl-auto=create (스키마 신규 생성)"
fi

echo "=== 1. JAR 빌드 ==="
./gradlew :api:bootJar -x test

echo "=== 2. /app 디렉터리 확인 ==="
$SSH "mkdir -p /app"

echo "=== 3. JAR 업로드 ==="
scp -i "$PEM_FILE" -o StrictHostKeyChecking=no "$JAR_PATH" "ec2-user@$EC2_IP:/app/api.jar"

echo "=== 4. 기존 앱 종료 ==="
$SSH "pkill -f 'java.*api.jar' || true; sleep 2"

echo "=== 5. 앱 기동 (prod,load-test 프로파일) ==="
$SSH "set -a; source /etc/environment; set +a; \
nohup java -jar /app/api.jar \
  --spring.profiles.active=prod,load-test \
  --spring.jpa.hibernate.ddl-auto=$DDL_AUTO \
  > /app/app.log 2>&1 &
echo 'PID: '\$!"

echo "=== 6. 헬스체크 (최대 60초 대기) ==="
for i in $(seq 1 12); do
  sleep 5
  if $SSH "curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1"; then
    echo "앱 기동 완료 (${i}*5초)"
    break
  fi
  echo "대기 중... ($((i*5))초)"
  if [[ $i -eq 12 ]]; then
    echo "헬스체크 실패. 로그 확인:"
    $SSH "tail -80 /app/app.log"
    exit 1
  fi
done

echo ""
echo "=== 배포 완료 ==="
echo "BASE_URL=http://$EC2_IP:8080"
echo ""
echo "k6 실행:"
echo "  k6 run -e BASE_URL=http://$EC2_IP:8080 k6/complete-book.js"
