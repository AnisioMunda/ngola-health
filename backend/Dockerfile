# ============================================================
# HospitalAO Backend — Dockerfile (Multi-stage)
# ============================================================

# ==============================================================
# STAGE 1 — development
# ==============================================================
FROM maven:3.9-eclipse-temurin-21-alpine AS development

WORKDIR /workspace

# Cache das dependências Maven
COPY pom.xml .
RUN mvn dependency:go-offline -B --no-transfer-progress

# Copiar código fonte
COPY src ./src

# Compilar e arrancar a aplicação
# "-Dspring-boot.run.fork=false" evita fork de processo que causa loop de CPU
# "-Dspring-devtools.restart.enabled=false" desactiva hot-reload por agora
RUN mvn package -DskipTests -B --no-transfer-progress

EXPOSE 8080

CMD ["java", \
     "-Xms256m", "-Xmx512m", \
     "-Dspring.devtools.restart.enabled=false", \
     "-jar", "target/hospitalao-backend-0.0.1-SNAPSHOT.jar"]


# ==============================================================
# STAGE 2 — builder
# ==============================================================
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline -B --no-transfer-progress

COPY src ./src

RUN mvn package -DskipTests -B --no-transfer-progress

RUN java -Djarmode=layertools \
    -jar target/*.jar extract \
    --destination target/extracted


# ==============================================================
# STAGE 3 — production
# ==============================================================
FROM eclipse-temurin:21-jre-alpine AS production

WORKDIR /app

RUN addgroup -S hospitalao && \
    adduser -S hospitalao -G hospitalao

COPY --from=builder /build/target/extracted/dependencies/ ./
COPY --from=builder /build/target/extracted/spring-boot-loader/ ./
COPY --from=builder /build/target/extracted/snapshot-dependencies/ ./
COPY --from=builder /build/target/extracted/application/ ./

RUN chown -R hospitalao:hospitalao /app
USER hospitalao

EXPOSE 8080

ENTRYPOINT ["java", \
            "-Xms256m", "-Xmx512m", \
            "-XX:+UseContainerSupport", \
            "-cp", "BOOT-INF/lib/*:.", \
            "org.springframework.boot.loader.launch.JarLauncher"]