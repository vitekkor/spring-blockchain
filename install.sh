#!/bin/bash

echo "Building vitcoin image..."

docker build -t vitekkor/vitcoin:1.0.0 .

echo "Successfully built vitekkor/vitcoin:1.0.0 image"

echo "Building startScript image..."

docker build -f DockerfileStartScript -t vitekkor/vitcoin-start-script:1.0.0 .

echo "Successfully built vitekkor/vitcoin-start-script image"
exit 0
