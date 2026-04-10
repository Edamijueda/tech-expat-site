# Use the Eclipse Temurin Alpine official image
# https://hub.docker.com/_/eclipse-temurin
FROM eclipse-temurin:25-jdk-alpine

# Create and change to the app directory
WORKDIR /app

# Copy local code to the container image
COPY . ./

# Build the app
RUN ./gradlew -DskipTests clean build

# Run the app by dynamically finding the JAR file in the build/libs directory
CMD ["sh", "-c", "java -Dspring.profiles.active=prod -jar build/libs/*.jar"]
