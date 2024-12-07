#!/bin/bash

echo "Starting the job..."

for i in {1..10}; do
    echo "Processing number $i"
    sleep 1
done

echo "Job completed!"
