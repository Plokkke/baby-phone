FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle gradle
COPY components/shared components/shared
COPY components/server components/server
RUN ./gradlew :server:buildFatJar -PskipAndroid --no-daemon -q

FROM eclipse-temurin:21-jre
RUN useradd --system --no-create-home babyphone
USER babyphone
COPY --from=build /src/components/server/build/libs/babyphone-server.jar /app/server.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/server.jar"]
