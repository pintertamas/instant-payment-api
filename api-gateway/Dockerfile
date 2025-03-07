FROM maven:3.8.3-amazoncorretto-17 as build
WORKDIR /app

COPY pom.xml .
COPY src ./src
RUN mvn clean package

FROM amazoncorretto:17
WORKDIR /app

COPY --from=build /app/target/api-gateway-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
