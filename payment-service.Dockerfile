FROM maven:3.9.11-amazoncorretto-17-alpine AS build

WORKDIR /app

COPY pom.xml .
COPY ozon-service/pom.xml ozon-service/
COPY payment-service/pom.xml payment-service/
COPY schema-registry/pom.xml schema-registry/
RUN mvn dependency:go-offline -pl payment-service -am -B --no-transfer-progress

COPY payment-service/src payment-service/src
COPY schema-registry/src schema-registry/src
RUN mvn package -pl payment-service -am -DskipTests --no-transfer-progress

FROM amazoncorretto:17-alpine
WORKDIR /app

COPY --from=build /app/payment-service/target/payment-service.jar ./payment-service.jar

ENTRYPOINT java -jar ./payment-service.jar