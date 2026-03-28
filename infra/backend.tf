terraform {
  backend "s3" {
    bucket  = "littlewriter-terraform"
    key     = "load-test/terraform.tfstate"
    region  = "ap-northeast-2"
    encrypt = true
  }
}
