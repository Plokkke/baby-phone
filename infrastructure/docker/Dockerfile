# Runtime-only image: the fat jar is built once by Gradle (architecture independent),
# so multi-arch images (arm64 for the Raspberry Pi) need no emulated compilation.
# Build context: components/server/build/libs
FROM eclipse-temurin:21-jre
RUN useradd --system --no-create-home babyphone
USER babyphone
COPY babyphone-server.jar /app/server.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/server.jar"]
