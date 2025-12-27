from astrapy import DataAPIClient
from astrapy.data_types import DataAPITimestamp
from datetime import datetime

# Get an existing collection
client = DataAPIClient()
database = client.get_database("**API_ENDPOINT**", token="**APPLICATION_TOKEN**")
collection = database.get_collection("**COLLECTION_NAME**")

# Use DataAPITimestamp in insertions
collection.insert_one(
    {"when": DataAPITimestamp.from_string("2024-12-06T12:34:56.789Z")}
)
collection.insert_one({"registered_at": DataAPITimestamp.from_datetime(datetime.now())})

# Use $current date in an update
collection.update_one(
    {"registered_at": {"$exists": True}},
    {"$currentDate": {"last_reviewed": True}},
)

# Use DataAPITimestamp in an equality filter
collection.update_one(
    {"registered_at": DataAPITimestamp.from_string("2024-12-06T12:34:56.789Z")},
    {"$set": {"message": "happy Sunday!"}},
)

# Use DataAPITimestamp in a "less than" filter

collection.find_one(
    {"registered_at": {"$lt": DataAPITimestamp.from_string("2025-12-06T12:34:56.789Z")}}
)

# Print a document with a date
# Will print something like:
# {'registered_at': DataAPITimestamp(timestamp_ms=1733488496789 [2024-12-06T12:34:56.789Z])}
print(
    collection.find_one(
        {
            "registered_at": {
                "$lt": DataAPITimestamp.from_string("2025-12-06T12:34:56.789Z")
            }
        },
        projection={"_id": False},
    )
)