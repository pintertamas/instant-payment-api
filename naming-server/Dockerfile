FROM maven:3.6.3-openjdk-17-slim AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src
RUN mvn clean package

FROM openjdk:17-alpine
WORKDIR /app

COPY --from=build /app/target/naming-server-*.jar naming-server.jar
EXPOSE 8761
ENTRYPOINT ["java","-jar","/naming-server.jar"]
