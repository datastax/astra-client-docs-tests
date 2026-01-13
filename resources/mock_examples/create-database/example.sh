curl -sS -L -X POST "https://api.astra.datastax.com/v2/databases" \
--header "Authorization: Bearer **APPLICATION_TOKEN**" \
--header "Content-Type: application/json" \
--data '{
  "name": "**DATABASE_NAME**",
  "keyspace": "",
  "cloudProvider": "gcp",
  "region": "us-east1",
  "dbType": "vector",
  "tier": "serverless",
  "capacityUnits": 1
}'
