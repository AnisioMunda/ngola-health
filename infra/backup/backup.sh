#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

: "${DB_HOST:?DB_HOST is required}"
: "${DB_PORT:?DB_PORT is required}"
: "${DB_NAME:?DB_NAME is required}"
: "${DB_USER:?DB_USER is required}"
: "${DB_PASSWORD:?DB_PASSWORD is required}"
: "${BACKUP_STORAGE_DIR:?BACKUP_STORAGE_DIR is required}"
: "${BACKUP_AGE_RECIPIENT:?BACKUP_AGE_RECIPIENT is required}"
: "${BACKUP_RETENTION_DAYS:=30}"

if [[ ! "$BACKUP_RETENTION_DAYS" =~ ^[1-9][0-9]*$ ]]; then
  printf '%s\n' 'BACKUP_RETENTION_DAYS must be a positive integer.' >&2
  exit 1
fi

mkdir -p "$BACKUP_STORAGE_DIR"

work_dir="$(mktemp -d)"
trap 'rm -rf "$work_dir"' EXIT

timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
archive_name="${DB_NAME}-${timestamp}.dump.age"
archive_path="$work_dir/$archive_name"
stored_archive_path="$BACKUP_STORAGE_DIR/$archive_name"
dump_path="$work_dir/${DB_NAME}-${timestamp}.dump"

export PGPASSWORD="$DB_PASSWORD"
pg_dump \
  --host="$DB_HOST" \
  --port="$DB_PORT" \
  --username="$DB_USER" \
  --dbname="$DB_NAME" \
  --format=custom \
  --file="$dump_path"

age --recipient "$BACKUP_AGE_RECIPIENT" --output "$archive_path" "$dump_path"
mv "$archive_path" "$stored_archive_path"

find "$BACKUP_STORAGE_DIR" \
  -maxdepth 1 \
  -type f \
  -name "${DB_NAME}-*.dump.age" \
  -mtime "+$BACKUP_RETENTION_DAYS" \
  -print \
  -delete

printf 'Created encrypted PostgreSQL backup %s\n' "$stored_archive_path"
