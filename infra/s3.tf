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

data "archive_file" "lambda" {
  type        = "zip"
  source_file = "${path.module}/../lambda/handler.py"
  output_path = "${path.module}/lambda.zip"
}

resource "aws_s3_object" "lambda" {
  bucket = aws_s3_bucket.lambda-artifacts.id
  key    = "bip-generator.zip"
  source = data.archive_file.lambda.output_path
  etag   = data.archive_file.lambda.output_md5

  lifecycle {
    ignore_changes = [etag]
  }
}
