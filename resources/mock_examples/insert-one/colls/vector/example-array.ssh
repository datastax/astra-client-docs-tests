curl -sS -L -X POST "**API_ENDPOINT**/api/json/v1/**KEYSPACE_NAME**/**COLLECTION_NAME**" \
  --header "Token: **APPLICATION_TOKEN**" \
  --header "Content-Type: application/json" \
  --data '{
  "insertOne": {
    "document": {
      "name": "Jane Doe",
      "$vector": [0.08, -0.62, 0.39]
    }
  }
}'