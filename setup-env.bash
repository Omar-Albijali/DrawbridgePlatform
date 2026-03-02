#!/usr/bin/env bash
set -euo pipefail

echo "Creating local environment files..."

if [[ ! -f "server/.env" ]]; then
  cp "server/.env.example" "server/.env"
  echo "Created server/.env"
else
  echo "Skipped server/.env (already exists)"
fi

if [[ ! -f "webApp/.env" ]]; then
  cp "webApp/.env.example" "webApp/.env"
  echo "Created webApp/.env"
else
  echo "Skipped webApp/.env (already exists)"
fi

echo
echo "Next: open server/.env and set DB_USER, DB_PASSWORD, JWT_SECRET"
