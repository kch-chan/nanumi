# 앱을 이미지로 만드는 설명서
#
# 두 단계로 나눔(멀티 스테이지):
#   1단계 build   JDK + Maven 으로 jar 를 만듦
#   2단계 runtime JRE 만 있는 작은 이미지에 그 jar 만 옮김
# 이렇게 하면 최종 이미지에 소스와 Maven 이 안 들어가서 크기가 줄고,
# 빌드 도구가 운영 컨테이너에 남지 않음
#
# 빌드:  docker build -t nanumi/api:latest backend/api
#
# 주의: 비밀값은 여기 절대 넣지 않음.
#       ENV 로 넣으면 이미지 레이어에 박혀서 이미지를 받은 누구나 읽을 수 있음.
#       비밀값은 실행할 때(docker run -e / --env-file) 넣음

# ---------- 1단계: 빌드 ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /build

# 의존성 목록만 먼저 복사해서 받아 둠
# 소스만 고쳤을 때 이 레이어를 재사용해서 빌드가 빨라짐
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw && ./mvnw -B dependency:go-offline

COPY src/ src/
# 테스트는 CI 에서 이미 돌렸으므로 이미지 빌드에서는 건너뜀
RUN ./mvnw -B -DskipTests package

# ---------- 2단계: 실행 ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# root 로 돌리지 않음. 컨테이너가 뚫렸을 때 할 수 있는 일을 줄임
RUN useradd --system --create-home --shell /usr/sbin/nologin nanumi
USER nanumi

COPY --from=build /build/target/*.jar app.jar

# 비밀 아닌 값만 둠
ENV SPRING_PROFILES_ACTIVE=prod

# 컨테이너 기본 시간대는 UTC 임. LocalDateTime.now() 가 그 시간대를 따르므로
# created_at, withdrawn_at, expiry_date 가 KST 기준 9시간 전으로 기록됨
ENV TZ=Asia/Seoul

EXPOSE 8080

# exec 형식으로 써야 java 가 PID 1 이 되어 systemd/도커의 종료 신호를 직접 받음
# 쉘 형식으로 쓰면 종료 신호가 java 에 전달되지 않아 강제 종료됨
ENTRYPOINT ["java", "-jar", "app.jar"]
