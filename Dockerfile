FROM bellsoft/liberica-runtime-container:jdk as builder

COPY . /app
RUN cd /app && ./gradlew :web:build -xtest

FROM bellsoft/liberica-runtime-container:jre as optimizer

# Copy the JAR file (using a wildcard to handle version changes)
COPY --from=builder /app/web/build/libs/web-0.0.1.jar /app/app.jar
WORKDIR /app
RUN java -Djarmode=tools -jar /app/app.jar extract --layers --launcher

FROM bellsoft/liberica-runtime-container:jre as runner

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
COPY --from=optimizer /app/app/dependencies/ ./
COPY --from=optimizer /app/app/spring-boot-loader/ ./
COPY --from=optimizer /app/app/snapshot-dependencies/ ./
COPY --from=optimizer /app/app/application/ ./
