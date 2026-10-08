#!/usr/bin/env bash
set -Eeuo pipefail

while :; do
  now_epoch="$(date -u +%s)"
  today_run_epoch="$(date -u -d "$(date -u +%F) 02:00:00" +%s)"

  if (( now_epoch >= today_run_epoch )); then
    next_run_epoch="$(date -u -d 'tomorrow 02:00:00' +%s)"
  else
    next_run_epoch="$today_run_epoch"
  fi

  wait_seconds=$((next_run_epoch - now_epoch))
  printf 'Next PostgreSQL backup scheduled for %s\n' \
    "$(date -u -d "@$next_run_epoch" +%Y-%m-%dT%H:%M:%SZ)"
  sleep "$wait_seconds"
  /usr/local/bin/backup.sh
done
