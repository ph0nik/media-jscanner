# syntax=docker/dockerfile:1.2

# run java
FROM eclipse-temurin:19.0.1_10-jre-ubi9-minimal@sha256:80319dfb166425f741f74f7726a8c375ed7e1989567fa00395bcd8dc1313d41c

# copy jar with new name into image
COPY media-jscanner-1.0.jar /app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]
# CMD ["java", "-jar", "/app.jar", "-Dspring.profiles.active=prod"]

# to build
# docker build -t media-jscanner:latest .
# to run
# docker run --rm -d -p 8081:8081 -v=/mnt/user/downloads:/download --name ulozto-remote-downloader ulozto-remote-downloader --dpath=/download
# --network="bridge"
