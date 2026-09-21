# syntax=docker/dockerfile:1
# Build context is the repository root so this build can bundle both the
# backend and the Astro frontend.

# Build the static website.
FROM node:24-alpine AS frontend
WORKDIR /frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM gradle:9-jdk24 AS build

ENV GRADLE_USER_HOME=/root/.gradle
WORKDIR /build

# Copy wrapper + build scripts first so dependency downloads get their own layer.
COPY backend/gradlew backend/gradlew.bat backend/settings.gradle.kts backend/build.gradle.kts backend/gradle.properties ./
COPY backend/gradle/ gradle/
RUN --mount=type=cache,target=/root/.gradle \
    --mount=type=cache,target=/root/.m2 \
    ./gradlew dependencies --no-daemon

# Copy sources and build the fat jar.
COPY backend/src/ src/
RUN --mount=type=cache,target=/root/.gradle \
    --mount=type=cache,target=/root/.m2 \
    ./gradlew shadowJar --no-daemon

FROM eclipse-temurin:24-jre-noble AS runtime

WORKDIR /app
COPY --from=build /build/build/libs/*-all.jar /app/app.jar
COPY --from=frontend /frontend/dist /app/static

ENV DATA_DIR=/data
ENV STATIC_DIR=/app/static
# Create + chown BEFORE the VOLUME declaration so a fresh named volume
# inherits the directory owned by the non-root app user.
RUN useradd -r app && mkdir -p /data && chown -R app /app /data
VOLUME /data
USER app

EXPOSE 3000

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
CMD ["serve-scheduled"]
