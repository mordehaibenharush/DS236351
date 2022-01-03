FROM openjdk:11
ARG JAR_FILE=./server/build/libs/server.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8080 8090
ENTRYPOINT ["java","-jar","/app.jar"]