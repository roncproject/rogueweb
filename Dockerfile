# ─────────────────────────────────────────────────────────────────
# Dockerfile  –  Rogue AWS Web Edition
# Multi-stage build: compile with Maven, run with a slim JRE image.
#
# Build:   docker build -t rogue-aws .
# Run:     docker run -p 8080:8080 rogue-aws
# AWS:     push to ECR → deploy to App Runner / ECS Fargate
# ─────────────────────────────────────────────────────────────────

# ── Stage 1: Build ───────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace

# Copy dependency descriptors first (layer-cache friendly)
COPY pom.xml .
# Download dependencies before copying source so Docker can cache
# this layer when only source files change.
RUN apt-get update -q && apt-get install -y -q maven && \
    mvn dependency:go-offline -B -q

# Copy source and build the fat JAR
COPY src ./src
RUN mvn package -DskipTests -B -q

# ── Stage 2: Runtime ─────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy

LABEL maintainer="you@example.com"
LABEL description="Rogue roguelike – AWS Web Edition"

# Non-root user for security (AWS best practice)
RUN groupadd -r rogue && useradd -r -g rogue -s /bin/false rogue

WORKDIR /app
COPY --from=build /workspace/target/rogue-aws.jar rogue-aws.jar
RUN chown rogue:rogue rogue-aws.jar

USER rogue

# AWS App Runner / ECS set PORT automatically; default 8080
EXPOSE 8080

# Health check: AWS uses GET /actuator/health
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD curl -sf http://localhost:${PORT:-8080}/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "rogue-aws.jar"]
