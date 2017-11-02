set -x
sed -i 's;parameters:.*$;\0\n- description: Queue size parameter for the collector\n  displayName: Jaeger Collector Queue Size\n  name: COLLECTOR_QUEUE_SIZE\n  required: false\n  value: "300000";g' jaeger-production-template.yml
sed -i '0,/.*- \"--cassandra.keyspace=${KEYSPACE}".*$/s//\0\n          - "--collector.queue-size=${COLLECTOR_QUEUE_SIZE}"/' jaeger-production-template.yml
