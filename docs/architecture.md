## 기술 스택
* 추후 라이브러리의 추가/제거 등의 경우 adr(Architecture Decision Record) 작성 예정, Breaking Change의 경우는 재수정

**프론트엔드**
- React (19.x): 컴포넌트 기반 UI 구조로 재사용성과 유지보수성
- vite (8.x): webpack보다 빠른 속도
- Node.js (24버전): 안정된 버전 사용
- Typescript (~6.0.x): 런타임 오류 사전 방지하고 코드 안정성 높임
- SCSS (1.101.x): 변수 등 활용해서 CSS 작성 가능
- Zustand (5.0.x): 전역 상태 관리 (로그인, ui 상태 등)
- TanStack Query (5.101.x):  서버 데이터 관리 (서버, 캐싱, api 응답 데이터)
- axios (1.18.x): API 호출
- react-router-dom(7.18.x): 라우팅
- react-hook-form (7.80.x): 폼이 복잡해질 때 대비
- zod (4.4.x): 폼이 복잡해질 때 대비
- husky (9.1.x) + lint-staged (17.0.x): 포맷팅을 위한 라이브러리
- prettier (3.8.x): 포맷팅을 위한 라이브러리


**백엔드**
- Spring Boot (4.0.7): 자바 기반 웹 애플리케이션 구축
- Java 21: 안정적인 버전 + 17버전에는 없는 가상 스레드
- Maven: 의존성 관리
- spring-boot-starter-web: rest api
- spring-boot-starter-security: 인증
- spring-boot-starter-data-jpa: DB 연동
- h2: 인메모리 DB
- spring-boot-starter-validation: NotBlank, Size 입력 검증
- jjwt (api/impl/jackson): JWT 생성/검증
- lombok: Getter, Builder 등 보일러플레이트 제거
- checkstyle(google): 코드 포맷팅

암호화 방식
- RS256


## 디렉터리 구조
- 아래 구조의 파일명은 예시임

```
nanumi/
├─ frontend/
├─ backend/
├─ docs/
│  ├─ adr/
│  ├─ excel/
│  ├─ project-rule.md
│  ├─ architecture.md
│  ├─ database.md
│  ├─ api-spec.md
│  └─ deployment.md
├─ README.md
└─ .gitignore
```
```
frontend/
src/
├─ features/		# 기능 단위 모듈
│   ├─ auth/
│   │   ├─ components/
│   │   │   └─ LoginForm.tsx
│   │   ├─ hooks/
│   │   │   └─ useLogin.ts
│   │   ├─ api/
│   │   │   └─ authApi.ts
│   │   ├─ store/
│   │   │   └─ authStore.ts
│   │   ├─ types.ts
│   │   └─ LoginPage.tsx
│   └─ ~~/		# 추가 기능
├─ pages/		# 실제 라우트
│   └─ LoginPage.tsx		# feature/의 기능 조립
├─ assets/		# 이미지, 폰트 등
├─ components/		# 여러 feature에서 공통으로 쓰는 것만
├─ hooks/
├─ routes/	
│   └─ index.tsx
├─ store/
├─ styles/
├─ types/
├─ constants/
├─ utils/
├─ App.tsx
└─ main.tsx
```
```
backend/
src/main/
│   └─ resources/
│       ├─ application.yml
└─    └─ data.sql

src/main/java/com/nanumi/api
├─ ApiApplication.java
├─ auth/
│   ├─ controller/
│   ├─ service/
│   └─ dto/
├─ user/
│   ├─ controller/
│   │   └─ UserController.java
│   ├─ service/
│   │   └─ UserService.java
│   ├─ repository/
│   │   └─ UserRepository.java
│   ├─ entity/
│   │   └─ User.java
│   └─ dto/
│       ├─ UserRequest.java
│       └─ UserResponse.java
└─ common/
```
