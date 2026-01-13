import { table } from "../../../_fixtures/basic-table";

export async function Reset() {
  await table.insertMany([
    {
      id: 'a',
    },
    {
      id: 'b',
      age: 3,
    },
    {
      id: 'c',
      name: '7',
    },
  ]);
}
