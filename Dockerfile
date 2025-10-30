# Build stage
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /app

# Copy gradle wrapper files
COPY gradlew ./
COPY gradle gradle

# Make gradlew executable
RUN chmod +x gradlew

COPY build.gradle settings.gradle ./

RUN ./gradlew dependencies --no-daemon || true

COPY src src

# Accept version as build argument
ARG VERSION
RUN if [ -n "$VERSION" ]; then \
      ./gradlew bootJar -Pversion="$VERSION" --no-daemon; \
    else \
      ./gradlew bootJar --no-daemon; \
    fi

# Runtime stage
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

COPY --from=builder /app/build/libs/babelbot.jar app.jar

# Create a non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
