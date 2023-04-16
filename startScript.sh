#!/bin/sh

sleep 10

if [[ $# -lt 1 ]]; then
    echo "Illegal number of parameters" >&2
    exit 2
fi

echo "Generate genesis..."

curl --fail -X GET --location "http://$1/generateGenesys" || exit 2

echo "Successful"

echo "Start blockchain..."

for node in "$@"
do
    curl --fail -X GET --location "http://$node/start" || exit 2
done

echo "Finished start script. Exiting..."
exit 0
