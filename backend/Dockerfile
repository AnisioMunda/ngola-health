# syntax=docker/dockerfile:1

FROM eclipse-temurin:25-jdk-alpine AS build

WORKDIR /build

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -ntp -DskipTests package

FROM eclipse-temurin:25-jre-alpine AS runtime

WORKDIR /app

RUN addgroup -S hospitalao && adduser -S hospitalao -G hospitalao
COPY --from=build --chown=hospitalao:hospitalao /build/target/hospitalao-backend-*.jar ./app.jar

USER hospitalao
EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]