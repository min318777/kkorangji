# 빌드 스테이지
FROM openjdk:17-slim AS builder
WORKDIR /app
COPY . .
RUN apt-get update && apt-get install -y findutils
RUN chmod +x ./gradlew && ./gradlew :api:build --no-daemon

# 실행 스테이지
FROM openjdk:17-slim
WORKDIR /app
COPY --from=builder /app/api/build/libs/api-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
