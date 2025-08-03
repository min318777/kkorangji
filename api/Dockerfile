FROM openjdk:17-slim

WORKDIR /app
COPY . .

RUN apt-get update && apt-get install -y findutils

RUN chmod +x ./gradlew && ./gradlew :api:build

COPY ./api/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
