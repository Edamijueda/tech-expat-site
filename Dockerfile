# Use the Eclipse Temurin Alpine official image
# https://hub.docker.com/_/eclipse-temurin
FROM eclipse-temurin:25-jdk-alpine

# Create and change to the app directory
WORKDIR /app

# Copy local code to the container image
COPY . ./

# Build the app
RUN ./gradlew clean build -x test

# Run the app
CMD ["sh", "-c", "java -Dspring.profiles.active=prod -jar build/libs/*.jar"]
