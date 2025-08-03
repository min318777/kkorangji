FROM openjdk:17-slim

WORKDIR /app

# 윈도우 환경에서는 COPY 전에 .dockerignore 확인
COPY . .

# 필요한 유틸리티 설치
RUN apt-get update && apt-get install -y findutils

# Gradle 실행 권한 부여 및 빌드
RUN chmod +x ./gradlew && ./gradlew :api:build --no-daemon

# JAR 복사
COPY ./api/build/libs/*.jar app.jar

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
