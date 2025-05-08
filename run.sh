#!/bin/bash

set -e

echo """


     ██╗ ██████╗ ██████╗ ██████╗ ██╗   ██╗███╗   ██╗██████╗     ████████╗███████╗███████╗████████╗██████╗ ███████╗██████╗ 
     ██║██╔═══██╗██╔══██╗██╔══██╗██║   ██║████╗  ██║██╔══██╗    ╚══██╔══╝██╔════╝██╔════╝╚══██╔══╝██╔══██╗██╔════╝██╔══██╗
     ██║██║   ██║██████╔╝██████╔╝██║   ██║██╔██╗ ██║██████╔╝       ██║   █████╗  ███████╗   ██║   ██████╔╝█████╗  ██║  ██║
██   ██║██║   ██║██╔══██╗██╔══██╗██║   ██║██║╚██╗██║██╔══██╗       ██║   ██╔══╝  ╚════██║   ██║   ██╔══██╗██╔══╝  ██║  ██║
╚█████╔╝╚██████╔╝██████╔╝██║  ██║╚██████╔╝██║ ╚████║██║  ██║       ██║   ███████╗███████║   ██║   ██████╔╝███████╗██████╔╝
 ╚════╝  ╚═════╝ ╚═════╝ ╚═╝  ╚═╝ ╚═════╝ ╚═╝  ╚═══╝╚═╝  ╚═╝       ╚═╝   ╚══════╝╚══════╝   ╚═╝   ╚═════╝ ╚══════╝╚═════╝ 
                                                                                                                                                                                                                          
"""

echo "🧹 Clearing old logs..."

> ./logs/scheduled-job-ids.txt
> ./logs/scheduler-logs.txt
> ./logs/worker-logs.txt

echo "✅ Logs cleared!"

# Step 1: Build scheduler and worker images
echo "🔨 Building scheduler image..."
docker-compose -f ./docker/scheduler/docker-compose.yml build --quiet

echo "🔨 Building worker image..."
docker-compose -f ./docker/worker/docker-compose.yml build --quiet

# Step 2: Run the scheduler (blocking)
echo "🚀 Running scheduler container...scheduling jobs"
docker-compose -f ./docker/scheduler/docker-compose.yml up --abort-on-container-exit >> ./logs/scheduler-logs.txt 2>&1

# Step 3: Scheduler has finished — now run 5 workers
echo "⚙️ Scheduler finished. Launching 5 worker containers..."

# Step 3: Scheduler has finished — now run 5 workers in background
echo "⚙️ Scheduler finished. Launching 5 worker containers..."
docker-compose -f ./docker/worker/docker-compose.yml up --scale jobrunr-testbed-worker=5 >> ./logs/worker-logs.txt 2>&1 &
worker_pid=$!

# Step 4: Run the live dashboard
echo "📊 Launching visualisation dashboard..."
python3 visualise.py

# Optional: Clean up workers when done
echo "🧹 Cleaning up workers..."
kill $worker_pid