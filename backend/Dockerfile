FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace/backend

COPY backend/gradlew backend/gradlew.bat ./
COPY backend/gradle ./gradle
COPY backend/settings.gradle.kts backend/build.gradle.kts backend/gradle.properties ./
RUN ./gradlew dependencies --no-daemon

COPY backend/src ./src
RUN ./gradlew installDist --no-daemon

FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S opensources && adduser -S -G opensources opensources

WORKDIR /app/backend

COPY --from=build --chown=opensources:opensources /workspace/backend/build/install/opensources-backend ./
COPY --chown=opensources:opensources database/migrations /app/database/migrations

USER opensources

EXPOSE 8080

ENTRYPOINT ["./bin/opensources-backend"]
