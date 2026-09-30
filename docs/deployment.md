# 실행·배포 설정 안내

값이 어디서 와서 어디로 가는지, 그리고 내가 직접 해야 하는 일이 무엇인지 정리한 문서입니다.

---

## 1. 값이 들어오는 세 경로

비밀값은 **GitHub Secrets** 에만 둡니다. `settings.xml` 은 쓰지 않습니다.

| 환경 | 값의 출처 | 비밀값 |
| --- | --- | --- |
| 로컬 개발 | `application.yml` 의 dev 문서에 박힌 기본값 | 없음 (개발용 pepper 는 공개 값) |
| CI (PR 검사) | 필요 없음 | 없음 (테스트는 H2 + 테스트용 pepper) |
| 운영 | 배포 워크플로가 GitHub Secrets → 서버 환경 변수 | 있음 |

### 파일 구조

| 파일 | git | 용도 |
| --- | --- | --- |
| `backend/api/src/main/resources/application.yml` | ✅ 올라감 | 설정 뼈대 + **개발용 기본값** |
| `backend/api/src/main/resources/db/migration/` | ✅ 올라감 | 스키마 변경 SQL (Flyway) |
| `docker-compose.yml` | ✅ 올라감 | 개발용 MySQL 컨테이너 |
| `backend/api/Dockerfile` | ✅ 올라감 | 앱을 이미지로 만드는 설명서 |
| `backend/api/.env.dev` | ❌ 안 올라감 | 내 PC 만 기본값과 다른 값. **없어도 됨** |
| `backend/api/.env.prod` | ❌ 안 올라감 | 운영 주소·계정명 등 비밀 아닌 값 |
| GitHub Secrets | (저장소 설정) | `PASSWORD_PEPPER` `DB_PASSWORD` `JWT_PRIVATE_KEY` `JWT_PUBLIC_KEY` |

> **왜 개발용 기본값을 git 에 올리는가**
> 로컬 DB 에는 진짜 회원 정보가 없으므로 비밀이 아닙니다. 올려 두면 새 팀원이 `clone` 후 바로 실행할 수 있고, 값을 따로 공유할 필요가 없어집니다.

---

## 2. 새로 합류한 사람이 할 일

세 줄이면 끝납니다. MySQL 을 설치하거나 DB·계정을 만들 필요가 없습니다.

```bash
git clone <저장소>
docker compose up -d
cd backend/api && ./mvnw spring-boot:run
```

`docker compose up -d` 가 MySQL 컨테이너를 띄우면서 `nanumi_dev` DB 와 `abc` 계정까지 만들어 줍니다. 테이블은 앱이 뜰 때 Flyway 가 `db/migration` 의 SQL 로 만듭니다.

`application.yml` 의 개발용 기본값이 컨테이너 설정과 같으므로 `.env.dev` 도 필요 없습니다.

| | 값 |
| --- | --- |
| 주소 | `127.0.0.1:3307` |
| DB | `nanumi_dev` |
| 계정 | `abc` / `aadd123` |

### 포트는 3307 입니다

**3306 은 다른 프로젝트가 쓰는 포트로 보고 건드리지 않습니다.** 컨테이너 안팎 모두 3307 로 맞춰 두었으니(compose 의 `--port=3307`) 어디서든 3307 하나만 기억하면 됩니다.

MySQL Workbench 로 들여다볼 때도 같습니다 — Hostname `127.0.0.1`, Port `3307`, 계정 `abc`.

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


## 3. GitHub Secrets 등록하기

저장소 → **Settings** → 왼쪽 **Secrets and variables** → **Actions** → **New repository secret**

네 개를 등록합니다.

| 이름 | 값 | 만드는 방법 |
| --- | --- | --- |
| `PASSWORD_PEPPER` | 32바이트 랜덤 문자열 | 아래 명령 |
| `DB_PASSWORD` | 운영 MySQL 비밀번호 | 직접 정함 |
| `JWT_PRIVATE_KEY` | `private_key.pem` **파일 내용 전체** | 아래 명령 |
| `JWT_PUBLIC_KEY` | `public_key.pem` **파일 내용 전체** | 아래 명령 |

```bash
# pepper 생성
openssl rand -base64 32

# JWT 키 생성 (PEM 본문을 그대로 붙여 넣습니다. -----BEGIN / -----END 줄 포함)
openssl genpkey -algorithm RSA -out private_key.pem -pkeyopt rsa_keygen_bits:2048
openssl rsa -in private_key.pem -pubout -out public_key.pem
```

### 화면에서 헷갈리는 두 가지

**Secrets 탭과 Variables 탭이 나란히 있습니다.** 반드시 **Secrets** 에 넣으세요.

