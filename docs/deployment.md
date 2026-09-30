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
2. PostgreSQL 버전을 **18** 로 맞춥니다. 로컬 `docker-compose.yml` 과 CI 서비스 컨테이너가 `postgres:18` 이므로 세 곳이 같아야 합니다. 주 버전이 다르면 로컬에서 통과한 마이그레이션이 운영에서 다르게 동작할 수 있습니다.
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

경로가 세 개이고 쓰는 곳이 다릅니다.

| 경로 | 보는 것 | 쓰는 곳 |
| --- | --- | --- |
| `/actuator/health/liveness` | 앱이 살아 있는지 | **Render 배포 판정** |
| `/actuator/health/readiness` | 앱 + **DB 연결** | 감시·알림 |
| `/actuator/health` | 전체 합계 | 사람이 눌러 볼 때 |

세 경로 모두 인증 없이 열려 있고 상태값만 내려줍니다. `/actuator/env` 같은 다른 경로는 닫혀 있습니다(401).

> **주의 — 지금 배포 판정에는 DB 상태가 들어가지 않습니다.** DB 가 연결되지 않아 모든 API 가 실패하는 상태에서도 `liveness` 는 `UP` 이므로 Render 는 정상으로 봅니다. 위 표처럼 나눈 것은 의도한 것이지만(Neon 유휴 중지로 멀쩡한 인스턴스가 재시작되는 것을 막기 위함), 그 대가로 **DB 장애를 자동으로 알아차릴 방법이 없습니다.**
>
> 회원을 받기 전에 `/actuator/health/readiness` 를 외부에서 주기적으로 찍고 연속 실패 시 알려 주는 감시를 붙여야 합니다. 무료로 쓸 수 있는 것으로는 UptimeRobot, Better Stack, Cronitor 등이 있습니다. "한 번 실패" 와 "계속 실패" 를 구분해야 합니다 — Neon 이 잠들었다 깨는 동안의 일시적 실패는 장애가 아닙니다.

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
| 테스트·CI | `src/test/resources/application-test.yml` — 워크플로가 띄운 `postgres:18` 서비스 컨테이너, 테스트용 pepper, 기동할 때 만드는 JWT 키 | 없음 |
| 운영 | Render·Vercel 대시보드의 환경 변수와 Secret Files | 있음 |

**GitHub Secrets 는 쓰지 않습니다.** Vercel 과 Render 가 직접 저장소를 받아 빌드하므로 배포 워크플로가 없고, 시크릿을 서버로 옮길 일이 없습니다. CI 는 테스트만 돌리며 비밀값이 필요 없습니다. 예전에 있던 "GitHub Secrets 등록 확인" 단계는 값을 아무 데도 쓰지 않아 지웠습니다 — 등록해도 실제 실행 경로에 들어가지 않으므로, 저장소에 넣어 둔 시크릿이 있으면 지워도 됩니다.

### 운영 비밀값은 누가 들고 있는가

저장소에서는 이 값들이 맞는지 확인할 수 없습니다. `git` 이 무시하는 파일이라 내용이 안 보이고, 로컬의 `.env.prod` 나 `keys/*.pem` 이 **서버에 자동으로 반영되지도 않습니다.** 로컬 파일을 고쳐도 운영은 그대로입니다.

| 값 | 어디에 들어 있는지 | 백업 | 바꿀 수 있는지 |
| --- | --- | --- | --- |
| `PASSWORD_PEPPER` | Render 환경 변수 | 구글 드라이브의 `나누미-운영-비밀값.txt` | ❌ 바꾸면 모든 비밀번호가 무효 |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | Render 환경 변수 | Neon 대시보드에서 다시 발급 가능 | ✅ Neon 에서 회전 후 Render 갱신 |
| JWT 키 쌍 | Render Secret Files (`/etc/secrets/*.pem`) | 구글 드라이브 | ✅ 바꾸면 발급된 토큰이 전부 무효(다시 로그인하면 됨) |
| `CORS_ALLOWED_ORIGINS` | Render 환경 변수 | 비밀 아님 | ✅ |
| `VITE_API_BASE_URL` | Vercel 환경 변수 | 비밀 아님 | ✅ 바꾸면 **재배포해야** 반영됨 |

회전 절차는 값마다 다릅니다. DB 비밀번호는 Neon 에서 새로 만들고 Render 를 갱신한 뒤 재배포하면 끝이지만, JWT 키를 바꾸면 그 순간 로그인한 모든 사람이 튕깁니다. pepper 는 회전 자체가 불가능합니다.

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

## 8. 운영 점검과 복구

회원을 받기 전에 이 항목들이 정해져 있어야 합니다. **아직 정해지지 않은 것은 그대로 적어 두었습니다.**

### 지금 쓰는 등급과 그 뜻

