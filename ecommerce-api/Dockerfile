FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null
COPY src src
RUN ./gradlew --no-daemon bootJar -x test \
    && mv "$(ls build/libs/*.jar | grep -v -- '-plain.jar')" /app/app.jar
RUN jdeps --ignore-missing-deps --multi-release 25 --print-module-deps /app/app.jar > /app/modules.txt \
    && jlink \
        --add-modules "$(cat /app/modules.txt),java.naming,java.management,java.sql,java.desktop,java.instrument,java.compiler,java.scripting,java.security.jgss,java.xml,java.logging,java.rmi,java.net.http,java.prefs,jdk.management,jdk.net,jdk.unsupported,jdk.crypto.ec,jdk.zipfs" \
        --strip-debug \
        --no-header-files \
        --no-man-pages \
        --compress=zip-6 \
        --output /opt/java-runtime

FROM alpine:3.22
RUN apk add --no-cache libstdc++ ca-certificates \
    && addgroup -S app && adduser -S -G app app
ENV JAVA_HOME=/opt/java-runtime
ENV PATH=/opt/java-runtime/bin:$PATH
WORKDIR /app
COPY --from=build /opt/java-runtime /opt/java-runtime
COPY --from=build /app/app.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
