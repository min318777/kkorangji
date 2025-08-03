FROM openjdk:17
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew && ./gradlew :consumer:build
COPY ./consumer/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
