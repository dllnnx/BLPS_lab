FROM maven:3.9.11-amazoncorretto-17-alpine AS build

WORKDIR /app

COPY pom.xml .
COPY ozon-service/pom.xml ozon-service/
COPY payment-service/pom.xml payment-service/
COPY schema-registry/pom.xml schema-registry/
RUN mvn dependency:go-offline -pl ozon-service -am -B --no-transfer-progress

COPY ozon-service/src ozon-service/src
COPY schema-registry/src schema-registry/src
RUN mvn package -pl ozon-service -am -DskipTests --no-transfer-progress

FROM amazoncorretto:17-alpine
WORKDIR /app

COPY --from=build /app/ozon-service/target/ozon.jar ./ozon.jar

ENTRYPOINT java -jar ./ozon.jar