import { DataAPIClient } from "@datastax/astra-db-ts";

// Get an existing collection
const client = new DataAPIClient();
const database = client.db("**API_ENDPOINT**", {
  token: "**APPLICATION_TOKEN**",
});
const collection = database.collection("**COLLECTION_NAME**");

(async function () {
  // Use dates in insertions
  await collection.insertOne({ dateOfBirth: new Date(1394104654000) });
  await collection.insertOne({ dateOfBirth: new Date("1863-05-28") });

  // Use $currentDate in an update
  await collection.updateOne(
    {
      dateOfBirth: new Date("1863-05-28"),
    },
    {
      $set: { message: "Happy Birthday!" },
      $currentDate: { lastModified: true },
    },
  );

  // Use a date in a filter
  const found = await collection.findOne({
    dateOfBirth: { $lt: new Date("1900-01-01") },
  });
})();