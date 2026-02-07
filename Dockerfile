# First stage: Build the application and extract layers
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace
ARG JAR_FILE=build/libs/*-SNAPSHOT.jar
COPY ${JAR_FILE} order-service.jar

# Spring Boot 3.2+ uses -Djarmode=tools instead of -Djarmode=layertools
RUN java -Djarmode=tools -jar order-service.jar extract --layers --destination extracted

# Second stage: Create the final image with the extracted layers
FROM eclipse-temurin:21-jre

RUN useradd -r nonroot-user
USER nonroot-user

WORKDIR /workspace
COPY --from=builder /workspace/extracted/dependencies/ ./
COPY --from=builder /workspace/extracted/spring-boot-loader/ ./
COPY --from=builder /workspace/extracted/snapshot-dependencies/ ./
COPY --from=builder /workspace/extracted/application/ ./

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
