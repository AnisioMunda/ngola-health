#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

if [[ $# -ne 2 ]]; then
  printf 'Usage: restore.sh <backup filename> <empty target database>\n' >&2
  exit 2
fi

: "${DB_HOST:?DB_HOST is required}"
: "${DB_PORT:?DB_PORT is required}"
: "${DB_USER:?DB_USER is required}"
: "${DB_PASSWORD:?DB_PASSWORD is required}"
: "${DB_NAME:?DB_NAME is required}"
: "${BACKUP_STORAGE_DIR:?BACKUP_STORAGE_DIR is required}"
: "${BACKUP_AGE_IDENTITY_PATH:?BACKUP_AGE_IDENTITY_PATH is required}"

backup_name="$1"
target_database="$2"

if [[ "$target_database" == "$DB_NAME" ]]; then
  printf '%s\n' 'Refusing to restore over the configured source database.' >&2
  exit 1
fi

backup_timestamp="${backup_name#"$DB_NAME"-}"
if [[ "$backup_name" != "$DB_NAME-"* ]] ||
  [[ ! "$backup_timestamp" =~ ^[0-9]{8}T[0-9]{6}Z\.dump\.age$ ]]; then
  printf '%s\n' 'Backup filename must match the configured database and timestamp format.' >&2
  exit 1
fi

encrypted_path="$BACKUP_STORAGE_DIR/$backup_name"
if [[ ! -f "$encrypted_path" || ! -r "$encrypted_path" ]]; then
  printf 'Backup file is missing or unreadable: %s\n' "$encrypted_path" >&2
  exit 1
fi

if [[ ! -r "$BACKUP_AGE_IDENTITY_PATH" ]]; then
  printf '%s\n' 'The age identity file is missing or unreadable.' >&2
  exit 1
fi

work_dir="$(mktemp -d)"
trap 'rm -rf "$work_dir"' EXIT
dump_path="$work_dir/backup.dump"

age --decrypt \
  --identity "$BACKUP_AGE_IDENTITY_PATH" \
  --output "$dump_path" \
  "$encrypted_path"

export PGPASSWORD="$DB_PASSWORD"
existing_table_count="$(
  psql \
    --host="$DB_HOST" \
    --port="$DB_PORT" \
    --username="$DB_USER" \
    --dbname="$target_database" \
    --tuples-only \
    --no-align \
    --command="SELECT count(*) FROM information_schema.tables WHERE table_schema NOT IN ('information_schema', 'pg_catalog') AND table_type = 'BASE TABLE';"
)"
if [[ "$existing_table_count" != "0" ]]; then
  printf 'Refusing to restore into non-empty target database %s.\n' "$target_database" >&2
  exit 1
fi

pg_restore \
  --host="$DB_HOST" \
  --port="$DB_PORT" \
  --username="$DB_USER" \
  --dbname="$target_database" \
  --exit-on-error \
  --no-owner \
  --no-privileges \
  "$dump_path"

restored_table_count="$(
  psql \
    --host="$DB_HOST" \
    --port="$DB_PORT" \
    --username="$DB_USER" \
    --dbname="$target_database" \
    --tuples-only \
    --no-align \
    --command="SELECT count(*) FROM information_schema.tables WHERE table_schema NOT IN ('information_schema', 'pg_catalog') AND table_type = 'BASE TABLE';"
)"
if [[ ! "$restored_table_count" =~ ^[1-9][0-9]*$ ]]; then
  printf 'Restore completed without user tables in target database %s.\n' "$target_database" >&2
  exit 1
fi

printf 'Restored backup to empty database %s; found %s user tables.\n' \
  "$target_database" "$restored_table_count"
