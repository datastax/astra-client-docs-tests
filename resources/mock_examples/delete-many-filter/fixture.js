import { collection } from "../_fixtures/basic-collection";

export async function Setup() {
  await collection.insertMany([
    { random: { fields: 'idk' } },
    { I: ['am', 'not', { very: 'creative' }] },
    { when: 'it', comes: 2, naming: 'things' },
  ]);
}
