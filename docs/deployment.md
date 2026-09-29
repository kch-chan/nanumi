# 실행·배포 설정 안내

설정이 어디에 있고, 무엇을 직접 채워야 하는지 정리한 문서입니다.

## 1. 설정 파일 구조

파일은 세 종류입니다. **비밀인지 아닌지**로 나눠 놨습니다.

| 파일 | git | 담는 것 |
| --- | --- | --- |
| `backend/api/src/main/resources/application.yml` | 커밋함 | 설정의 뼈대. 값은 `${}` 로 비워 둠 |
| `backend/api/.env.dev` · `.env.prod` | **안 함** | 환경마다 달라지는 값 (DB 주소, 계정명, CORS 출처) |
| `settings.xml` (저장소 루트) | **안 함** | 비밀값 (DB 비밀번호, pepper, JWT 키) |

`application.yml` 은 한 파일 안에서 `---` 로 프로필을 나눕니다. 위쪽이 공통, 아래 두 문서가 각각 `dev` / `prod` 입니다. 프로필에 따라 `.env.dev` 또는 `.env.prod` 를 읽습니다.

```
application.yml ──┬── (공통)
                  ├── dev  → .env.dev  를 읽음
                  └── prod → .env.prod 를 읽음

settings.xml ── pom.xml ── 환경 변수 ──→ 앱
```

## 2. MySQL 준비 (직접 하셔야 하는 부분)

H2 를 쓰지 않습니다. 개발도 MySQL 로 붙습니다.

```sql
CREATE DATABASE nanumi_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'nanumi'@'localhost' IDENTIFIED BY '여기에_비밀번호';
GRANT ALL PRIVILEGES ON nanumi_dev.* TO 'nanumi'@'localhost';
FLUSH PRIVILEGES;
```

운영용은 `nanumi_dev` 대신 `nanumi` 로 하나 더 만들면 됩니다.

### 그리고 이 값들을 채웁니다

| 어느 파일 | 어느 변수 | 무엇으로 |
| --- | --- | --- |
| `backend/api/.env.dev` | `DB_URL` | 호스트·포트·DB 이름. 기본값은 `localhost:3306/nanumi_dev` |
| `backend/api/.env.dev` | `DB_USERNAME` | 위에서 만든 계정명 |
| `settings.xml` | `nanumi.db.password` | 위에서 정한 비밀번호 |

운영은 `.env.prod` 의 같은 이름 변수를 고칩니다.

> `ddl-auto` 가 dev 는 `update`(엔티티 바꾸면 테이블도 따라감), prod 는 `validate`(스키마를 건드리지 않고 다르면 기동 거부) 입니다. 운영 스키마는 사람이 직접 반영해야 합니다.

## 3. 비밀값 (settings.xml)

`settings.xml` 은 Maven 설정 파일입니다. 여기 적은 값을 `pom.xml` 이 환경 변수로 바꿔서 앱에 넘깁니다.

| settings.xml 속성 | 환경 변수 | 쓰이는 곳 |
| --- | --- | --- |
| `nanumi.db.password` | `DB_PASSWORD` | DB 접속 |
| `nanumi.password.pepper` | `PASSWORD_PEPPER` | 비밀번호 해싱 |
| `nanumi.jwt.private-key` | `JWT_PRIVATE_KEY` | JWT 서명 (PEM 본문) |
| `nanumi.jwt.public-key` | `JWT_PUBLIC_KEY` | JWT 검증 (PEM 본문) |

실행할 때 `-s` 로 위치를 알려 줍니다.

```bash
cd backend/api
./mvnw -s ../../settings.xml spring-boot:run
```

`~/.m2/settings.xml` 에 두면 `-s` 없이도 자동으로 읽힙니다.

### pepper 주의

`openssl rand -base64 32` 로 만듭니다. **한 번 정하면 바꿀 수 없습니다.** 바꾸면 기존 회원 비밀번호가 전부 안 맞게 됩니다. 반드시 따로 백업하세요.

### JWT 키

키 파일을 쓰는 방법과 PEM 본문을 직접 넣는 방법 둘 다 됩니다.

```bash
cd backend/api
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out keys/private_key.pem
openssl rsa -in keys/private_key.pem -pubout -out keys/public_key.pem
```

- **파일**: `.env.*` 의 `JWT_PRIVATE_KEY_PATH` 에 경로를 적습니다 (로컬 개발에 편함)
- **본문**: `settings.xml` 의 `nanumi.jwt.private-key` 에 PEM 을 통째로 붙여넣습니다 (CI·컨테이너에 편함)

