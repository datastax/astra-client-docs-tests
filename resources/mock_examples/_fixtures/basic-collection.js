import * as $ from './_prelude';

export const collection = $.db.collection($.CollectionName);

export async function Setup() {
  await $.db.createCollection(collection.name);
}

export async function Reset() {
  await collection.deleteMany({});
}

export async function Teardown() {
  await collection.drop();
}
