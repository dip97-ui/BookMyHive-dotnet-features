# build context: backend/
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/bookmyhive-backend-1.0.0.jar app.jar
ENV JAVA_TOOL_OPTIONS="-Xmx256m -XX:MaxMetaspaceSize=128m -Xss512k -XX:+UseSerialGC"
EXPOSE 8080
CMD ["sh","-c","java -Dserver.port=${PORT:-8080} -jar app.jar"]
