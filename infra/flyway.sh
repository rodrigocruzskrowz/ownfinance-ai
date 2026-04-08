#!/bin/bash
docker run --rm \
  --network ownfinance-ai_default \
  -v "$(pwd)/backend/src/main/resources/db/migration:/flyway/sql" \
  flyway/flyway:10 \
  -url="jdbc:postgresql://postgres:5432/ownfinance" \
  -user="ownfinance_user" \
  -password="0wnfinanc€" \
  migrate