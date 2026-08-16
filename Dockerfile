ARG JAVA_VERSION=21.0.11_10

FROM eclipse-temurin:${JAVA_VERSION}-jdk-alpine AS build
WORKDIR /workspace

RUN apk add --no-cache unzip

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw \
    && ./mvnw --batch-mode --no-transfer-progress dependency:go-offline

COPY src/ src/
RUN ./mvnw --batch-mode --no-transfer-progress clean package -DskipTests \
    && mv target/job-application-tracker-0.0.1-SNAPSHOT.jar target/application.jar \
    && java -Djarmode=tools \
        -jar target/application.jar \
        extract --layers --destination target/extracted

FROM eclipse-temurin:${JAVA_VERSION}-jre-alpine AS runtime
WORKDIR /application

RUN apk upgrade --no-cache \
    && addgroup -S application \
    && adduser -S application -G application

ENV SERVER_PORT=8080
EXPOSE 8080

COPY --from=build --chown=application:application /workspace/target/extracted/dependencies/ ./
COPY --from=build --chown=application:application /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=build --chown=application:application /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=application:application /workspace/target/extracted/application/ ./

USER application

HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=10 \
    CMD wget -q -T 5 -O /dev/null http://127.0.0.1:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "-jar", "application.jar"]
