variable "aws-region" {
  description = "AWS 리전"
  type        = string
  default     = "ap-northeast-2"
}

variable "key-name" {
  description = "EC2 키 페어 이름"
  type        = string
  default     = "littlewriter-keypair"
}

variable "allowed-ssh-cidr" {
  description = "SSH 허용 IP CIDR"
  type        = string
  default     = "0.0.0.0/0"
}

variable "db_password" {
  description = "RDS MySQL 비밀번호 (TF_VAR_db_password 환경변수로 주입)"
  type        = string
  sensitive   = true
}

variable "db-username" {
  description = "RDS MySQL 유저명"
  type        = string
  default     = "root"
}

variable "app-instance-type" {
  description = "Spring Boot 서버 인스턴스 타입"
  type        = string
  default     = "t3.small"
}

variable "redis-instance-type" {
  description = "Redis 서버 인스턴스 타입"
  type        = string
  default     = "t3.micro"
}

variable "db-instance-class" {
  description = "RDS 인스턴스 클래스"
  type        = string
  default     = "db.t3.micro"
}
