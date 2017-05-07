#!/usr/bin/env bash
docker rm -f $(docker ps -a -q)
docker rmi master
docker rmi slave
sbt -Djava.library.path=./lib_managed assembly
cd ..
docker build -t master -f  master/Dockerfile .
docker build -t slave -f work/Dockerfile .


