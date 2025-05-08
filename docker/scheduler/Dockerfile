FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY ./libs/jobrunr-1.0.0-SNAPSHOT.jar /tmp/


RUN mvn install:install-file \
    -DgroupId=org.jobrunr \
    -DartifactId=jobrunr \
    -Dversion=1.0.0-SNAPSHOT \
    -Dpackaging=jar \
    -Dfile=/tmp/jobrunr-1.0.0-SNAPSHOT.jar

# compile, copy deps into target/dependency
RUN mvn clean package -DskipTests \
    dependency:copy-dependencies \
    -Dmaven.compiler.source=17 \
    -Dmaven.compiler.target=17


FROM eclipse-temurin:17-jdk AS runtime
WORKDIR /app
# your app jar
COPY --from=build /app/target/*.jar app.jar
# all dependency jars
COPY --from=build /app/target/dependency ./lib

ENTRYPOINT ["java", "-Duser.timezone=America/Los_Angeles", "-cp", "app.jar:lib/*", "com.example.App"]
