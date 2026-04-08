output "app-server-public-ip" {
  description = "Spring Boot 서버 퍼블릭 IP (k6 타겟)"
  value       = aws_instance.app-server.public_ip
}

output "redis-private-ip" {
  description = "Redis 서버 프라이빗 IP"
  value       = aws_instance.redis.private_ip
}

output "rds-endpoint" {
  description = "RDS MySQL 엔드포인트"
  value       = aws_db_instance.mysql.endpoint
}

output "app-server-env-vars" {
  description = "app-server에 설정할 환경변수 (prod profile)"
  value       = <<-EOT

    REDIS_HOST=${aws_instance.redis.private_ip}
    REDIS_PORT=6379
    JDBC_URL=jdbc:mysql://${aws_db_instance.mysql.endpoint}/littlewriter?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
    MYSQL_USERNAME=${var.db-username}
    MYSQL_PASSWORD=<db_password 변수 값>
    AWS_ACCESS_KEY=<값>
    AWS_SECRET_KEY=<값>

  EOT
}

output "ssh-app-server" {
  value = "ssh -i <key>.pem ec2-user@${aws_instance.app-server.public_ip}"
}

output "ssh-redis" {
  value = "ssh -i <key>.pem ec2-user@${aws_instance.redis.public_ip}"
}

output "k6-base-url" {
  description = "k6 스크립트에서 사용할 BASE_URL"
  value       = "http://${aws_instance.app-server.public_ip}:8080"
}

output "sqs-queue-url" {
  description = "SQS FIFO 큐 URL (app-server 환경변수 SQS_QUEUE_URL)"
  value       = aws_sqs_queue.bip-generation.url
}

output "lambda-function-name" {
  description = "Lambda 함수명 (배포 스크립트용)"
  value       = aws_lambda_function.bip-generator.function_name
}

output "lambda-artifacts-bucket" {
  description = "Lambda JAR 업로드 S3 버킷명"
  value       = aws_s3_bucket.lambda-artifacts.id
}

output "redis-public-ip" {
  description = "Redis 퍼블릭 IP (Lambda 환경변수 REDIS_HOST)"
  value       = aws_instance.redis.public_ip
}

output "app-server-env-vars-v3" {
  description = "v3 추가 환경변수"
  value       = <<-EOT

    SQS_QUEUE_URL=${aws_sqs_queue.bip-generation.url}
    AWS_REGION=${var.aws-region}

  EOT
}
