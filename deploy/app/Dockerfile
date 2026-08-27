# Sunrise Dental Clinic — application image.
#
# Multi-stage build: Maven produces the WAR, Tomcat runs it as ROOT so the
# application answers at "/" with no context path. Build context is the repo
# root (Render builds from there), so paths are relative to the root.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY modular/pom.xml .
RUN mvn -q dependency:go-offline
COPY modular/src ./src
RUN mvn -o -q -DskipTests package

FROM tomcat:10.1-jdk17-temurin
COPY --from=build /app/target/clinic.war /usr/local/tomcat/webapps/ROOT.war
# Disable Tomcat's shutdown socket (default port 8005): an unused control
# channel that leaves a port listening and logs "Invalid shutdown command"
# for every stray probe. port="-1" turns the listener off entirely; the
# shutdown scripts are not used inside a container.
RUN sed -i 's/<Server port="8005" shutdown="SHUTDOWN">/<Server port="-1" shutdown="SHUTDOWN">/' \
    /usr/local/tomcat/conf/server.xml
EXPOSE 8080