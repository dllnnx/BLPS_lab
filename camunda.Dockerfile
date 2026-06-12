FROM maven:3.9.11-amazoncorretto-17-alpine AS build

WORKDIR /app

COPY pom.xml .
COPY ozon-service/pom.xml ozon-service/
COPY payment-service/pom.xml payment-service/
COPY schema-registry/pom.xml schema-registry/
COPY bpmn-listeners/pom.xml bpmn-listeners/

RUN mvn dependency:go-offline -pl bpmn-listeners -am -B --no-transfer-progress

COPY bpmn-listeners/src bpmn-listeners/src
RUN mvn package -pl bpmn-listeners -am -DskipTests --no-transfer-progress

FROM camunda/camunda-bpm-platform:run-7.21.0

COPY --from=build /app/bpmn-listeners/target/bpmn-listeners-1.0-SNAPSHOT.jar \
    /camunda/configuration/userlib/bpmn-listeners.jar

COPY ozon-service/src/main/resources/bpmn/ /camunda/configuration/resources/
COPY camunda-default.yml /camunda/configuration/default.yml
