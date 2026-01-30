import * as $ from '../_base/prelude';

export const basicCollection = $.db.collection(Meta().CollectionName);

basicCollection.truncate = async function () {
  await $.truncate(this);
}

export function Meta() {
  return {
    CollectionName: 'basic_collection',
  };
}

export async function Setup() {
  // await $.db.createCollection(basicCollection.name);
}

export async function Teardown() {
  // await basicCollection.drop();
}
