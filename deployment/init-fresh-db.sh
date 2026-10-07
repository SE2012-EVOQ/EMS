#!/bin/sh
# Fresh installations only. This script never drops a database.
set -eu
: "${MYSQL_CNF:?Point MYSQL_CNF to an external MySQL client credentials file}"
EMS_DATABASE_NAME=${EMS_DATABASE_NAME:-evoq_ems}
case "$EMS_DATABASE_NAME" in ''|*[!A-Za-z0-9_]*) echo 'Invalid database name' >&2; exit 1;; esac
EMS_REPO_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
# CREATE without IF NOT EXISTS deliberately refuses an existing database.
mysql --defaults-extra-file="$MYSQL_CNF" --execute="CREATE DATABASE \`$EMS_DATABASE_NAME\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci"
# Read finalized table DDL only; omit its destructive DROP/CREATE/USE preamble.
sed -n '/^CREATE TABLE role/,$p' "$EMS_REPO_ROOT/database/01_schema.sql" | mysql --defaults-extra-file="$MYSQL_CNF" "$EMS_DATABASE_NAME"
mysql --defaults-extra-file="$MYSQL_CNF" "$EMS_DATABASE_NAME" < "$EMS_REPO_ROOT/database/06_runtime_roles.sql"
echo 'Fresh schema and runtime roles installed. No accounts or demonstration data created.'
