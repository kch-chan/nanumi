# 데이터베이스

**기준은 `backend/api/src/main/resources/db/migration/` 의 SQL 입니다.** 이 문서와 어긋나면 SQL 이 맞습니다.

> `docs/excel/*.xlsx` 의 설계는 구현 이전에 그린 것입니다. `username`, `name`, `room_number`, 별도 `Auth_Tokens` 표 같은 항목은 **현재 스키마에 없습니다.** 처음 보는 사람은 Excel 이 아니라 이 문서와 마이그레이션을 보십시오.

| | |
| --- | --- |
| 엔진 | PostgreSQL 18 (로컬 컨테이너 · CI · Neon 모두 같게) |
| 스키마 관리 | Flyway (`V숫자__이름.sql`) |
| 검증 | `ddl-auto: validate` — 엔티티와 어긋나면 기동을 거부함 |

---

## 마이그레이션 이력

| 버전 | 내용 |
| --- | --- |
| `V1__initial_schema.sql` | `users`, `accounts` |
| `V2__refresh_tokens_table.sql` | `refresh_tokens` 추가, `accounts` 의 토큰 칼럼 제거 |
| `V3__data_erasure_logs.sql` | `data_erasure_logs` 추가 |
| `V4__terms_agreements.sql` | `terms_agreements` 추가 |

**이미 적용된 파일은 고치지 않습니다.** Flyway 가 체크섬을 기록해 두므로 내용이 바뀌면 기동이 막힙니다. 잘못된 것은 다음 번호로 보정합니다.

---

## users — 회원

사람에 대한 정보입니다. 로그인 수단은 `accounts` 로 나눠 두었습니다.

| 칼럼 | 형 | 제약 | |
| --- | --- | --- | --- |
| `id` | `INTEGER` | PK, identity | |
| `nickname` | `VARCHAR(20)` | NOT NULL, UNIQUE | 중복 검사는 대소문자를 무시함 |
| `apt_name` | `VARCHAR(100)` | NOT NULL | 나눔글이 같은 단지 안에서만 보이게 하는 기준 |
| `dong` | `VARCHAR(20)` | | |
| `ho` | `VARCHAR(20)` | | |
| `role` | `VARCHAR(20)` | NOT NULL, CHECK | `USER` / `ADMIN` |
| `status` | `VARCHAR(20)` | NOT NULL, CHECK | `ACTIVE` / `WITHDRAWN` |
| `withdrawn_at` | `TIMESTAMP(6)` | | 탈퇴 시각. 파기 기한을 세는 기준 |
| `withdrawal_reason` | `VARCHAR(255)` | | 선택 항목 |
| `created_at` `updated_at` | `TIMESTAMP(6)` | NOT NULL | JPA Auditing 이 채움 |

`role` 과 `status` 는 `@Enumerated(EnumType.STRING)` 이라 문자열로 들어갑니다. PostgreSQL 의 `ENUM` 타입 대신 `VARCHAR` + `CHECK` 를 쓰는 이유는, 값을 추가할 때 표를 잠그지 않고 마이그레이션 한 줄로 끝나기 때문입니다.

---

## accounts — 로그인 수단

| 칼럼 | 형 | 제약 | |
| --- | --- | --- | --- |
| `id` | `INTEGER` | PK, identity | |
| `user_id` | `INTEGER` | NOT NULL, UNIQUE, FK → `users.id` | 지금은 1:1 |
| `email` | `VARCHAR(100)` | NOT NULL, UNIQUE | |
| `password` | `VARCHAR(83)` | | PBKDF2 해시. 아래 참고 |
| `created_at` `updated_at` | `TIMESTAMP(6)` | NOT NULL | |

`user_id` 에 UNIQUE 를 걸어 지금은 1:1 입니다. 소셜 로그인을 붙이면 이 제약을 풀어 한 회원이 여러 계정을 갖게 됩니다 — 그래서 처음부터 표를 나눠 두었습니다.

비밀번호는 원문을 담지 않습니다. `NanumiPasswordEncoder` 가 만든 문자열 하나에 알고리즘·반복 횟수·솔트·해시가 함께 들어가므로, 반복 횟수를 나중에 올려도 기존 회원은 그대로 로그인됩니다.

> `VARCHAR(83)` 은 유지하기로 결정된 값입니다. 해시 형식을 바꾸면 길이가 달라질 수 있으므로 그때 함께 다뤄야 합니다.

**비밀번호 해시로 계정을 찾는 조회는 두지 않습니다.** 해시로 되짚을 수 있으면 같은 비밀번호를 쓰는 사람들을 한꺼번에 찾아낼 수 있기 때문입니다.

---

## refresh_tokens — 기기별 로그인 유지

기기 하나가 한 행입니다. 그래서 PC 에서 로그인해도 폰의 로그인이 끊기지 않습니다.

| 칼럼 | 형 | 제약 | |
| --- | --- | --- | --- |
| `id` | `INTEGER` | PK, identity | |
| `account_id` | `INTEGER` | NOT NULL, FK → `accounts.id` (ON DELETE CASCADE), INDEX | |
| `token_hash` | `VARCHAR(64)` | NOT NULL, UNIQUE | SHA-256 16진수 64자 |
| `expires_at` | `TIMESTAMP(6)` | NOT NULL | 발급 후 14일 |
| `created_at` `updated_at` | `TIMESTAMP(6)` | NOT NULL | 기기 수 상한을 넘었을 때 오래된 것부터 지우는 기준 |

