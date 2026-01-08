import * as $ from '../_base/prelude';

export const collection = $.db.collection($.CollectionName);

export async function Setup() {
  await $.db.createCollection(collection.name);
}

export async function Teardown() {
  await collection.drop();
}
