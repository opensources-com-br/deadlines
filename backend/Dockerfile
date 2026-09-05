FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace/backend

COPY backend/gradlew backend/gradlew.bat ./
COPY backend/gradle ./gradle
COPY backend/settings.gradle.kts backend/build.gradle.kts backend/gradle.properties ./
RUN ./gradlew dependencies --no-daemon

COPY backend/src ./src
RUN ./gradlew installDist --no-daemon

FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S deadlines && adduser -S -G deadlines deadlines

WORKDIR /app/backend

COPY --from=build --chown=deadlines:deadlines /workspace/backend/build/install/deadlines-backend ./
COPY --chown=deadlines:deadlines database/migrations /app/database/migrations

USER deadlines

EXPOSE 8080

ENTRYPOINT ["./bin/deadlines-backend"]
