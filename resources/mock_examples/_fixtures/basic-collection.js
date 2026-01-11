import * as $ from '../_base/prelude';

export const collection = $.db.collection(Meta().CollectionName);

export function Meta() {
  return {
    CollectionName: 'basic_collection',
  };
}

export async function Setup() {
  await $.db.createCollection(collection.name);
}

export async function Teardown() {
  await collection.drop();
}
