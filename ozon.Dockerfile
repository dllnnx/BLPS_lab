FROM maven:3.9.11-amazoncorretto-17-alpine AS build

WORKDIR /app

COPY pom.xml .
COPY ozon-service/pom.xml ozon-service/
COPY geocoder-connector/pom.xml geocoder-connector/
COPY geocoder-connector-shared/pom.xml geocoder-connector-shared/
COPY ozon-ear/pom.xml ozon-ear/
COPY payment-service/pom.xml payment-service/
COPY schema-registry/pom.xml schema-registry/
RUN mvn dependency:go-offline -pl ozon-ear -am -B --no-transfer-progress

COPY ozon-service/src ozon-service/src
COPY schema-registry/src schema-registry/src
COPY geocoder-connector-shared/src geocoder-connector-shared/src
COPY geocoder-connector/src geocoder-connector/src
RUN mvn package -pl ozon-ear -am -DskipTests --no-transfer-progress

FROM quay.io/wildfly/wildfly:latest

USER root

RUN curl -L -o /opt/jboss/wildfly/postgresql.jar \
    https://jdbc.postgresql.org/download/postgresql-42.7.3.jar

USER jboss

COPY --from=build /app/ozon-ear/target/ozon-ear.ear /opt/jboss/wildfly/standalone/deployments/
COPY ozon-ear/configure-datasource.cli /opt/jboss/wildfly/
RUN /opt/jboss/wildfly/bin/jboss-cli.sh --file=/opt/jboss/wildfly/configure-datasource.cli

RUN /opt/jboss/wildfly/bin/add-user.sh -u admin -p admin --silent

CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]