#!/bin/bash
# EC2 app-server에 환경변수 설정
# 사용법: ./k6/setup-env.sh <EC2_IP> <PEM_FILE>
#
# terraform output -json 으로 RDS endpoint, Redis IP 확인:
#   cd infra && terraform output app-server-env-vars

set -e

EC2_IP="${1:?'EC2 IP를 첫 번째 인자로 전달하세요'}"
PEM_FILE="${2:?'pem 파일 경로를 두 번째 인자로 전달하세요'}"
SSH="ssh -i $PEM_FILE -o StrictHostKeyChecking=no ec2-user@$EC2_IP"

# ──── 아래 값을 채우세요 (terraform output app-server-env-vars 참고) ────
JDBC_URL="jdbc:mysql://<RDS_ENDPOINT>/littlewriter?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul"
MYSQL_USERNAME="root"
MYSQL_PASSWORD="<db_password>"
REDIS_HOST="<REDIS_PRIVATE_IP>"
REDIS_PORT="6379"
# ──────────────────────────────────────────────────────────────────────────

echo "=== EC2 환경변수 설정 ==="
$SSH "sudo tee /etc/environment > /dev/null << 'ENVEOF'
JDBC_URL=\"$JDBC_URL\"
MYSQL_USERNAME=\"$MYSQL_USERNAME\"
MYSQL_PASSWORD=\"$MYSQL_PASSWORD\"
REDIS_HOST=\"$REDIS_HOST\"
REDIS_PORT=\"$REDIS_PORT\"
ENVEOF
echo '환경변수 설정 완료'"

echo ""
echo "=== /etc/environment 내용 확인 ==="
$SSH "sudo cat /etc/environment"

echo ""
echo "다음 단계: ./k6/deploy.sh $EC2_IP $PEM_FILE --init"
