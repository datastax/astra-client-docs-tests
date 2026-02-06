import { basicCollection } from "../../../_fixtures/basic-collection";

export async function BeforeEach() {
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
