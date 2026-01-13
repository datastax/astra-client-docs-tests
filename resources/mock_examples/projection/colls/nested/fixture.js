import { basicCollection } from "../../../_fixtures/basic-collection";

export async function Setup() {
  await basicCollection.truncate();

  await basicCollection.insertMany([
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
