terraform {
  backend "s3" {
    bucket  = "littlewriter-terraform-032736241918-ap-northeast-2-an"
    key     = "load-test/terraform.tfstate"
    region  = "ap-northeast-2"
    encrypt = true
  }
}
