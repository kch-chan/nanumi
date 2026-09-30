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
- [docs/deployment.md](docs/deployment.md) — 실행·배포 설정 (로컬 준비, Vercel·Render·Neon)
- [docs/project-rule.md](docs/project-rule.md) — 협업 규칙


## Getting Started

**실행 방법**

- 배포된 서비스를 보고 싶은 경우

| | 주소 |
| --- | --- |
| 프런트엔드 | https://nanumi-neon.vercel.app |
| 백엔드 | https://nanumi-api.onrender.com |

> 무료 등급이라 한동안 접속이 없으면 서버가 잠듭니다. 첫 요청이 1분 가까이 걸릴 수 있습니다.

- 코드를 직접 실행하고 싶은 경우

**준비물은 Docker 하나입니다.** DB 를 설치하거나 계정을 만들거나 키를 만들 필요가 없습니다.

```bash
docker compose up -d
```

PostgreSQL 컨테이너가 뜨면서 `nanumi_dev` DB 와 계정까지 만들어 줍니다.
테이블은 백엔드가 뜰 때 Flyway 가 `backend/api/src/main/resources/db/migration` 의 SQL 로 만듭니다.

`application.yml` 의 개발용 기본값이 컨테이너 설정과 같으므로 `.env.dev` 도 필요 없습니다.
JWT 키도 개발 프로필에서는 기동할 때 자동으로 만들어 씁니다.

**backend 실행 코드**
```bash
cd "$(git rev-parse --show-toplevel)/backend/api"
./mvnw spring-boot:run
```

> 터미널 두 개가 필요합니다. 백엔드와 프런트엔드를 각각 띄웁니다.
> 더 자세한 내용과 배포 설정은 **[docs/deployment.md](docs/deployment.md)** 에 있습니다.

**frontend 실행 코드**
```bash
cd "$(git rev-parse --show-toplevel)"
cd frontend/
pnpm install
pnpm dev
```

> frontend는 패키지 매니저로 **pnpm**을 사용합니다. pnpm이 없다면 `corepack enable pnpm` 으로 활성화하면 됩니다 (Node.js에 기본 포함).
