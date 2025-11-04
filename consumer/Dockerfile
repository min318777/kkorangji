# 빌드 스테이지
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app
COPY . .
RUN apt-get update && apt-get install -y findutils
RUN chmod +x ./gradlew && ./gradlew :consumer:build --no-daemon

# 실행 스테이지
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /app/consumer/build/libs/consumer-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
