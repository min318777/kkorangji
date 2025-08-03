# 빌드 스테이지
FROM openjdk:17-slim AS builder
WORKDIR /app
COPY . .
RUN apt-get update && apt-get install -y findutils
RUN chmod +x ./gradlew && ./gradlew :consumer:build --no-daemon

# 실행 스테이지
FROM openjdk:17-slim
WORKDIR /app
COPY --from=builder /app/consumer/build/libs/consumer-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
