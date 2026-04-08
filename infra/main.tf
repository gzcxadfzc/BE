terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws-region

  default_tags {
    tags = {
      Env       = "test"
      ManagedBy = "Terraform"
    }
  }
}

data "aws_ami" "al2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-*-x86_64"]
  }
  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

# ─── Network ──────────────────────────────────────────────────────────────────

resource "aws_vpc" "this" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  tags = { Name = "littlewriter-load-test" }
}

resource "aws_internet_gateway" "this" {
  vpc_id = aws_vpc.this.id
  tags   = { Name = "littlewriter-load-test-igw" }
}

# EC2용 퍼블릭 서브넷 (AZ-a)
resource "aws_subnet" "public-a" {
  vpc_id                  = aws_vpc.this.id
  cidr_block              = "10.0.1.0/24"
  availability_zone       = "${var.aws-region}a"
  map_public_ip_on_launch = true
  tags = { Name = "littlewriter-load-test-public-a" }
}

# RDS 서브넷 그룹용 (AZ-b, DB subnet group은 2개 AZ 필요)
resource "aws_subnet" "public-b" {
  vpc_id                  = aws_vpc.this.id
  cidr_block              = "10.0.2.0/24"
  availability_zone       = "${var.aws-region}b"
  map_public_ip_on_launch = true
  tags = { Name = "littlewriter-load-test-public-b" }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.this.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.this.id
  }

  tags = { Name = "littlewriter-load-test-rt" }
}

resource "aws_route_table_association" "public-a" {
  subnet_id      = aws_subnet.public-a.id
  route_table_id = aws_route_table.public.id
}

resource "aws_route_table_association" "public-b" {
  subnet_id      = aws_subnet.public-b.id
  route_table_id = aws_route_table.public.id
}

# ─── Security Groups ──────────────────────────────────────────────────────────

resource "aws_security_group" "app-server" {
  name   = "littlewriter-load-test-app"
  vpc_id = aws_vpc.this.id

  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.allowed-ssh-cidr]
  }
  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "littlewriter-load-test-app-sg" }
}

resource "aws_security_group" "redis" {
  name   = "littlewriter-load-test-redis"
  vpc_id = aws_vpc.this.id

  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.allowed-ssh-cidr]
  }
  ingress {
    description     = "Redis from app-server"
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [aws_security_group.app-server.id]
  }
  ingress {
    description = "Redis from Lambda (public)"
    from_port   = 6379
    to_port     = 6379
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "littlewriter-load-test-redis-sg" }
}

resource "aws_security_group" "rds" {
  name   = "littlewriter-load-test-rds"
  vpc_id = aws_vpc.this.id

  ingress {
    description = "MySQL public access"
    from_port   = 3306
    to_port     = 3306
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "littlewriter-load-test-rds-sg" }
}

# ─── EC2: App Server ──────────────────────────────────────────────────────────

resource "aws_instance" "app-server" {
  ami                    = data.aws_ami.al2023.id
  instance_type          = var.app-instance-type
  subnet_id              = aws_subnet.public-a.id
  vpc_security_group_ids = [aws_security_group.app-server.id]
  key_name               = var.key-name
  iam_instance_profile   = aws_iam_instance_profile.app-server.name

  user_data = <<-EOF
    #!/bin/bash
    dnf install -y java-21-amazon-corretto-headless
    mkdir -p /app
  EOF

  tags = { Name = "littlewriter-load-test-app" }
}

# ─── EC2: Redis ───────────────────────────────────────────────────────────────

resource "aws_instance" "redis" {
  ami                    = data.aws_ami.al2023.id
  instance_type          = var.redis-instance-type
  subnet_id              = aws_subnet.public-a.id
  vpc_security_group_ids = [aws_security_group.redis.id]
  key_name               = var.key-name

  user_data = <<-EOF
    #!/bin/bash
    dnf install -y redis
    REDIS_CONF=$(find /etc -name "redis.conf" 2>/dev/null | head -1)
    sed -i 's/^bind 127.0.0.1.*$/bind 0.0.0.0/' "$REDIS_CONF"
    sed -i 's/^protected-mode yes/protected-mode no/' "$REDIS_CONF"
    systemctl enable redis
    systemctl start redis
  EOF

  tags = { Name = "littlewriter-load-test-redis" }
}

# ─── RDS: MySQL ───────────────────────────────────────────────────────────────

resource "aws_db_subnet_group" "this" {
  name       = "littlewriter-load-test"
  subnet_ids = [aws_subnet.public-a.id, aws_subnet.public-b.id]
  tags = { Name = "littlewriter-load-test-db-subnet" }
}

resource "aws_db_instance" "mysql" {
  identifier        = "littlewriter-load-test"
  engine            = "mysql"
  engine_version    = "8.0"
  instance_class    = var.db-instance-class
  allocated_storage = 20
  storage_type      = "gp2"

  db_name  = "littlewriter"
  username = var.db-username
  password = var.db_password

  db_subnet_group_name   = aws_db_subnet_group.this.name
  vpc_security_group_ids = [aws_security_group.rds.id]

  publicly_accessible = true
  skip_final_snapshot = true
  deletion_protection = false
  multi_az            = false

  tags = { Name = "littlewriter-load-test-mysql" }
}
