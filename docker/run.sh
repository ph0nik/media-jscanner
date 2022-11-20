#! /usr/bin/bash
docker run --rm -d -p 44222:44222 -v=/mnt/user/movies:/ --name media-jscanner media-jscanner