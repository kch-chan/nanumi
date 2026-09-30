# 실행·배포 설정 안내

## 구성

| | 어디에 | 무엇으로 |
| --- | --- | --- |
| 프런트엔드 | **Vercel** | 정적 사이트 (`frontend/vercel.json`) |
| 백엔드 | **Render** | 도커 이미지 (`render.yaml`, `backend/api/Dockerfile`) |
| 데이터베이스 | **Neon** | PostgreSQL 18 |

세 서비스로 나눠도 코드는 한 저장소에 있고, `main` 에 병합하면 Vercel 과 Render 가 각자 알아서 배포합니다. 배포용 GitHub Actions 워크플로는 없습니다.

---

## 1. 새로 합류한 사람이 할 일

세 줄이면 끝납니다. PostgreSQL 설치나 DB·계정 생성이 필요 없습니다.

```bash
git clone <저장소>
docker compose up -d
cd backend/api && ./mvnw spring-boot:run
```

`docker compose up -d` 가 PostgreSQL 컨테이너를 띄우면서 `nanumi_dev` DB 와 `abc` 계정까지 만들어 줍니다. 테이블은 앱이 뜰 때 Flyway 가 `db/migration` 의 SQL 로 만듭니다.

`application.yml` 의 개발용 기본값이 컨테이너 설정과 같으므로 `.env.dev` 도 필요 없습니다.

| | 값 |
| --- | --- |
| 주소 | `127.0.0.1:5432` |
| DB | `nanumi_dev` |
| 계정 | `abc` / `aadd123` |

JWT 키도 받을 필요가 없습니다. `keys/*.pem` 은 `.gitignore` 대상이라 새로 받은 저장소에는 없는데, 개발 프로필은 `jwt.generate-key-if-missing: true` 라서 기동할 때 한 쌍을 만들어 씁니다. 재시작하면 기존 토큰이 무효가 되지만 다시 로그인하면 됩니다.

프런트는 따로 띄웁니다. Vite 프록시가 `/api` 를 `localhost:8080` 으로 넘겨 줍니다.

```bash
cd frontend && pnpm install && pnpm dev
```

### DB 를 처음부터 다시 만들기

```bash
docker compose down -v && docker compose up -d
```

`-v` 가 볼륨(데이터)까지 지웁니다. 적용된 마이그레이션 파일을 고쳤을 때 필요합니다 — Flyway 가 파일의 체크섬을 DB 에 저장해 두기 때문에, 파일이 바뀌면 기동을 거부합니다.

### 앱까지 컨테이너로 띄워 보기

```bash
docker compose --profile app up -d --build
```

배포 전에 "이미지가 실제로 뜨는지" 확인하는 용도입니다. 평소 개발에는 쓰지 마세요 — 코드를 고칠 때마다 이미지를 다시 만들어야 합니다.

---

## 2. 스키마 변경 (Flyway)

운영은 `ddl-auto: validate` 라 Hibernate 가 테이블을 만들지 않습니다. `db/migration` 의 SQL 이 만듭니다. **개발도 `validate` 입니다** — 그래서 마이그레이션을 빠뜨리면 내 PC 에서 바로 막히고, 배포 후에 알게 되지 않습니다.

엔티티에 필드를 추가하면 파일을 하나 추가합니다.

```sql
-- V2__add_user_phone.sql
ALTER TABLE users ADD COLUMN phone VARCHAR(20);
```

> **이미 적용된 파일은 고치지 않습니다.** Flyway 가 체크섬을 저장하므로, 고치면 기동이 거부됩니다. 잘못 썼으면 V3 로 고칩니다. 개발 중에는 `docker compose down -v` 로 넘길 수 있지만 운영에서는 방법이 없습니다.

---

## 3. Neon (데이터베이스)

