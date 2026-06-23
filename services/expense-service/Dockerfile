FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="SEIP Engineering <engineering@seip.com>"
LABEL service="expense-service"
LABEL version="1.0.0"

WORKDIR /app

# Add non-root user for security
RUN addgroup -S seip && adduser -S seip -G seip

# Create logs directory
RUN mkdir -p logs && chown -R seip:seip /app

ARG JAR_FILE=target/expense-service-1.0.0-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

# Switch to non-root user
USER seip

EXPOSE 8083

# JVM tuning for container environment
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseG1GC", \
  "-XX:+UseStringDeduplication", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:prod}", \
  "-jar", "app.jar"]
