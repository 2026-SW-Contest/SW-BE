#!/usr/bin/env bash

set -Eeuo pipefail

if [[ $# -ne 2 ]]; then
  echo "Usage: $0 <docker-image> <next-env-file>" >&2
  exit 64
fi

readonly NEW_IMAGE="$1"
readonly NEXT_ENV_FILE="$2"
readonly ACTIVE_ENV_FILE=".env"
readonly PREVIOUS_ENV_FILE=".env.previous"
readonly COMPOSE_FILE="docker-compose.yml"
readonly IMAGE_RETENTION_PERIOD="168h"

if [[ ! -f "$NEXT_ENV_FILE" ]]; then
  echo "$NEXT_ENV_FILE does not exist." >&2
  exit 66
fi

if [[ ! -f "$COMPOSE_FILE" ]]; then
  echo "$COMPOSE_FILE does not exist." >&2
  exit 66
fi

previous_container_id="$(
  DOCKER_IMAGE="$NEW_IMAGE" \
    docker compose --env-file "$NEXT_ENV_FILE" \
      -f "$COMPOSE_FILE" ps -q app || true
)"

previous_image=""
if [[ -n "$previous_container_id" ]]; then
  previous_image="$(
    docker inspect --format '{{.Config.Image}}' \
      "$previous_container_id" || true
  )"
fi

echo "Pruning unused images older than $IMAGE_RETENTION_PERIOD"
docker image prune \
  --all \
  --force \
  --filter "until=$IMAGE_RETENTION_PERIOD"

echo "Pulling $NEW_IMAGE"
DOCKER_IMAGE="$NEW_IMAGE" \
  docker compose --env-file "$NEXT_ENV_FILE" \
    -f "$COMPOSE_FILE" pull app

had_previous_env=false
if [[ -f "$ACTIVE_ENV_FILE" ]]; then
  cp -p "$ACTIVE_ENV_FILE" "$PREVIOUS_ENV_FILE"
  had_previous_env=true
fi

chmod 600 "$NEXT_ENV_FILE"
mv "$NEXT_ENV_FILE" "$ACTIVE_ENV_FILE"

echo "Deploying $NEW_IMAGE"
if DOCKER_IMAGE="$NEW_IMAGE" \
    docker compose --env-file "$ACTIVE_ENV_FILE" \
      -f "$COMPOSE_FILE" \
      up -d --wait --wait-timeout 120 --remove-orphans; then
  rm -f "$PREVIOUS_ENV_FILE"
  echo "Deployment succeeded: $NEW_IMAGE"
  exit 0
fi

echo "Deployment health check failed." >&2
DOCKER_IMAGE="$NEW_IMAGE" \
  docker compose --env-file "$ACTIVE_ENV_FILE" \
    -f "$COMPOSE_FILE" \
    logs --no-color --tail=100 app || true

if [[ "$had_previous_env" == true ]]; then
  mv "$PREVIOUS_ENV_FILE" "$ACTIVE_ENV_FILE"
else
  rm -f "$ACTIVE_ENV_FILE"
fi

if [[ -z "$previous_image" ]]; then
  echo "No previous image is available for rollback." >&2
  exit 1
fi

echo "Rolling back to $previous_image" >&2
DOCKER_IMAGE="$previous_image" \
  docker compose --env-file "$ACTIVE_ENV_FILE" \
    -f "$COMPOSE_FILE" \
    up -d --wait --wait-timeout 120 --remove-orphans

echo "Rollback completed: $previous_image" >&2
exit 1
