import { collection } from "../../_fixtures/basic-collection";

export async function Reset() {
  await collection.deleteMany({ i_dont_exist_for_sure: { $exists: false } });

  await collection.insertMany([
    {
      is_checked_out: false,
      number_of_pages: 150, // will be deleted
    },
    {
      is_checked_out: false,
      number_of_pages: 0,   // will be deleted
    },
    {
      is_checked_out: true,
      number_of_pages: 150, // will stay
    },
    {
      is_checked_out: false,
      number_of_pages: 300, // will stay
    },
  ]);
}
