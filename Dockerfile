FROM eclipse-temurin:25-jdk-alpine
LABEL maintainer="baeldung.com"

COPY target/pr-review-app-1.0.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