본문이 비어 있지 않으면 본문이 우선합니다.

## 4. GitHub 에 settings.xml 공유하기

팀원끼리 비밀값을 나누는 방법입니다. 저장소 관리자 권한이 있으면 1번, 없으면 4-3 을 보세요.

### 4-1. 시크릿 등록

1. GitHub 저장소 → **Settings** 탭
2. 왼쪽 메뉴 아래쪽 **Secrets and variables** → **Actions**
3. **New repository secret** 버튼
4. Name 에 `MAVEN_SETTINGS`
5. Secret 칸에 `settings.xml` **파일 전체 내용**을 붙여넣기 (여러 줄 그대로)
6. **Add secret**

등록한 뒤에는 값을 다시 볼 수 없습니다. 덮어쓰기만 됩니다.

### 4-2. CI 가 쓰는 방식

`.github/workflows/ci.yml` 에 복원 단계를 넣어 뒀습니다.

```yaml
- name: Maven settings.xml 복원
  env:
    MAVEN_SETTINGS: ${{ secrets.MAVEN_SETTINGS }}
  run: |
    mkdir -p ~/.m2
    printf '%s' "$MAVEN_SETTINGS" > ~/.m2/settings.xml
```

`~/.m2/settings.xml` 은 Maven 이 기본으로 읽는 위치라, 뒤따르는 `./mvnw verify` 가 `-s` 없이 그대로 씁니다. 시크릿이 없으면 이 단계는 그냥 넘어가고 `pom.xml` 의 개발용 기본값으로 빌드됩니다. 즉 **외부 기여자가 보낸 PR 도 CI 가 깨지지 않습니다.**

### Secrets 와 Variables 차이

같은 화면에 탭이 두 개 있습니다.

| | Secrets | Variables |
| --- | --- | --- |
| 값 다시 보기 | 불가 | 가능 |
| 로그 출력 | `***` 로 가려짐 | 그대로 찍힘 |
| 쓸 것 | 비밀번호, pepper, 개인키 | DB 호스트, CORS 출처 같은 공개 가능한 값 |

`.env.*` 에 들어가는 값들은 Variables 로 올려도 됩니다. `settings.xml` 내용은 반드시 Secrets 입니다.

### 4-3. 저장소 설정을 못 건드리는 경우

Settings 탭이 안 보이면 권한이 없는 것입니다. 이럴 때는:

- **관리자에게 요청**: 위 4-1 절차를 대신 해 달라고 하면 됩니다
- **개인 fork 에서 시험**: 본인 fork 의 Settings 에는 시크릿을 넣을 수 있습니다
- **CI 없이 로컬만**: `settings.xml` 을 팀원끼리 안전한 경로(1Password, 사내 메신저 DM 등)로 직접 주고받고 각자 `~/.m2/settings.xml` 에 둡니다. 카카오톡·이메일·Slack 공개 채널은 기록이 남으므로 피하세요

## 5. 실행

```bash
# 백엔드 (개발)
cd backend/api
./mvnw -s ../../settings.xml spring-boot:run

# 백엔드 (운영)
./mvnw -s ../../settings.xml spring-boot:run -Dspring-boot.run.profiles=prod

# 프런트
cd frontend
pnpm install
pnpm dev
```

## 6. 잘 되는지 확인

```bash
# settings.xml 값이 Maven 에 들어왔는지
./mvnw -s ../../settings.xml help:evaluate -Dexpression=nanumi.password.pepper -DforceStdout

# 테스트
cd backend/api && ./mvnw -s ../../settings.xml verify
cd frontend && pnpm test
```

앱을 띄웠을 때 `Communications link failure` 가 나오면 MySQL 이 안 떠 있거나 `DB_URL` 이 틀린 것입니다. `Access denied` 면 계정·비밀번호가 틀린 것입니다.

> ⚠️ `.env.*` 나 `settings.xml` 을 통째로 빠뜨려도 **앱은 그냥 뜹니다.** `@ConfigurationProperties` 는 채우지 못한 `${}` 를 문자열 그대로 남깁니다. DB 는 접속에 실패해서 바로 알 수 있지만, `PASSWORD_PEPPER` 와 `CORS_ALLOWED_ORIGINS` 는 조용히 잘못된 값으로 동작합니다. 배포 전에 직접 확인하세요.
