FROM node:24-alpine AS frontend-build

WORKDIR /frontend

COPY frontend/package*.json ./
RUN npm ci

COPY frontend/ ./

ARG FRONTEND_API_BASE_URL=
ARG FRONTEND_PUBLIC_SHORT_URL_BASE=https://ai-url-shortener-682u.onrender.com
ARG FRONTEND_ENVIRONMENT=production
RUN FRONTEND_API_BASE_URL="${FRONTEND_API_BASE_URL}" \
    FRONTEND_PUBLIC_SHORT_URL_BASE="${FRONTEND_PUBLIC_SHORT_URL_BASE}" \
    FRONTEND_ENVIRONMENT="${FRONTEND_ENVIRONMENT}" \
    npm run build:render

FROM eclipse-temurin:21-jdk-jammy AS backend-build

WORKDIR /app

COPY pom.xml mvnw ./
COPY .mvn .mvn

RUN chmod +x mvnw
RUN ./mvnw -q -DskipTests dependency:go-offline

COPY src src
COPY --from=frontend-build /frontend/dist/frontend/browser/ /app/src/main/resources/static/

RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=backend-build /app/target/url-shortener-0.1.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
