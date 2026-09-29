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

**문서**
- [docs/architecture.md](docs/architecture.md) — 기술 스택과 디렉터리 구조
- [docs/backend-code-guide.md](docs/backend-code-guide.md) — 백엔드 코드 안내 (계층 구조, 클래스별 상세, 호출 흐름)
- [docs/deployment.md](docs/deployment.md) — 설정 파일 구조, MySQL 준비, GitHub 시크릿 공유 방법
- [docs/project-rule.md](docs/project-rule.md) — 협업 규칙


## Getting Started

**실행 방법**
- 서비스 링크 접속하고 싶은 경우
실제 서비스 URL: `추후 URL 참고`

- 코드 실행하고 싶은 경우
1. 프로젝트 다운로드 후 터미널 2개 이상 준비
2. 각각의 터미널에 backend 실행 코드와 frontend 실행 코드 입력

**backend 실행 전 준비 (최초 1회)**

DB 는 MySQL 을 씁니다. 개발도 운영도 같습니다. 먼저 DB 와 계정을 만들어 주세요.

```sql
CREATE DATABASE nanumi_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'nanumi'@'localhost' IDENTIFIED BY '여기에_비밀번호';
GRANT ALL PRIVILEGES ON nanumi_dev.* TO 'nanumi'@'localhost';
```

그리고 설정 파일 두 개를 만듭니다. 둘 다 저장소에 올라가지 않습니다.

| 파일 | 담는 것 |
| --- | --- |
| `backend/api/.env.dev` | DB 주소·계정명·CORS 출처 같은 환경 값 |
| `settings.xml` (저장소 루트) | DB 비밀번호·pepper·JWT 키 같은 비밀값 |

항목별 설명과 GitHub 시크릿 공유 방법은 **[docs/deployment.md](docs/deployment.md)** 에 있습니다.

JWT 서명에 쓰는 RSA 키도 직접 만들어야 합니다.
(만들지 않고 실행하면 `JwtTokenProvider` 에서 바로 실패합니다.)

```bash
cd "$(git rev-parse --show-toplevel)/backend/api"
mkdir -p keys
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out keys/private_key.pem
openssl rsa -pubout -in keys/private_key.pem -out keys/public_key.pem
```

> 키 위치는 `.env.dev` 의 `JWT_PRIVATE_KEY_PATH` / `JWT_PUBLIC_KEY_PATH` 로 지정합니다.
> 파일 대신 PEM 본문을 `settings.xml` 에 직접 넣어도 됩니다(CI·컨테이너에서 편함).

**backend 실행 코드**
```bash
cd "$(git rev-parse --show-toplevel)"
cd backend/api
./mvnw -s ../../settings.xml spring-boot:run
```

> `-s` 는 비밀값이 든 `settings.xml` 위치를 알려 주는 것입니다.
> `~/.m2/settings.xml` 에 두면 `-s` 없이 `./mvnw spring-boot:run` 만 해도 됩니다.

**frontend 실행 코드**
```bash
cd "$(git rev-parse --show-toplevel)"
cd frontend/
pnpm install
pnpm dev
```

> frontend는 패키지 매니저로 **pnpm**을 사용합니다. pnpm이 없다면 `corepack enable pnpm` 으로 활성화하면 됩니다 (Node.js에 기본 포함).
