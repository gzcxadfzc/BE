ssh를 사용하여 각 인스턴스에 접근 할 수 있음

littlewriter-keypair.pem 을 사용할 것

부하 테스트용 인스턴스 주소: ec2-3-35-50-130.ap-northeast-2.compute.amazonaws.com
redis용 인스턴스 주소: ec2-43-203-128-153.ap-northeast-2.compute.amazonaws.com

배포는 다음과 같은 절차를 따를 것.

1. 배포 스크립트는 무조건 프로젝트를 기준으로 할 것.
2. ssh 내부에서 임의로 변경하지 말것.
3. 변경이 필요하다면 프로젝트 파일에서 변경후 scp를 통해 옮겨서 사용할것.