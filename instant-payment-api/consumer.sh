#!/usr/bin/env bash

BOOTSTRAP_SERVER="kafka:9092"

TOPIC_NAME=${TOPIC_TRANSACTION_NOTIFICATION}

./opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server "$BOOTSTRAP_SERVER" \
  --topic "$TOPIC_NAME" \
  --from-beginning
