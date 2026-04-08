# ─── SQS: DLQ ─────────────────────────────────────────────────────────────────

resource "aws_sqs_queue" "bip-dlq" {
  name                        = "littlewriter-bip-dlq.fifo"
  fifo_queue                  = true
  content_based_deduplication = false
  message_retention_seconds   = 1209600 # 14일 (DLQ는 길게)

  tags = { Name = "littlewriter-bip-dlq" }
}

# ─── SQS: Main Queue ──────────────────────────────────────────────────────────

resource "aws_sqs_queue" "bip-generation" {
  name                        = "littlewriter-bip-generation.fifo"
  fifo_queue                  = true
  content_based_deduplication = false
  visibility_timeout_seconds  = 300
  message_retention_seconds   = 86400 # 1일

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.bip-dlq.arn
    maxReceiveCount     = 3
  })

  tags = { Name = "littlewriter-bip-generation" }
}
