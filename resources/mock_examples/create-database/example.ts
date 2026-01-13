import { DataAPIClient } from "@datastax/astra-db-ts";

const client = new DataAPIClient("**APPLICATION_TOKEN**");

const admin = client.admin();

(async function () {
  await admin.createDatabase({
    name: "**DATABASE_NAME**",
    cloudProvider: "GCP",
    region: "us-east1",
  });
})();
