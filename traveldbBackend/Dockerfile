FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

COPY pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B verify

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S traveldb && adduser -S -G traveldb traveldb

COPY --from=build --chown=traveldb:traveldb /app/target/*.jar app.jar

ENV PORT=8080
EXPOSE 8080

USER traveldb

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
