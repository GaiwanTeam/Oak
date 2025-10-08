FROM docker.io/library/clojure:temurin-25-tools-deps AS builder

WORKDIR /app

COPY deps.edn ./
COPY src ./src/
COPY resources ./resources/
COPY bin ./bin/

RUN ./bin/dev uberjar

#-----------------------------------------------#

FROM docker.io/library/eclipse-temurin:25-jre-alpine AS runtime

COPY --from=builder /app/target/oak-uberjar.jar /

EXPOSE 4800

ENTRYPOINT ["java", "-jar", "/oak-uberjar.jar"]
CMD ["run"]
