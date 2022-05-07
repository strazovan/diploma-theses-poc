FROM openjdk:8-jdk-alpine as build
WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

RUN chmod +x ./mvnw
RUN ./mvnw dependency:go-offline -B

COPY src src

# TODO there is an option for further optimization by extracting data from the jar
RUN ./mvnw package -DskipTests

FROM openjdk:8-slim
COPY --from=build /app/target/agent-sidecar-0.1.jar sidecar.jar
ENTRYPOINT ["java","-jar","/sidecar.jar"]