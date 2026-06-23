FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create a non-root user for security
RUN addgroup -S seip && adduser -S seip -G seip

ARG JAR_FILE=target/fraud-service-1.0.0-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

# Ensure the log directory exists and is writable
RUN mkdir -p /app/logs && chown -R seip:seip /app

USER seip

EXPOSE 8084

ENTRYPOINT ["java", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:default}", \
            "-jar", "app.jar"]
