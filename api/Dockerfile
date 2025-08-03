FROM openjdk:17
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew && ./gradlew :api:build
COPY ./api/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
