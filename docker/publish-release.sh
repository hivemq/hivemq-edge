#!/usr/bin/env bash

set -eo pipefail

cd "$(dirname $0)/../"
HIVEMQ_EDGE_VERSION=$(./gradlew properties | grep ^version: | sed -e "s/version: //")
echo "Building Docker image for HiveMQ Edge ${HIVEMQ_EDGE_VERSION}"
./gradlew :hivemqEdgeZip
cd docker
cp ../build/distributions/hivemq-edge-${HIVEMQ_EDGE_VERSION}.zip .
IMAGE="hivemq/hivemq-edge:${HIVEMQ_EDGE_VERSION}"
IMAGE_LATEST="hivemq/hivemq-edge:latest"

BUILDER=$(docker buildx create --use)

function shutdown () {
  echo "Killing builder"
  docker buildx rm "$BUILDER"
}

trap shutdown INT TERM EXIT

if [[ ${PUSH_IMAGE} == true ]]; then
  #build multiarch
  docker buildx build --push --platform linux/arm64,linux/amd64,linux/arm/v7 --build-arg HIVEMQ_EDGE_VERSION=${HIVEMQ_EDGE_VERSION} -f DockerFile -t ${IMAGE} -t ${IMAGE_LATEST} .
else
  #Only build for local docker and local arch
  docker build --build-arg HIVEMQ_EDGE_VERSION=${HIVEMQ_EDGE_VERSION} -f DockerFile -t ${IMAGE} -t ${IMAGE_LATEST} .
fi

rm -f hivemq-edge-${HIVEMQ_EDGE_VERSION}.zip
