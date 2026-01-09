curl -sS -L -X POST "**API_ENDPOINT**/api/json/v1/**KEYSPACE_NAME**/**COLLECTION_NAME**" \
  --header "Token: **APPLICATION_TOKEN**" \
  --header "Content-Type: application/json" \
  --data '{
  "deleteMany": {
    "filter": {}
  }
}'
