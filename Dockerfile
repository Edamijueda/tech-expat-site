# Use the Eclipse Temurin Alpine official image
# https://hub.docker.com/_/eclipse-temurin
FROM eclipse-temurin:25-jdk-alpine

# Create and change to the app directory
WORKDIR /app

# Copy local code to the container image
COPY . ./

# Build the app and rename JAR to a known filename
RUN ./gradlew clean build -x test && cp build/libs/*.jar app.jar

# Run the app
CMD ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
