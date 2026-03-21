# =====================================================
# [기존] 레이어 캐시 미적용 버전 (비교용)
# 문제: 소스 코드 1줄 수정해도 의존성을 매번 재다운로드
# =====================================================
# FROM eclipse-temurin:17-jdk-jammy AS builder
# WORKDIR /app
# COPY . .                                                      # 소스 전체 한번에 복사
# RUN chmod +x ./gradlew && ./gradlew :api:build --no-daemon    # 의존성 + 빌드 한번에
#
# FROM eclipse-temurin:17-jre-jammy
# WORKDIR /app
# COPY --from=builder /app/api/build/libs/api-0.0.1-SNAPSHOT.jar app.jar
# ENTRYPOINT ["java", "-jar", "app.jar"]


# =====================================================
# [개선] 레이어 캐시 최적화 버전
# 효과: 소스만 수정 시 의존성 레이어 캐시 재사용 → 빌드 시간 단축
# =====================================================

# 빌드 스테이지 (멀티 스테이지 빌드)
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app

# 1단계: 의존성 파일만 먼저 복사
# → 소스 코드가 바뀌어도 이 레이어는 캐시 재사용
COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle
COPY api/build.gradle ./api/build.gradle
RUN chmod +x ./gradlew

# 2단계: 의존성 다운로드 (소스 변경 없으면 캐시 HIT → 빠른 빌드)
RUN ./gradlew dependencies --no-daemon

# 3단계: 소스 코드 복사 (자주 바뀌는 부분은 마지막에)
COPY . .

# 4단계: 빌드 (테스트 제외)
RUN ./gradlew :api:build -x test --no-daemon

# 실행 스테이지 (JRE만 포함 → 이미지 크기 최소화)
# JDK(빌드 도구 포함) → JRE(실행만 가능)로 전환
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# 빌드 스테이지에서 생성된 JAR만 복사
COPY --from=builder /app/api/build/libs/api-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]