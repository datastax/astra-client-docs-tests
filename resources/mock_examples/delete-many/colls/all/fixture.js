import { basicCollection } from "../../../_fixtures/basic-collection";

export async function Reset() {
  await basicCollection.insertMany([
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
