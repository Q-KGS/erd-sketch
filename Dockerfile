# Stage 1: Frontend build
FROM node:20-alpine AS frontend-build
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# Stage 2: Backend build
FROM eclipse-temurin:21-jdk-alpine AS backend-build
WORKDIR /app
COPY backend/ ./
COPY --from=frontend-build /app/dist src/main/resources/static/
RUN chmod +x mvnw && ./mvnw package -DskipTests -q

# Stage 3: Runtime
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
COPY --from=backend-build /app/target/*.jar app.jar
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=fly
ENTRYPOINT ["java", "-Xmx384m", "-jar", "app.jar"]
