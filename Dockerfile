FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar order-service.jar

EXPOSE 8082

ENTRYPOINT ["java", "-Dspring.profiles.active=docker", "-jar", "order-service.jar"]