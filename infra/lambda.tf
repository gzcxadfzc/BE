# ─── Lambda: BIP Generator ────────────────────────────────────────────────────

resource "aws_lambda_function" "bip-generator" {
  function_name    = "littlewriter-bip-generator"
  role             = aws_iam_role.lambda.arn

  s3_bucket        = aws_s3_bucket.lambda-artifacts.id
  s3_key           = aws_s3_object.lambda.key
  source_code_hash = data.archive_file.lambda.output_base64sha256

  runtime     = "python3.12"
  handler     = "handler.handler"
  timeout     = 300
  memory_size = 512

  environment {
    variables = {
      REDIS_HOST    = aws_instance.redis.public_ip
      REDIS_PORT    = "6379"
      MOCK_SLEEP_MS = "3000"
    }
  }

  tags = { Name = "littlewriter-bip-generator" }
}

# ─── Lambda: SQS Event Source Mapping ─────────────────────────────────────────

resource "aws_lambda_event_source_mapping" "bip-sqs" {
  event_source_arn        = aws_sqs_queue.bip-generation.arn
  function_name           = aws_lambda_function.bip-generator.arn
  batch_size              = 1
  function_response_types = ["ReportBatchItemFailures"]
}
