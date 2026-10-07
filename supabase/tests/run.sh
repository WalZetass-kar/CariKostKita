#!/usr/bin/env bash
# Menjalankan migrasi + skenario serangan RLS di PostgreSQL biasa (tiruan Supabase),
# lalu membandingkan hasilnya dengan expected_output.txt.
# Pemakaian: PGHOST=127.0.0.1 PGPORT=5432 PGUSER=postgres ./supabase/tests/run.sh [--update]
set -euo pipefail
cd "$(dirname "$0")"
DB="${PGDATABASE_TEST:-ckk_test}"
psql -q -c "DROP DATABASE IF EXISTS $DB" postgres
psql -q -c "CREATE DATABASE $DB" postgres
psql -q -d "$DB" -f 00_mock_supabase.sql >/dev/null 2>&1
psql -q -d "$DB" -v ON_ERROR_STOP=1 -f ../migrations/20261005_security_hardening.sql 2>&1 | grep -v NOTICE || true
psql -q -d "$DB" -v ON_ERROR_STOP=1 -f ../migrations/20261006_industry_features.sql 2>&1 | grep -v NOTICE || true
ACTUAL="$(mktemp)"
{
  psql -q -d "$DB" -f 10_security_hardening_tests.sql 2>&1
  psql -q -d "$DB" -f 20_industry_features_tests.sql 2>&1
} | grep -v '^ *$' | grep -v 'as_user' | sed -E 's/^psql:[^:]+:[0-9]+: //' | grep -v '^CONTEXT\|^SQL statement\|^PL/pgSQL' > "$ACTUAL"
if [[ "${1:-}" == "--update" ]]; then
  cp "$ACTUAL" expected_output.txt
  echo "expected_output.txt diperbarui"
  exit 0
fi
if diff -u expected_output.txt "$ACTUAL"; then
  echo "Semua skenario keamanan sesuai harapan"
else
  echo "Hasil berbeda dari expected_output.txt (lihat diff di atas)" >&2
  exit 1
fi