토큰 **원문은 담지 않습니다.** 원문을 담으면 DB 가 유출됐을 때 그대로 로그인에 쓸 수 있는 자격 증명이 됩니다.

`token_hash` 에 UNIQUE 를 건 이유는 한 토큰이 두 계정을 인증하는 상태를 DB 차원에서 막기 위함입니다.

동작 규칙

- 재발급하면 쓰인 행을 지우고 새 행을 만듭니다(회전).
- 해시에 해당하는 행이 없는 토큰이 오면 **그 계정의 모든 행을 지웁니다**(재사용 감지).
- 한 계정에 최대 5행. 넘으면 오래된 것부터 지워집니다.
- 기한이 지난 행은 재발급할 때 함께 치웁니다.

> V2 이전에는 `accounts` 에 `refresh_token_hash` 한 칸만 있어서 기기 하나만 로그인을 유지할 수 있었습니다. PC 에서 로그인하면 폰의 토큰이 덮어써지고, 폰이 재발급을 시도하면 재사용으로 판단되어 양쪽 모두 로그아웃됐습니다.

---

## data_erasure_logs — 개인정보 파기 기록

약관에 "회원 탈퇴 후 30일까지" 보유한다고 안내하므로, 정말 지웠는지 보여 줄 수 있어야 합니다.

| 칼럼 | 형 | 제약 | |
| --- | --- | --- | --- |
| `id` | `INTEGER` | PK, identity | |
| `user_id` | `INTEGER` | NOT NULL, UNIQUE | 지워진 `users.id`. **FK 를 걸지 않음** |
| `withdrawn_at` | `TIMESTAMP(6)` | NOT NULL | |
| `erased_at` | `TIMESTAMP(6)` | NOT NULL | |
| `created_at` `updated_at` | `TIMESTAMP(6)` | NOT NULL | |

FK 를 걸지 않는 이유는 가리킬 행이 이미 사라졌기 때문입니다.

**이 표에 개인을 알아볼 수 있는 값은 담지 않습니다.** 번호와 시각만 남기고, 그 번호로 되짚을 수 있는 행이 없으므로 표만 봐서는 누구였는지 알 수 없습니다.

`WithdrawnDataPurgeService` 가 채웁니다. 지우는 순서는 외래키를 거스르지 않게 `refresh_tokens` → `accounts` → `terms_agreements` → `users` 입니다.

---

## terms_agreements — 약관 동의 기록

| 칼럼 | 타입 | 제약 | 비고 |
| --- | --- | --- | --- |
| `id` | `INTEGER` | PK, identity | |
| `user_id` | `INTEGER` | NOT NULL, FK → `users(id)` ON DELETE CASCADE | |
| `terms_key` | `VARCHAR(20)` | NOT NULL | `service` / `privacy` / `marketing` |
| `terms_version` | `VARCHAR(40)` | NOT NULL | 동의한 약관의 시행일자 |
| `agreed` | `BOOLEAN` | NOT NULL | 선택 약관의 **거부도 기록** |
| `agreed_at` | `TIMESTAMP(6)` | NOT NULL | |
| `created_at` `updated_at` | `TIMESTAMP(6)` | NOT NULL | |

- UNIQUE `(user_id, terms_key)` — 한 회원이 같은 약관에 두 줄을 남기지 못하게 함
- INDEX `(user_id)` — 회원 단위로 찾고 지움

**왜 필요한가.** 화면에는 동의 체크박스가 있었지만 동의했다는 사실이 어디에도 남지 않았습니다. 개인정보 보호법 제22조는 동의를 각각 알리고 받으라고 하고, **동의를 받았다는 사실의 입증 책임은 사업자에게 있습니다.** 기록이 없으면 분쟁에서 증명할 방법이 없습니다.

**판(`terms_version`)을 함께 남기는 이유.** 약관을 개정하면 그 뒤에 가입한 사람과 이전에 가입한 사람이 서로 다른 내용에 동의한 것이 됩니다. 어느 판에 동의했는지가 기록의 핵심입니다. 재동의를 따로 쌓아야 하면 UNIQUE 를 `(user_id, terms_key, terms_version)` 으로 넓혀야 합니다.

**선택 약관의 거부도 남기는 이유.** `agreed = false` 가 "물어봤고 거부했다" 는 기록입니다. 마케팅을 보내지 않았음을 증명하는 근거가 됩니다.

**파기.** `users` 에 `ON DELETE CASCADE` 로 걸려 있고, `WithdrawnDataEraser` 가 명시적으로도 지웁니다. 약관에 적은 "탈퇴 후 30일" 이 이 표에도 똑같이 적용되어야 하므로 함께 사라집니다. 파기했다는 사실은 `data_erasure_logs` 가 남깁니다.

---

## 관계

```
users 1 ──── 1 accounts 1 ──── N refresh_tokens
  │                                 (기기마다 한 행)
  ├─ 1 ──── N terms_agreements      (약관마다 한 행)
  └─ 탈퇴 후 30일 → 행이 지워지고 data_erasure_logs 에 기록만 남음
```

## 아직 없는 표

나눔글, 댓글, 찜, 신고, 알림은 구현 범위 밖입니다. 추가할 때는 `users.apt_name` 을 기준으로 같은 단지끼리만 보이게 하는 조건을 함께 설계해야 합니다.
