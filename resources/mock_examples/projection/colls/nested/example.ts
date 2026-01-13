import { DataAPIClient } from "@datastax/astra-db-ts";

// Get an existing collection
const client = new DataAPIClient();
const database = client.db("**API_ENDPOINT**", {
  token: "**APPLICATION_TOKEN**",
});
const collection = database.collection("**COLLECTION_NAME**");

// Use a projection
const cursor = collection.find(
  { "metadata.language": "English" },
  { projection: { "metadata.edition": true, title: true } },
);
