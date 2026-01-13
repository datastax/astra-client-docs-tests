from astrapy import DataAPIClient

# Get an existing collection
client = DataAPIClient()
database = client.get_database(
    "**API_ENDPOINT**",
    token="**APPLICATION_TOKEN**",
)
collection = database.get_collection("**COLLECTION_NAME**")

# Use a projection
cursor = collection.find(
    {"metadata.language": "English"},
    projection={"metadata.edition": True, "title": True},
)
