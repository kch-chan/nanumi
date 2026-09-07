# Project Name
**nanumi**

같은 아파트(맨션) 내의 무료 나눔을 도와주는 서비스


## Project Overview
노션 참고: https://orange-molecule-36d.notion.site/

**프로젝트 목적**
- 설계 단계부터 시작해서 제조・구현 단계까지의 연습

**제조・구현 범위**
- 로그인과 회원 가입 구현
- 메인 페이지 및 게시글 작성 등의 작업은 추후 상황 보면서 작업 예정


## Getting Started

**실행 방법**
- 서비스 링크 접속하고 싶은 경우
실제 서비스 URL: `추후 URL 참고`

- 코드 실행하고 싶은 경우
1. 프로젝트 다운로드 후 터미널 2개 이상 준비
2. 각각의 터미널에 backend 실행 코드와 frontend 실행 코드 입력

**backend 실행 전 준비 (최초 1회)**

JWT 서명에 쓰는 RSA 키가 필요합니다. 키는 저장소에 올리지 않으므로 클론 후 직접 만들어야 합니다.
(만들지 않고 실행하면 `JwtTokenProvider` 에서 바로 실패합니다.)

```bash
cd "$(git rev-parse --show-toplevel)"
mkdir -p backend/api/src/main/resources/keys
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out backend/api/src/main/resources/keys/private_key.pem
openssl rsa -pubout -in backend/api/src/main/resources/keys/private_key.pem -out backend/api/src/main/resources/keys/public_key.pem
```

> 운영에서는 이 경로 대신 `JWT_PRIVATE_KEY_PATH` / `JWT_PUBLIC_KEY_PATH` 환경 변수로 키 위치를 넘깁니다.
> 그 밖에 `PASSWORD_PEPPER`, `CORS_ALLOWED_ORIGINS`, `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` 도 함께 지정해야 합니다.

**backend 실행 코드**
```bash
cd "$(git rev-parse --show-toplevel)"
cd backend/api
./mvnw spring-boot:run
```

**frontend 실행 코드**
```bash
cd "$(git rev-parse --show-toplevel)"
cd frontend/
pnpm install
pnpm dev
```

> frontend는 패키지 매니저로 **pnpm**을 사용합니다. pnpm이 없다면 `corepack enable pnpm` 으로 활성화하면 됩니다 (Node.js에 기본 포함).