| | 등급 | 그래서 생기는 일 |
| --- | --- | --- |
| Render | free | 15분 무응답이면 인스턴스가 멈춤. 다음 요청이 수십 초 걸림. 인스턴스가 하나뿐 |
| Neon | free | 약 5분 유휴면 컴퓨트 중지. 저장 용량과 브랜치 수에 상한 |
| Vercel | Hobby | 상업적 사용 불가 조항이 있음 |

인스턴스가 하나뿐이라는 점은 시도 제한과도 얽혀 있습니다 — 아래 "시도 제한의 한계" 참고.

### 백업과 복구 — 확인해야 할 것

아래는 대시보드에서 직접 확인해야 하며, 저장소만 봐서는 알 수 없습니다.

1. **Neon 의 백업(history retention / PITR)이 켜져 있는지, 보존 기간이 며칠인지.** 무료 등급은 기간이 짧습니다.
2. **복구 권한을 누가 갖고 있는지.** 계정이 한 사람뿐이면 그 사람이 접근할 수 없게 되는 순간 복구가 불가능합니다.
3. **복구 리허설.** 한 번도 복원해 본 적 없는 백업은 백업이 아닙니다. 빈 브랜치에 복원해 보고 걸린 시간을 적어 두십시오.

### 목표를 정해야 할 것

| 항목 | 뜻 | 현재 |
| --- | --- | --- |
| RPO | 사고가 나면 몇 분 전 데이터까지 되살릴 수 있어야 하는가 | **미정** |
| RTO | 몇 분 안에 서비스가 돌아와야 하는가 | **미정** |

무료 등급으로는 둘 다 보장할 수 없습니다. 회원 데이터를 받기 시작하면 유료 등급으로 올려야 하는 지점입니다.

### 마이그레이션을 운영에 올릴 때

Flyway 는 적용 이력을 남기고 되돌리지 않습니다. 잘못 올린 것은 `V(다음 번호)` 로 보정합니다 — 이미 적용된 파일을 고치면 체크섬이 어긋나 기동이 막힙니다.

칼럼을 지우거나 이름을 바꾸는 마이그레이션은 순서를 지켜야 합니다.

1. Neon 에서 **브랜치를 하나 떠서** 그쪽에 먼저 적용해 봅니다 (무료 등급에서도 됩니다).
2. 대상 테이블에 실제로 행이 몇 개 있는지 셉니다. 없다고 가정하지 않습니다.
3. 배포 직전에 백업(또는 브랜치)을 만듭니다.
4. 배포하고 `/actuator/health/readiness` 가 `UP` 인지 봅니다.

`V2__refresh_tokens_table.sql` 은 `accounts` 의 `refresh_token_hash` / `expiry_date` 칼럼을 지웁니다. 그 칼럼에 있던 세션 정보는 보존되지 않습니다. 적용 당시 운영 DB 에 회원이 없어 옮길 데이터가 없었지만, **다음에 비슷한 마이그레이션을 올릴 때는 위 순서를 밟아야 합니다.** 비밀번호는 `accounts.password` 에 그대로 있으므로, 회원이 있는 상태였다면 다시 로그인만 하면 됩니다.

`V3__data_erasure_logs.sql` 은 표를 하나 추가할 뿐이라 기존 데이터에 영향이 없습니다.

### 배포가 잘못됐을 때

Render 는 이전 배포로 되돌릴 수 있습니다 (Deploys → 성공한 배포 → Rollback). 다만 **앱만 되돌아가고 DB 는 그대로입니다.** 마이그레이션이 이미 적용됐다면 옛 앱이 새 스키마를 보게 되므로, `ddl-auto: validate` 에 걸려 기동이 막힐 수 있습니다. 스키마를 건드린 배포는 앱 롤백만으로 복구되지 않습니다.

### 시도 제한의 한계

로그인 잠금과 가입 제한은 **인스턴스 메모리에만** 기록합니다 (`AttemptCounter`). 그래서

- 재배포하거나 무료 등급이 인스턴스를 멈추면 기록이 사라집니다.
- 인스턴스를 둘 이상으로 늘리면 요청이 나뉘어 제한도 나뉩니다.
- 기록이 상한(1만 개)에 닿으면 오래된 것부터 밀려납니다.

즉 **지금 이건 확실한 보안 통제가 아니라 현재 규모에서의 임시 방어입니다.** 실제 통제로 쓰려면 Redis 같은 공용 저장소(Upstash 무료 등급 등)나 게이트웨이 단계의 제한으로 옮겨야 합니다. 회원이 생기고 인스턴스를 늘리는 시점이 그 경계입니다.

