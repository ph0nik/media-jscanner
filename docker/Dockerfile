# syntax=docker/dockerfile:1.2

# run java
FROM eclipse-temurin:11.0.17_8-jre-ubi9-minimal@sha256:2fb0e563f8be7f3729d3db659a0882ad84c0b8f9d3f6570feea7cd9590339f97

# copy jar with new name into image
COPY media-jscanner-1.0.jar /app.jar

# ENTRYPOINT ["java", "-jar", "/app.jar"]
CMD ["java", "-Dspring.profiles.active=prod", "-jar", "/app.jar"]

# to build
# docker build -t media-jscanner:latest .
# to run
# docker run --rm -d -p 44222:44222 -v=/mnt/user/appdata/jscanner:/data -v=/mnt/user/movies:/share --log-opt max-size=50m --name media-jscanner media-jscanner
# docker run --rm -p 44222:44222 -v=/mnt/user/appdata/jscanner:/data --log-opt max-size=50m --name media-jscanner media-jscanner
# --network="bridge"
