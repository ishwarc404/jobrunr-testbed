echo "🧨 Starting background chaos monkey (kills and revives 4/5 workers every 3 minutes)..."

  while true; do
    sleep 100  # wait 100 seconds

    # Get all running worker containers
    worker_containers=$(docker ps --filter "name=jobrunr-testbed-worker" --format "{{.ID}}")

    # Proceed only if we have at least 5
    num_workers=$(echo "$worker_containers" | wc -l)
    if [ "$num_workers" -ge 5 ]; then
      containers_to_kill=$(echo "$worker_containers" | awk 'BEGIN{srand()} {print rand(), $0}' | sort -k1,1n | cut -d' ' -f2- | head -n 4)

      echo "💀 [$(date)] Killing 4/5 jobrunr-testbed-worker containers..."
      for cid in $containers_to_kill; do
        echo "      💀 [$(date)] Killing container $cid..."
        docker kill "$cid"
      done

      echo "🛠️ [$(date)] Waiting 10 seconds before reviving workers..."
      sleep 5

      echo "🚀 [$(date)] Reviving workers back to 5 replicas..."
      docker-compose -f ./docker/worker/docker-compose.yml up --scale jobrunr-testbed-worker=5 >> ./logs/worker-logs.txt 2>&1 &

    else
      echo "⚠️ [$(date)] Less than 5 workers detected. Skipping chaos round."
    fi
  done

