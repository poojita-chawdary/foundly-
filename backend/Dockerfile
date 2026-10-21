FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src src
RUN mvn -B verify
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 foundly && mkdir -p /data/images && chown -R foundly /data
COPY --from=build /app/target/foundly-1.0.0.jar app.jar
USER foundly
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
