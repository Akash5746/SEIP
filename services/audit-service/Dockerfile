FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S seip && adduser -S seip -G seip

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

RUN mkdir -p /app/logs && chown -R seip:seip /app
USER seip

EXPOSE 8086

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:docker}", \
    "-jar", "app.jar"]
