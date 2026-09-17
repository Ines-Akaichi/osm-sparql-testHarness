#!/bin/bash

# Path to the jar
JAR="gucon-sparql-evaluation.jar"

# Paths to dataset folders
ABOX_DIR="data/abox"

# Path to output folder
RESULTS_DIR="data/results"

# diagnostic script's A/B/C labels.
RUN_LABEL="J"

ITERATION_COUNTS=(200)

mkdir -p "$RESULTS_DIR"

# Extended CSV header - matches PipelineMetrics.csvHeaderFragment() output
# from the updated TestHarness.
HEADER="iteration,timestamp,aboxSizeOblig,queryKeyword,elapsedTimeInMillis,memoryUsedInKB,tboxLoadMs,aboxLoadMs,contextBuildMs,inferenceMs,queryMs,gcCount,gcTimeMs"

for TOTAL_ITERATIONS in "${ITERATION_COUNTS[@]}"; do
  OUTPUT_FILE="$RESULTS_DIR/results-experiments-${RUN_LABEL}-${TOTAL_ITERATIONS}.csv"

  if [ ! -f "$OUTPUT_FILE" ]; then
    echo "$HEADER" > "$OUTPUT_FILE"
  fi

  echo
  echo "=========================================="
  echo "=== Running all abox files - TOTAL_ITERATIONS=$TOTAL_ITERATIONS ==="
  echo "=========================================="
  for query in "obligation-state" "regulated-action-state" "temporal-action-state" "event-state" "entity" "action" "resource"; do
    for nbOblig in 9 18 27 36 45 54 63 72; do

     	file_path="$ABOX_DIR/generated-obligations-$nbOblig.ttl"

  	# clear cash 

  	sync
	sudo sh -c 'echo 3 > /proc/sys/vm/drop_caches'
            
      	for i in $(seq 1 "$TOTAL_ITERATIONS"); do

	   /usr/lib/jvm/java-8-temurin-jdk/bin/java -Xms1g -Xmx1g -jar "$JAR" "$file_path" "$query" "$i"  >> "$OUTPUT_FILE"

     	done
    done
  done

  echo
  echo "=== Done with TOTAL_ITERATIONS=$TOTAL_ITERATIONS. Results: $OUTPUT_FILE ==="
done

echo
echo "=========================================="
echo "=== All runs complete. Files written: ==="
for TOTAL_ITERATIONS in "${ITERATION_COUNTS[@]}"; do
  echo "  $RESULTS_DIR/results-experiments-${RUN_LABEL}-${TOTAL_ITERATIONS}.csv"
done
echo "=========================================="