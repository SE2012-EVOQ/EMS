FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
COPY src src
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --gid 10001 ems && useradd --uid 10001 --gid ems --no-create-home ems
COPY --from=build /workspace/target/ems-backend-0.0.1-SNAPSHOT.jar /app/ems.jar
USER ems
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/ems.jar"]