| | Secrets | Variables |
| --- | --- | --- |
| 등록 후 값 보기 | ❌ 불가 (덮어쓰기만) | ✅ 가능 |
| 로그에서 | 자동으로 `***` 로 가려짐 | 그대로 찍힘 |
| 용도 | 비밀번호, 키, pepper | 포트 번호, 지역 이름 |

**Repository / Environment / Organization 세 종류가 있습니다.** 지금은 **Repository secret** 으로 충분합니다. 나중에 스테이징과 운영을 나눌 때 Environment secret 을 쓰면 같은 이름으로 다른 값을 줄 수 있습니다.

### ⚠️ Secrets 는 한 번 넣으면 다시 못 봅니다

값을 확인할 방법이 없습니다. 그래서 **원본은 비밀번호 관리자(1Password, Bitwarden)에 따로 보관해야 합니다.** GitHub Secrets 는 사본일 뿐, 백업이 아닙니다.

### pepper 는 영구입니다

`PASSWORD_PEPPER` 를 바꾸면 **기존 회원 비밀번호가 전부 무효**가 되고 복구 방법이 없습니다. 모든 회원이 비밀번호를 재설정해야 합니다. 한 번 정하면 절대 바꾸지 마세요. 그리고 반드시 따로 백업하세요.

### 저장소 설정에 못 들어가는 경우

`Settings` 탭이 안 보이면 권한이 없는 것입니다. 저장소 소유자에게 요청하세요. Secrets 는 **Admin 권한**이 있어야 등록할 수 있고, Write 권한만으로는 안 됩니다.

임시로는 운영 서버에 직접 환경 변수를 넣는 방법(아래 5번)으로도 동작합니다. GitHub Secrets 는 그걸 자동화하는 수단일 뿐입니다.

---

## 4. CI 는 시크릿을 쓰지 않습니다

PR 검사(`./mvnw verify`)는 **시크릿 없이 돕니다.** 테스트가 `test` 프로필로 돌면서 인메모리 H2 와 `application-test.yml` 의 테스트용 pepper 를 쓰기 때문입니다.

일부러 이렇게 뒀습니다. 두 가지 이유입니다.

1. 외부 기여자의 PR 에는 GitHub 이 시크릿을 전달하지 않습니다. 시크릿에 의존하면 그 PR 은 항상 실패합니다.
2. 시크릿이 없을 때 GitHub 은 환경 변수를 **빈 문자열로 설정**합니다. 빈 환경 변수는 스프링에서 "값이 있음"으로 취급되어 `${VAR:기본값}` 의 기본값을 눌러 버립니다. 넘기지 않는 편이 안전합니다.

대신 `GitHub Secrets 등록 확인` 단계가 네 개가 등록됐는지만 알려 줍니다(값은 찍지 않고, 없어도 실패시키지 않습니다).

---

## 5. 운영 서버에 시크릿을 전달하는 방법

여기가 **실제로 앱이 시크릿을 받는 지점**입니다. GitHub Secrets 는 Actions 러너 안에서만 존재하므로, 배포 워크플로가 서버로 옮겨 줘야 합니다.

### 방법 A. 배포 워크플로가 SSH 로 넣기

`.github/workflows/deploy.yml` 에 이런 단계를 둡니다.

```yaml
      - name: 서버로 환경 변수 전달
        env:
          PASSWORD_PEPPER: ${{ secrets.PASSWORD_PEPPER }}
          DB_PASSWORD: ${{ secrets.DB_PASSWORD }}
          JWT_PRIVATE_KEY: ${{ secrets.JWT_PRIVATE_KEY }}
          JWT_PUBLIC_KEY: ${{ secrets.JWT_PUBLIC_KEY }}
        run: |
          # /etc/nanumi/secrets.env 를 만들어 systemd 가 읽게 함
          ssh deploy@서버 "install -m 600 /dev/null /etc/nanumi/secrets.env"
          ssh deploy@서버 "cat > /etc/nanumi/secrets.env" <<EOF
          PASSWORD_PEPPER=$PASSWORD_PEPPER
          DB_PASSWORD=$DB_PASSWORD
          JWT_PRIVATE_KEY=$JWT_PRIVATE_KEY
          JWT_PUBLIC_KEY=$JWT_PUBLIC_KEY
          EOF
          ssh deploy@서버 "systemctl restart nanumi-api"
```

서버의 systemd 유닛은 그 파일을 읽습니다.

```ini
[Service]
EnvironmentFile=/etc/nanumi/secrets.env
Environment=SPRING_PROFILES_ACTIVE=prod
ExecStart=/usr/bin/java -jar /opt/nanumi/api.jar
```

### 방법 B. 도커로 띄우기