접속자 IP 는 `TRUSTED_PROXY_COUNT` 로 읽습니다. Render 는 프록시 한 대 뒤이므로 `1` 입니다. **이 값이 없으면 모든 이용자가 프록시 IP 하나를 공유해서, 누군가 다섯 번 가입한 뒤에는 나머지 사람 전부가 429 를 받습니다.** 스프링의 `FORWARD_HEADERS_STRATEGY` 는 쓰지 않습니다 — 그 기능은 `X-Forwarded-For` 목록의 맨 앞을 접속자로 보는데, 맨 앞은 요청하는 쪽이 지어낼 수 있는 자리입니다. 대시보드에 그 변수가 남아 있으면 지워야 합니다.

### 개인정보 파기

약관은 "회원 탈퇴 후 30일까지" 보유한다고 안내합니다. `WithdrawnDataPurgeService` 가 기한이 지난 `users` / `accounts` 행을 실제로 지우고, `data_erasure_logs` 에 회원 번호와 시각만 남깁니다.

기한은 `WITHDRAWN_RETENTION_DAYS` 로 정하며 **약관 문구(`frontend/src/constants/terms.ts`)와 같아야 합니다.** 한쪽만 고치면 적어 둔 것과 실제 동작이 달라집니다.

배치는 고정 시각이 아니라 기동 2분 뒤부터 6시간 간격으로 돕니다. 무료 등급은 인스턴스가 잠들어 있을 수 있어서 "새벽 4시" 같은 시각을 잡으면 한 번도 안 돌 수 있기 때문입니다. **그래도 인스턴스가 오래 잠들어 있으면 파기가 늦어집니다.** 기한을 반드시 지켜야 하는 단계가 되면 바깥에서 주기적으로 깨우거나 플랫폼의 예약 작업으로 옮겨야 합니다.

### 아직 구현 범위 밖인 것

의도적으로 만들지 않은 것들입니다. 출시 필수인지 정해야 합니다.

| 기능 | 현재 | 없으면 생기는 일 |
| --- | --- | --- |
| 비밀번호 찾기·재설정 | 없음 | 비밀번호를 잊으면 **본인이 복구할 방법이 없습니다** |
| 이메일 소유 확인 | 없음 | 타인의 주소나 오타 주소로 가입할 수 있습니다 |
| 이메일 변경 | 없음 | — |
| 액세스 토큰 즉시 폐기 | 없음 | 로그아웃 후에도 액세스 토큰이 최대 15분 통합니다(탈퇴는 `ActiveUserGuard` 가 막습니다) |
| 리프레시 토큰을 httpOnly 쿠키로 | 없음 | XSS 가 나면 토큰을 읽을 수 있습니다 |

앞의 세 개는 메일 공급자, 일회성 토큰의 만료·사용 처리, 재발급 횟수 제한까지 함께 설계해야 하는 일감입니다.

쿠키 전환은 **지금 구조에서는 그냥 옮길 수 없습니다.** 프런트(`vercel.app`)와 백엔드(`onrender.com`)가 등록 도메인부터 달라서 그 쿠키는 서드파티 쿠키가 되고, 사파리는 이미 막고 있고 크롬도 단계적으로 막는 중입니다. 두 쪽을 같은 도메인 아래로(`nanumi.com` / `api.nanumi.com`) 옮기는 것이 선행 조건입니다. 그전까지는 `frontend/vercel.json` 의 CSP 로 "훔칠 스크립트가 아예 못 돌게" 하는 쪽을 씁니다.

### 회원을 받기 전 점검표

코드로 끝나지 않는, 운영 주체가 정해야 하는 항목입니다.

- [ ] **개인정보 보호책임자와 문의처.** `frontend/src/constants/terms.ts` 에 `개인정보 보호책임자: 나누미 운영팀`, `문의: privacy@nanumi.example` 로 되어 있습니다. `.example` 은 실제로 받을 수 없는 주소이고, 이 문구는 법적 고지이므로 **받을 사람과 실제로 메일이 도착하는 주소로 바꿔야 합니다.** 코드로 대신 정할 수 없어 남겨 두었습니다.
- [ ] Neon 백업 보존 기간과 복구 권한 확인, 복원 리허설
- [ ] RPO / RTO 결정
- [ ] `/actuator/health/readiness` 외부 감시·알림 연결
- [ ] `PASSWORD_PEPPER` 백업이 Render·GitHub 과 다른 곳에 있는지 확인
- [ ] Render 대시보드에 `TRUSTED_PROXY_COUNT=1` 이 있고 `FORWARD_HEADERS_STRATEGY` 가 없는지 확인
- [ ] Vercel 의 Preview 와 Production 환경 변수가 따로 설정되어 있는지 확인
- [ ] 로컬 `.env.prod` 와 `keys/*.pem` 이 시험용인지 실제 운영값인지 확인

---

## 9. 잘 되는지 확인

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
