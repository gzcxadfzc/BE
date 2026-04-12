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

# 코드 업로드는 GitHub Actions 워크플로우 전담 — Terraform은 오브젝트 존재만 관리
resource "aws_s3_object" "lambda" {
  bucket = aws_s3_bucket.lambda-artifacts.id
  key    = "bip-generator.zip"
  source = "/dev/null" # placeholder — ignore_changes = all로 실제 업로드 없음

  lifecycle {
    ignore_changes = all
  }
}
