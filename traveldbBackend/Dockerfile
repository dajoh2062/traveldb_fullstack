FROM node:24-alpine AS document-rules-audit

WORKDIR /audit

COPY scripts ./scripts
COPY src/main/resources/data/document-rules.json ./src/main/resources/data/document-rules.json

RUN node scripts/audit-document-rules.mjs --as-of "$(date -u +%F)" \
    && node --test scripts/document-rules-audit.test.mjs \
    && touch /audit-passed

FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

COPY --from=document-rules-audit /audit-passed /audit-passed
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
