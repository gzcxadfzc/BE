data "aws_caller_identity" "current" {}

# ─── S3: Lambda 아티팩트 ───────────────────────────────────────────────────────

resource "aws_s3_bucket" "lambda-artifacts" {
  bucket        = "littlewriter-lambda-artifacts-${data.aws_caller_identity.current.account_id}-${var.aws-region}"
  force_destroy = true

  tags = { Name = "littlewriter-lambda-artifacts" }
}

resource "aws_s3_bucket_versioning" "lambda-artifacts" {
  bucket = aws_s3_bucket.lambda-artifacts.id

  versioning_configuration {
    status = "Enabled"
  }
}

