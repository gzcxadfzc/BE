#!/bin/bash
# 사용법: ./k6/deploy.sh <EC2_IP> <PEM_FILE>
#
# EC2에 사전 설정 필요한 환경변수 (/etc/environment):
#   JDBC_URL, MYSQL_USERNAME, MYSQL_PASSWORD
#   REDIS_HOST, REDIS_PORT
#
# 예시:
#   ./k6/deploy.sh 3.35.50.130 ~/keys/littlewriter-keypair.pem

set -e

EC2_IP="${1:?'EC2 IP를 첫 번째 인자로 전달하세요'}"
PEM_FILE="${2:?'pem 파일 경로를 두 번째 인자로 전달하세요'}"
SSH="ssh -i $PEM_FILE -o StrictHostKeyChecking=no ec2-user@$EC2_IP"
SCP="scp -i $PEM_FILE -o StrictHostKeyChecking=no"

echo "=== 1. JAR 빌드 ==="
./gradlew :api:bootJar -x test

JAR_PATH=$(ls api/build/libs/api-*.jar 2>/dev/null | head -1)
if [[ -z "$JAR_PATH" ]]; then
  echo "JAR 파일을 찾을 수 없습니다."
  exit 1
fi

echo "=== 2. /app 디렉터리 확인 ==="
$SSH "mkdir -p /app"

echo "=== 3. JAR 업로드 ==="
$SCP "$JAR_PATH" "ec2-user@$EC2_IP:/app/api.jar"

echo "=== 4. start.sh 업로드 ==="
$SCP "k6/start.sh" "ec2-user@$EC2_IP:/app/start.sh"
$SSH "chmod +x /app/start.sh"

echo "=== 5. 기존 앱 종료 ==="
$SSH "pkill -f 'java.*api.jar' || true; sleep 2"

echo "=== 6. 앱 기동 ==="
$SSH "nohup bash /app/start.sh > /app/app.log 2>&1 &"

echo "=== 7. 헬스체크 (최대 60초 대기) ==="
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
