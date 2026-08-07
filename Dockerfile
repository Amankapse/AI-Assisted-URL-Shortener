FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

COPY pom.xml mvnw .
COPY .mvn .mvn

RUN chmod +x mvnw
RUN ./mvnw -q -DskipTests dependency:go-offline

COPY src src
COPY scripts scripts

RUN ./mvnw -q -DskipTests package

EXPOSE 8080
ENTRYPOINT ["./mvnw", "spring-boot:run"]
