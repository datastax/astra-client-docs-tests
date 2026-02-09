import { DataAPIClient } from "@datastax/astra-db-ts";

// Get a database
const client = new DataAPIClient();
const database = client.db("**API_ENDPOINT**", {
  token: "**APPLICATION_TOKEN**",
});

// Drop a user-defined type
(async function () {
  await database.createType("member", {
    definition: {
      fields: {
        name: "text",
        is_active: "boolean",
        date_joined: "date",
      },
    },
  });
})();