1. [neon.com](https://neon.com) 에서 프로젝트를 만듭니다. 리전은 **Singapore** 가 한국에서 가장 가깝습니다.
2. PostgreSQL 버전을 **17** 로 맞춥니다 (로컬 컨테이너와 같게).
3. 연결 문자열을 복사합니다. 이런 형태입니다.

```
postgresql://nanumi_owner:비밀번호@ep-xxx.ap-southeast-1.aws.neon.tech/nanumi?sslmode=require
```

이걸 **세 조각으로 나눠서** Render 에 넣습니다. JDBC 는 계정·비밀번호를 URL 에 넣지 않습니다.

| Render 환경 변수 | 값 |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://ep-xxx.ap-southeast-1.aws.neon.tech/nanumi?sslmode=require` |
| `DB_USERNAME` | `nanumi_owner` |
| `DB_PASSWORD` | 비밀번호 |

`sslmode=require` 를 빼면 연결이 거부됩니다.

> 무료 등급은 잠시 쓰지 않으면 DB 가 자동으로 잠듭니다. 깨우는 데 몇 초 걸리고, Render 무료 등급도 15분 무응답이면 자므로 **첫 요청이 1분 가까이 걸릴 수 있습니다.** 데모에는 괜찮지만 실사용에는 유료 등급이 필요합니다.

---

## 4. Render (백엔드)

**New → Blueprint** → 이 저장소 선택. [render.yaml](../render.yaml) 을 읽어서 `nanumi-api` 를 만듭니다.

`sync: false` 인 값들을 물어봅니다. 넣어야 하는 것은 일곱 개입니다.

| 환경 변수 | 값 |
| --- | --- |
| `DB_URL` `DB_USERNAME` `DB_PASSWORD` | 위 3번의 Neon 정보 |
| `PASSWORD_PEPPER` | 백업 파일의 pepper |
| `CORS_ALLOWED_ORIGINS` | 아래 6번 참고 |

### JWT 키는 Secret Files 로 넣습니다

같은 **Environment** 화면 아래쪽 **Secret Files** → **+ Add Secret File** 에서 파일 두 개를 추가합니다. 여러 줄 값이라 환경 변수 칸에 붙여 넣으면 줄바꿈이 깨지기 쉽습니다.

| Filename | Contents |
| --- | --- |
| `private_key.pem` | 백업 파일의 개인키 — `-----BEGIN` 부터 `-----END` 까지 전체 |
| `public_key.pem` | 백업 파일의 공개키 — 전체 |

Render 는 이 파일들을 `/etc/secrets/파일명` 에 둡니다. `render.yaml` 의 `JWT_PRIVATE_KEY_PATH` / `JWT_PUBLIC_KEY_PATH` 가 그 경로를 가리키고 있으니 따로 손댈 것이 없습니다. 파일명을 정확히 위와 같이 써야 합니다.

> 환경 변수 `JWT_PRIVATE_KEY` / `JWT_PUBLIC_KEY` 에 PEM 본문을 직접 넣어도 동작합니다. 본문이 있으면 경로보다 본문이 우선하므로 둘이 충돌하지 않습니다.

`SPRING_PROFILES_ACTIVE=prod` 와 `FORWARD_HEADERS_STRATEGY=framework` 는 `render.yaml` 에 이미 있습니다.

### 모노레포라서 경로 세 칸을 맞춰야 합니다

**Settings** 화면의 세 칸입니다. Blueprint 로 만들지 않고 직접 서비스를 만들었다면 `render.yaml` 대신 이 값들이 쓰입니다.

| 항목 | 값 |
| --- | --- |
| Root Directory | `backend/api` |
| Dockerfile Path | `./Dockerfile` |
| Docker Build Context Directory | `.` |

**아래 두 칸은 Root Directory 기준입니다.** 여기에 `backend/api` 를 또 쓰면 경로가 두 번 붙어서 이런 오류가 납니다.

```
failed to read dockerfile: open Dockerfile: no such file or directory
invalid local: lstat /opt/render/project/src/backend/api/backend: no such file or directory
```

로컬에서 `docker build -t nanumi/api:latest backend/api` 로 만드는 것과 같은 구조입니다.

### 왜 Render 가 PORT 를 정하는가

Render 는 자기가 정한 포트로 앱이 듣기를 요구합니다. `application.yml` 의 `port: ${PORT:8080}` 이 그걸 따릅니다. 이게 없으면 배포는 성공하는데 접속이 안 됩니다.

### 헬스 체크

`healthCheckPath: /actuator/health/liveness` 입니다. 기동에 실패한 이미지로 교체되는 것을 막아 줍니다.

`/actuator/health` 가 아니라 `liveness` 를 쓰는 이유가 있습니다. **전자는 DB 까지 확인합니다.** Neon 무료 등급은 유휴 시 컴퓨트를 중지하므로, 그동안 헬스 체크가 들어오면 `DOWN` 이 나오고 Render 가 멀쩡한 인스턴스를 재시작해 버립니다. `liveness` 는 앱이 살아 있는지만 봅니다.

DB 까지 포함한 상태는 `/actuator/health` 로 따로 확인할 수 있습니다. 두 경로 모두 인증 없이 열려 있고 상태값만 내려줍니다. `/actuator/env` 같은 다른 경로는 닫혀 있습니다(401).

---

## 5. Vercel (프런트엔드)

**Add New → Project** → 이 저장소 선택. 모노레포이므로 설정 세 개를 맞춥니다.

| 항목 | 값 |
| --- | --- |
| Root Directory | `frontend` |
| Build Command | `pnpm build` (기본 감지됨) |
| Output Directory | `dist` |

환경 변수 하나를 넣습니다. **Render 백엔드 주소 + `/api`** 입니다.

```
VITE_API_BASE_URL = https://nanumi-api.onrender.com/api
```

`/api` 를 빼먹으면 모든 요청이 404 가 됩니다.

> **Vite 환경 변수는 빌드 시점에 코드에 박힙니다.** 값을 바꾸면 반드시 재배포해야 합니다. 대시보드에서 바꾸기만 해서는 반영되지 않습니다.

`frontend/vercel.json` 의 rewrite 는 SPA 용입니다. 이게 없으면 `/login` 을 직접 열거나 새로고침할 때 404 가 납니다.

---

## 6. CORS — 두 서비스를 서로 알려 주기

닭과 달걀 문제라 양쪽이 배포된 뒤에 합니다. Render 의 `CORS_ALLOWED_ORIGINS` 에 Vercel 주소를 넣습니다.

```
https://nanumi-neon.vercel.app,https://nanumi-neon-*.vercel.app
```

**두 번째 항목이 중요합니다.** Vercel 은 브랜치·커밋마다 프리뷰 주소를 새로 만듭니다(`nanumi-git-feat-62-kch.vercel.app` 같은 형태). 정확히 일치하는 목록만 두면 프리뷰에서 API 호출이 전부 막힙니다. `setAllowedOriginPatterns` 를 쓰므로 와일드카드가 동작합니다.

실제 도메인을 붙이면 여기에 추가합니다.

```
https://nanumi-neon.vercel.app,https://nanumi-neon-*.vercel.app,https://nanumi.com,https://www.nanumi.com
```

`www.` 도 따로 적어야 합니다. CORS 는 출처를 정확히 비교하므로 `nanumi.com` 만 있으면 `www.` 는 막힙니다.

---

## 7. 비밀값을 어디에 두는가

| 환경 | 값의 출처 | 비밀값 |
| --- | --- | --- |
| 로컬 개발 | `application.yml` 의 개발용 기본값 | 없음 |
| CI (PR 검사) | 필요 없음 (H2 + 테스트용 pepper) | 없음 |
| 운영 | Render·Vercel 대시보드의 환경 변수 | 있음 |

**GitHub Secrets 는 쓰지 않습니다.** Vercel 과 Render 가 직접 저장소를 받아 빌드하므로 배포 워크플로가 없고, 시크릿을 서버로 옮길 일이 없습니다.

### 파일 구조

| 파일 | git | 용도 |
| --- | --- | --- |
| `backend/api/src/main/resources/application.yml` | ✅ | 설정 뼈대 + 개발용 기본값 |
| `backend/api/src/main/resources/db/migration/` | ✅ | 스키마 변경 SQL |
| `docker-compose.yml` | ✅ | 개발용 PostgreSQL |
| `backend/api/Dockerfile` | ✅ | 앱을 이미지로 만드는 설명서 |
| `render.yaml` / `frontend/vercel.json` | ✅ | 배포 설계도 |
| `backend/api/.env.dev` | ❌ | 기본값과 다른 값. **없어도 됨** |
| `backend/api/.env.prod` | ❌ | 로컬에서 운영 설정 시험용. 서버에는 안 올라감 |

개발용 기본값을 git 에 올리는 이유: 로컬 DB 에는 진짜 회원 정보가 없으므로 비밀이 아닙니다. 올려 두면 새 팀원이 clone 후 바로 실행할 수 있습니다.

### ⚠️ pepper 는 영구입니다

`PASSWORD_PEPPER` 를 바꾸면 기존 회원 비밀번호가 전부 무효가 되고 복구 방법이 없습니다. Render 대시보드는 값을 다시 보여 주지만, 그래도 **원본은 따로 백업해 두십시오.**

`@ConfigurationProperties` 는 환경 변수가 없어도 예외를 던지지 않고 `${PASSWORD_PEPPER}` 라는 글자를 그대로 값으로 넣습니다. 그 상태로 뜬 서버가 만든 해시는 영구히 검증되지 않으므로, `NanumiPasswordProperties` 가 기동 시점에 막습니다.

---

## 8. 잘 되는지 확인

```bash
# 테스트 (DB·비밀값 없이 돎)
cd backend/api && ./mvnw verify

# 컨테이너 상태
docker compose ps

# DB 안 들여다보기
docker exec nanumi-db psql -U abc -d nanumi_dev -c "\dt"

# 배포된 백엔드가 살아 있는지
curl -s https://nanumi-api.onrender.com/actuator/health
# -> {"status":"UP"}
```

### 기동이 막히는 경우와 원인

| 메시지 | 원인 |
| --- | --- |
| `pepper 가 비어 있음` / `치환되지 않은 자리표시자` | `PASSWORD_PEPPER` 가 없음 |
| `class path resource [keys/private_key.pem] cannot be opened` | 운영인데 JWT 키가 없음 (Secret Files 확인) |
| `/etc/secrets/private_key.pem (No such file or directory)` | Secret File 이름이 다름 |
| 새로고침하면 로그아웃됨 | `VITE_API_BASE_URL` 이 없어 리프레시가 프런트 도메인으로 감 |
| `Connection refused` (로컬) | `docker compose up -d` 를 안 했음 |
| `The server does not support SSL` / 연결 거부 (운영) | `DB_URL` 에 `sslmode=require` 가 없음 |
| `Schema validation: missing table` | 마이그레이션이 적용되지 않았음 |
| `Migration checksum mismatch` | 이미 적용된 마이그레이션 파일을 고쳤음 |
| 프런트에서 API 404 | `VITE_API_BASE_URL` 이 없거나 `/api` 를 빼먹었음 |
| 브라우저 콘솔의 CORS 오류 | `CORS_ALLOWED_ORIGINS` 에 그 주소가 없음 |
