import { collection } from "../../_fixtures/basic-collection";

export async function Reset() {
  await collection.deleteMany({ i_dont_exist_for_sure: { $exists: false } });

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
