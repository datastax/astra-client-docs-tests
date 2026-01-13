import { collection } from "../../../_fixtures/basic-collection";

export async function Reset() {
  await collection.deleteMany({ i_dont_exist_for_sure: { $exists: false } });

  await collection.insertMany([
    {
      title: "Moby Richard",
      author: "Some old guy",
      metadata: {
        language: "English",
        edition: "1852",
      },
    },
    {
      title: "Donald Quixote",
      author: "Probably some Spanish guy",
      metadata: {
        language: "Definitely not english",
        edition: "1605",
      }
    },
    {
      title: "Rankine 910.67",
      author: "Raymond Brad Berry",
      metadata: {
        language: "English",
        edition: "1955",
      }
    }
  ]);
}
