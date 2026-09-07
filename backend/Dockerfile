# syntax=docker/dockerfile:1

FROM gradle:9-jdk24 AS build

USER root
ENV GRADLE_USER_HOME=/root/.gradle
WORKDIR /build

# Copy wrapper + build scripts first so dependency downloads get their own layer.
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle/ gradle/
RUN --mount=type=cache,target=/root/.gradle \
    --mount=type=cache,target=/root/.m2 \
    ./gradlew dependencies --no-daemon

# Copy sources and build the fat jar.
COPY src/ src/
RUN --mount=type=cache,target=/root/.gradle \
    --mount=type=cache,target=/root/.m2 \
    ./gradlew shadowJar --no-daemon

FROM eclipse-temurin:24-jre-noble AS runtime

WORKDIR /app
COPY --from=build /build/build/libs/*-all.jar /app/app.jar

ENV DATA_DIR=/data
# Create + chown BEFORE the VOLUME declaration so a fresh named volume
# inherits the directory owned by the non-root app user.
RUN useradd -r app && mkdir -p /data && chown -R app /app /data
VOLUME /data
USER app

EXPOSE 3000

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
CMD ["serve-scheduled"]
