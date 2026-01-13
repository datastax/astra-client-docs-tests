from astrapy import DataAPIClient

# Get an existing collection
client = DataAPIClient()
database = client.get_database("**API_ENDPOINT**", token="**APPLICATION_TOKEN**")
collection = database.get_collection("**COLLECTION_NAME**")

# Estimate count
result = collection.estimated_document_count()
print(result)
