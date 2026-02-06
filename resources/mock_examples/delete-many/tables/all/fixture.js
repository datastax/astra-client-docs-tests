import { basicTable } from "../../../_fixtures/basic-table";

export async function BeforeEach() {
  await basicTable.insertMany([
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
