FROM gradle:8.14.3-jdk21-alpine AS build
WORKDIR /workspace
COPY . .
RUN gradle --no-daemon bootJar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar /app/recommendation-service.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/recommendation-service.jar"]
