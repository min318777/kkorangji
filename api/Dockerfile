# 빌드 스테이지
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app
COPY . .
# findutils는 대부분 이미 설치되어 있으므로 제거
# 혹시 필요하다면 apt 캐시를 정리하여 이미지 크기 최적화
RUN chmod +x ./gradlew && ./gradlew :api:build --no-daemon

# 실행 스테이지
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /app/api/build/libs/api-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
