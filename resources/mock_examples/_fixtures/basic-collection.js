import * as $ from "../_base/prelude";

export const basicCollection = $.withUtils(
  $.db.collection(Meta().CollectionName)
);

export function Meta() {
  return {
    CollectionName: $.name("basic_collection"),
    Initialization: "parallel",
  };
}

export async function Setup() {
  await $.db.createCollection(basicCollection.name);
}

export async function Teardown() {
  await basicCollection.drop();
}
