#!/bin/bash

if [[ $# -lt 1 ]]; then
    echo "Illegal number of parameters" >&2
    exit 2
fi

echo "Stopping nodes $@..."

for node in "$@"
do
    curl --fail -X GET --location "http://$node/stop" && echo "Node $node stopped successfully" || echo "Couldn't stop node $node" >&2
done

echo "Finished stopping nodes. Exiting..."
exit 0
