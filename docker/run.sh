#! /usr/bin/bash
docker run --rm -d -p 8081:8081 -v=/mnt/user/downloads:/download --name ulozto-remote-downloader ulozto-remote-downloader --dpath=/download