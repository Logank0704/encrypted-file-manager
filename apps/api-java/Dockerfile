# ===== Build stage =====
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn -q -e dependency:go-offline -B
COPY src ./src
RUN mvn -q -e clean package -DskipTests

# ===== Runtime stage =====
FROM eclipse-temurin:17-jre-jammy
ENV TZ=UTC LANG=C.UTF-8
WORKDIR /app
COPY --from=builder /app/target/*.jar /app/app.jar
RUN mkdir -p /app/data
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
