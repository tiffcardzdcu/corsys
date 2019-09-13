#!/bin/bash
export PORT=4000
export SERVER_NAME=http://localhost:$PORT
./dgate -w$(pwd) -v
