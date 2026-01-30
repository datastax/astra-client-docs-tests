import { DataAPIClient } from "@datastax/astra-db-ts";

// Get an existing collection
const client = new DataAPIClient();
const database = client.db("**API_ENDPOINT**", {
  token: "**APPLICATION_TOKEN**",
});
const collection = database.collection("**COLLECTION_NAME**");

(async function () {
  // Find documents
  const cursor = collection.find({
    $and: [{ is_checked_out: false }, { number_of_pages: { $lt: 300 } }],
  });

  // Iterate over the found documents
  for await (const document of cursor) {
    console.log(document);
  }
})();