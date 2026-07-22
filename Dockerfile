FROM eclipse-temurin:21-jre-jammy AS runtime

WORKDIR /app

RUN useradd --system --create-home --shell /usr/sbin/nologin monagent \
    && mkdir -p /app /var/lib/monagent /var/log/monagent \
    && chown -R monagent:monagent /app /var/lib/monagent /var/log/monagent

COPY target/monagentjava-0.1.0-SNAPSHOT.jar /app/app.jar

USER monagent

EXPOSE 8080

ENTRYPOINT ["java","-XX:+UseG1GC","-XX:MaxRAMPercentage=75","-jar","/app/app.jar"]
