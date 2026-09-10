# syntax=docker/dockerfile:1

FROM --platform=$BUILDPLATFORM eclipse-temurin:25-jdk AS build
WORKDIR /src

COPY gradlew ./
COPY gradle ./gradle
COPY settings.gradle* build.gradle* ./
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies || true

COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test

######
RUN cp build/libs/*.jar /tmp/app.jar \
 && mkdir -p /out && cd /out \
 && java -Djarmode=tools -jar /tmp/app.jar extract --layers --destination .

######
FROM eclipse-temurin:25-jre
WORKDIR /app

RUN groupadd -r app && useradd -r -g app app \
 && apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*

COPY --from=build --chown=app:app /out/dependencies/ ./
COPY --from=build --chown=app:app /out/spring-boot-loader/ ./
COPY --from=build --chown=app:app /out/snapshot-dependencies/ ./
COPY --from=build --chown=app:app /out/application/ ./

USER app
EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="\
-Duser.timezone=Asia/Seoul \
-Dfile.encoding=UTF-8"

ENTRYPOINT ["java", "-jar", "app.jar"]
