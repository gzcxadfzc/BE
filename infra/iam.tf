# ─── IAM: Lambda Execution Role ───────────────────────────────────────────────

resource "aws_iam_role" "lambda" {
  name = "littlewriter-lambda-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "lambda.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "lambda-basic" {
  role       = aws_iam_role.lambda.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

resource "aws_iam_role_policy" "lambda-sqs" {
  name = "littlewriter-lambda-sqs"
  role = aws_iam_role.lambda.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = [
        "sqs:ReceiveMessage",
        "sqs:DeleteMessage",
        "sqs:GetQueueAttributes"
      ]
      Resource = [
        aws_sqs_queue.bip-generation.arn,
        aws_sqs_queue.bip-dlq.arn
      ]
    }]
  })
}

# ─── IAM: EC2 App Server (SQS SendMessage) ────────────────────────────────────

resource "aws_iam_role" "app-server" {
  name = "littlewriter-app-server-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy" "app-server-sqs" {
  name = "littlewriter-app-server-sqs"
  role = aws_iam_role.app-server.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["sqs:SendMessage"]
      Resource = [aws_sqs_queue.bip-generation.arn]
    }]
  })
}

resource "aws_iam_instance_profile" "app-server" {
  name = "littlewriter-app-server-profile"
  role = aws_iam_role.app-server.name
}
