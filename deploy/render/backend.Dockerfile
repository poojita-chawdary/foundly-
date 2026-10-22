FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY backend/pom.xml .
COPY backend/src src
RUN mvn -B -ntp verify

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends gosu curl ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 foundly \
    && useradd --system --uid 10001 --gid foundly foundly
COPY backend/src/main/resources/prod-ca-2021.crt /usr/local/share/ca-certificates/supabase.crt
RUN update-ca-certificates
COPY --from=build /app/target/foundly-1.0.0.jar /app/app.jar
COPY backend/src/main/resources/prod-ca-2021.crt /app/prod-ca-2021.crt
COPY deploy/render/start-backend.sh /app/start-backend.sh
RUN sed -i 's/\r$//' /app/start-backend.sh && chmod 755 /app/start-backend.sh && chown -R foundly:foundly /app
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=120s --retries=5 \
    CMD curl --fail --silent http://127.0.0.1:8080/actuator/health || exit 1
ENTRYPOINT ["/app/start-backend.sh"]