```yaml
      - name: 배포
        env:
          PASSWORD_PEPPER: ${{ secrets.PASSWORD_PEPPER }}
          DB_PASSWORD: ${{ secrets.DB_PASSWORD }}
        run: |
          docker run -d --name nanumi-api \
            -e SPRING_PROFILES_ACTIVE=prod \
            -e PASSWORD_PEPPER="$PASSWORD_PEPPER" \
            -e DB_PASSWORD="$DB_PASSWORD" \
            nanumi/api:latest
```

### 방법 C. 손으로 한 번 넣기 (배포 워크플로가 아직 없을 때)

서버에 직접 파일을 만듭니다. GitHub Secrets 에 넣은 값과 **같은 값**을 씁니다.

```bash
sudo install -d -m 700 /etc/nanumi
sudo install -m 600 /dev/null /etc/nanumi/secrets.env
sudo tee /etc/nanumi/secrets.env > /dev/null <<'EOF'
PASSWORD_PEPPER=여기에_pepper
DB_PASSWORD=여기에_DB_비밀번호
EOF
```

JWT 키는 문자열이 길어서 파일로 두는 편이 편합니다. 그러면 `.env.prod` 에 경로만 적습니다.

```properties
JWT_PRIVATE_KEY_PATH=file:/etc/nanumi/keys/private_key.pem
JWT_PUBLIC_KEY_PATH=file:/etc/nanumi/keys/public_key.pem
```

---

## 6. 실행

```bash
# 개발용 MySQL 먼저 (한 번 띄우면 계속 떠 있음)
docker compose up -d

# 백엔드 (개발) - 시크릿 없이 그냥 돎
cd backend/api && ./mvnw spring-boot:run

# 백엔드 (운영) - 환경 변수가 있어야 돎
SPRING_PROFILES_ACTIVE=prod java -jar target/api-0.0.1-SNAPSHOT.jar

# 프런트
cd frontend && pnpm install && pnpm dev
```

### ⚠️ IDE 실행 버튼은 다릅니다

IDE 에서 `ApiApplication` 을 직접 실행하면 개발 프로필이 켜지고 `application.yml` 의 기본값을 씁니다. 여기까지는 문제 없습니다. 다만 **운영 프로필로 IDE 에서 실행하려면** Run Configuration 의 Environment variables 에 `PASSWORD_PEPPER` 등을 직접 넣어야 합니다.

---

## 7. 잘 되는지 확인

```bash
# 테스트 (DB·시크릿 없이 돎)
cd backend/api && ./mvnw verify

# 컨테이너 상태 (healthy 가 떠야 함)
docker compose ps

# DB 안을 직접 들여다보기
docker exec nanumi-mysql mysql -uabc -paadd123 -P 3307 -h 127.0.0.1 -e "SHOW TABLES FROM nanumi_dev"

# 앱이 실제로 응답하는지 (actuator 는 넣지 않았으므로 실제 엔드포인트로 확인)
curl -s -X POST localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"email":"nobody@example.com","password":"Ab3!efgh"}'
# -> 401 과 "이메일 또는 비밀번호가 올바르지 않습니다." 가 나오면 정상
```

### 기동이 막히는 경우와 원인

| 메시지 | 원인 |
| --- | --- |
| `pepper 가 비어 있음` | 운영 프로필인데 `PASSWORD_PEPPER` 환경 변수가 없음 |
| `치환되지 않은 자리표시자가 들어옴` | 위와 같음. 값이 `${PASSWORD_PEPPER}` 글자로 들어온 상태 |
| `Access denied for user ... (using password: YES)` | `DB_PASSWORD` 가 없거나 틀림 |
| `Access denied for user ... to database 'nanumi_dev'` | 계정은 맞지만 그 DB 에 권한이 없음 → `GRANT` 필요 |
| `Unknown database` | 컨테이너를 안 띄웠음 → `docker compose up -d` |
| `Connection refused` | 같음. 또는 포트를 3306 으로 잘못 씀(3307 이어야 함) |
| `wrong column type ... found [int], but expecting [bigint]` | 마이그레이션 SQL 과 엔티티 타입이 다름 |
| `Migration checksum mismatch` | 이미 적용된 마이그레이션 파일을 고쳤음 → `docker compose down -v` (개발만) |

### 왜 pepper 만 따로 가드를 두는가

`@ConfigurationProperties` 는 `@Value` 와 달리 **환경 변수가 없어도 예외를 던지지 않고** `${PASSWORD_PEPPER}` 라는 글자를 그대로 값으로 넣습니다. DB 비밀번호는 MySQL 이 거절해 주니 즉시 드러나지만, pepper 는 아무도 거절하지 않습니다. 그대로 뜬 서버가 만든 비밀번호 해시는 나중에 올바른 pepper 로는 **영구히 검증되지 않습니다.**

그래서 `NanumiPasswordProperties` 가 기동 시점에 직접 확인하고 막습니다.
