FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="SEIP Team"
LABEL service="api-gateway"
LABEL version="1.0.0-SNAPSHOT"

WORKDIR /app

# Create a non-root user for security
RUN addgroup -S seip && adduser -S seip -G seip

# Copy the fat jar
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

# Create log directory with correct permissions
RUN mkdir -p /app/logs && chown -R seip:seip /app

USER seip

EXPOSE 8080

# Spring profile can be overridden at runtime via SPRING_PROFILES_ACTIVE env var
ENV SPRING_PROFILES_ACTIVE=docker

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
