# syntax=docker/dockerfile:1@sha256:4edf897a3ffa55b89f906fc8cc78afdb3f1834cc9c7083565e611a8a7d5fe99e
# Imagen de la aplicación completa (API + frontend en un único JAR), en tres etapas:
#   docker build -t alimentarse .
#   docker compose --profile full up -d        (con MySQL y Mailpit; ver docker-compose.yml)
# Las imágenes van fijadas por digest (D-090): una etiqueta se puede mover. Dependabot propone cada semana el digest
# nuevo de la misma etiqueta, con los parches.

# ---- 1. Frontend: build de producción de Angular ----
FROM node:24-alpine@sha256:116b800c3754b440ee0aa3ed2f20f5bf3d99a554bfd9adeea09ea8017f6e85f5 AS frontend
WORKDIR /src/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
# angular.json deja el build en ../backend/src/main/resources/static
RUN npm run build

# ---- 2. Backend: JAR de Spring Boot con el build de Angular dentro ----
FROM eclipse-temurin:25-jdk@sha256:b8705e936964c228dfce359e63dbf08f6c69cb380ed1d576b1f6b1064f15567d AS backend
WORKDIR /src/backend
COPY backend/.mvn/ .mvn/
COPY backend/mvnw backend/pom.xml ./
# mvnw comprueba el SHA-256 del .zip de Maven (D-090), pero sin unzip descarga el .tar.gz, que tiene otro. La imagen
# base no lo trae; esta etapa no llega a la imagen final.
RUN apt-get update && apt-get install -y --no-install-recommends unzip && rm -rf /var/lib/apt/lists/*
# Descarga las dependencias en una capa propia, para no repetirlo si solo cambia el código. Si algún plugin no se
# resuelve por adelantado, ya lo hará el «package» de después.
RUN chmod +x mvnw && (./mvnw -B -q dependency:go-offline || true)
COPY backend/src/ src/
COPY --from=frontend /src/backend/src/main/resources/static/ src/main/resources/static/
# Los tests necesitan MySQL: se pasan aparte (mvnw verify)
RUN ./mvnw -B -q -DskipTests package

# ---- 3. Imagen final: solo el JRE y el JAR ----
FROM eclipse-temurin:25-jre@sha256:a640968ae55027b322cedd5f8228b01a84baff0b9e9360e47601c6a111ba1699
RUN groupadd --system toca && useradd --system --gid toca --home-dir /app --no-create-home toca \
    && mkdir -p /app/uploads && chown toca:toca /app/uploads
WORKDIR /app
COPY --from=backend /src/backend/target/alimentarse.jar app.jar
USER toca
ENV SPRING_PROFILES_ACTIVE=prod \
    UPLOAD_DIR=/app/uploads \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
# Salud con bash (/dev/tcp): la imagen base no trae curl ni wget
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
    CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080 && printf "GET /api/health/readiness HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n" >&3 && grep -q "\"UP\"" <&3'
# Las imágenes de las recetas
VOLUME /app/uploads
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
