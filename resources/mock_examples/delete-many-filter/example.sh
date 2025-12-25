curl -sS -L -X POST "**API_ENDPOINT**/api/json/v1/**KEYSPACE_NAME**/**COLLECTION_NAME**" \
  --header "Token: **APPLICATION_TOKEN**" \
  --header "Content-Type: application/json" \
  --data '{
  "deleteMany": {
    "filter": {"$and": [
      {"is_checked_out": false},
      {"number_of_pages": {"$lt": 300}}
    ]}
  }
}'