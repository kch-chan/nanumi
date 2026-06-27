
## 브랜치 전략
**Git flow 변형(main-develop-(<type>/#issueNumber-subject)) 방식**
- main: 언제든 배포 가능한 브랜치  (default branch)
- develop: 기능 통합 후 에러 및 작동 확인하는 브랜치
- (<type>/#issueNumber-subject): 작업 특징에 따라 type명 정의 후 작업하는 브랜치
```
<type>
feat: 기능 추가
chore: 빌드, 설정 등 잡무
docs: README 등 문서 작업
refactor: 코드 구조 개선
fix: 버그 수정
style: 포맷팅 등 수정
test: 테스트 코드 추가/수정
perf: 성능 개선
deploy: 배포


Breaking Change의 경우는 <type!>
```

## (<type>/#issueNumber-subject) 브랜치 작명
```
예시: 로그인 버튼 구현

1. Github issue에 feat issue 작성
2. 해당 이슈 넘버 확인 (#10으로 가정)
3. feat/10-login-button 브랜치 생성 후 작업
```

## 브랜치 보호 규칙
- main, develop 직접 push 금지
- 항상 PR을 통해 merge


## Commit 방식
- commit 명은 “<type>: 작업 내용” (예: `feat: 로그인 버튼 개발`)
- <body>는 선택 사항


## Merge 규칙
- (<type>/#issueNumber-subject) → develop: **Squash**
- develop → main: **Merge**


## 이슈 규칙
- 작업 시작 전 GitHub Issue 생성
- 작업 타입에 맞는 label 지정
- 이슈명은 "[Type] 작업 내용" (예: `[Feature] 로그인 버튼 개발`)
- <body>는 이슈 템플릿 참고


## PR 규칙
- PR 명은 “<type>: 작업 내용” (예: `feat: 로그인 버튼 개발`)
- ci.yml 같은 Github Actions으로 포맷팅 등 체크하기
- self review 후 merge 진행
- PR merge 후 origin 브랜치는 즉시 삭제
- <body>는 PR 템플릿 참고


## 부모 브랜치 내용 반영 규칙
- **merge**를 통해 내용 업데이트 진행


## 네이밍 컨벤션

- 컴포넌트: PascalCase
- 함수/변수: camelCase


- 클래스: PascalCase
- 패키지: architecture 참고


## 코드&네이밍 컨벤션
### Frontend
**네이밍 컨벤션**
- 컴포넌트: PascalCase
- 함수/변수: camelCase

**코드 컨벤션**
- 리액트 컴포넌트는 함수 선언식 사용

```jsx
function App() {
  return <div>Hello</div>;
}

export default App;
```
- 컴포넌트 내부의 일반 함수 또는 유틸 함수는 목적에 따라 함수 표현식 사용 가능

```tsx
const handleMouseEvent = (e: React.MouseEvent<HTMLButtonElement>) => {
  console.log(e);
};
```
- 그 외 컨벤션은 eslint와 prettier를 따름

### Backend
- Checkstyle (google) 플러그인으로 기본 검사
- 그 외 컨벤션은 되도록 구글 자바 스타일 가이드(https://google.github.io/styleguide/javaguide.html)를 따름



## 테스트 도구
### Frontend
- 단위/통합 테스트: Jest
- React 컴포넌트 테스트: React Testing Library
- E2E 테스트: Playwright

### Backend
- 단위 테스트: JUnit 5
- Mocking: Mockito
- API 통합 테스트: Spring Boot Test


## 환경 변수/시크릿 관리
- .env, application-local.yml 등 .gitignore 설정


## API 응답 규칙
- 추후 수정


## 예외 처리 규칙
- 추후 수정
