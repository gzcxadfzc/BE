#!/bin/bash
# Grafana Alloy 설치 및 설정 스크립트
#
# 사용법:
#   ./k6/setup-monitoring.sh <EC2_IP> <PEM_FILE> <REMOTE_WRITE_URL> <GF_USERNAME> <GF_API_KEY>
#
# 예시:
#   ./k6/setup-monitoring.sh 3.35.50.130 k6/littlewriter-keypair.pem \
#     https://prometheus-prod-xx.grafana.net/api/prom/push \
#     123456 \
#     glc_xxxxx

set -e

EC2_IP="${1:?'EC2 IP를 첫 번째 인자로 전달하세요'}"
PEM_FILE="${2:?'pem 파일 경로를 두 번째 인자로 전달하세요'}"
REMOTE_WRITE_URL="${3:?'Grafana Cloud Remote Write URL을 세 번째 인자로 전달하세요'}"
GF_USERNAME="${4:?'Grafana Cloud Username(숫자)을 네 번째 인자로 전달하세요'}"
GF_API_KEY="${5:?'Grafana Cloud API Key를 다섯 번째 인자로 전달하세요'}"

SSH="ssh -i $PEM_FILE -o StrictHostKeyChecking=no ec2-user@$EC2_IP"

echo "=== 1. Grafana 저장소 등록 ==="
$SSH "sudo rpm --import https://rpm.grafana.com/gpg.key"
$SSH "sudo tee /etc/yum.repos.d/grafana.repo > /dev/null <<'EOF'
[grafana]
name=grafana
baseurl=https://rpm.grafana.com
repo_gpgcheck=1
enabled=1
gpgcheck=1
gpgkey=https://rpm.grafana.com/gpg.key
EOF"

echo "=== 2. Alloy 설치 ==="
$SSH "sudo yum install -y alloy"

echo "=== 3. Alloy 설정 파일 작성 ==="
$SSH "sudo tee /etc/alloy/config.alloy > /dev/null <<EOF
prometheus.scrape \"spring_boot\" {
  targets = [{
    __address__ = \"localhost:8080\",
  }]
  metrics_path   = \"/actuator/prometheus\"
  scrape_interval = \"15s\"

  forward_to = [prometheus.remote_write.grafana_cloud.receiver]
}

prometheus.remote_write \"grafana_cloud\" {
  endpoint {
    url = \"${REMOTE_WRITE_URL}\"

    basic_auth {
      username = \"${GF_USERNAME}\"
      password = \"${GF_API_KEY}\"
    }
  }
}
EOF"

echo "=== 4. Alloy 서비스 시작 ==="
$SSH "sudo systemctl enable alloy && sudo systemctl restart alloy"

echo "=== 5. 상태 확인 ==="
sleep 3
$SSH "sudo systemctl status alloy --no-pager | head -20"

echo ""
echo "=== 설치 완료 ==="
echo "Grafana Cloud에서 15초 후부터 메트릭 수신 시작"
echo ""
echo "추천 대시보드 Import ID:"
echo "  - JVM (Micrometer): 4701"
echo "  - Spring Boot Statistics: 12685"
