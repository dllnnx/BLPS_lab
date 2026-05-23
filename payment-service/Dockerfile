# Build from project root:
#   docker build -f payment-service/Dockerfile -t payment-service .
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY . .
RUN mvn clean package -pl payment-service -am -DskipTests

FROM quay.io/wildfly/wildfly:31.0.0.Final-jdk17
COPY --from=builder /build/payment-service/target/payment-service.war /opt/jboss/wildfly/standalone/deployments/

ENV DATABASE_HOST=postgres
ENV DATABASE_PORT=5432
ENV DATABASE_DB=study
ENV DATABASE_USERNAME=study
ENV DATABASE_PASSWORD=study
ENV CAMUNDA_CLIENT_BASE_URL=http://camunda:8080/engine-rest

EXPOSE 8080 9990

CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]
