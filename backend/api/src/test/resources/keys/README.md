# 테스트 전용 키

이 폴더의 키는 **테스트에서만** 쓰는 값임. 아무 의미 없는 임시 키라서 저장소에 그대로 올려 둠.

- 테스트는 dev 프로필로 도는데, `application-dev.yml` 이 `classpath:keys/*.pem` 을 보므로
  테스트 클래스패스에 있는 이 키가 먼저 잡힘
- 운영은 `JWT_PRIVATE_KEY_PATH` / `JWT_PUBLIC_KEY_PATH` 환경 변수로 다른 키를 받도록 되어 있음

**이 키를 개발용이나 운영용으로 가져다 쓰면 안 됨.** 직접 만드는 방법은 README 참고.
