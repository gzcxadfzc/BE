# ─── Lambda: BIP Generator ────────────────────────────────────────────────────

resource "aws_lambda_function" "bip-generator" {
  function_name = "littlewriter-bip-generator"
  role          = aws_iam_role.lambda.arn

  # JAR 배포 전 placeholder로 초기 생성
  s3_bucket = aws_s3_bucket.lambda-artifacts.id
  s3_key    = "bip-generator-placeholder.zip"

  runtime = "java21"
  handler = "com.pkg.lambda.BipGeneratorHandler::handleRequest"
  timeout = 300
  memory_size = 512

  environment {
    variables = {
      REDIS_HOST    = aws_instance.redis.public_ip
      REDIS_PORT    = "6379"
      MOCK_SLEEP_MS = "3000"
    }
  }

  lifecycle {
    ignore_changes = [s3_key, s3_object_version]
  }

  tags = { Name = "littlewriter-bip-generator" }
}

# ─── Lambda: SQS Event Source Mapping ─────────────────────────────────────────

resource "aws_lambda_event_source_mapping" "bip-sqs" {
  event_source_arn                   = aws_sqs_queue.bip-generation.arn
  function_name                      = aws_lambda_function.bip-generator.arn
  batch_size                         = 1
  function_response_types            = ["ReportBatchItemFailures"]
}
