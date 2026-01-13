import { collection } from "../../../_fixtures/basic-collection";

export async function Reset() {
  await collection.insertMany([
    {
      a: 3
    },
    {
      b: ["c"]
    },
    {
      d: { e: 5 }
    },
  ]);
}
