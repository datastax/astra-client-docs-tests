import * as $ from '../_base/prelude';

export const mutatedCollection = $.withUtils(
  $.db.collection(Meta().CollectionName)
);

export function Meta() {
  return {
    CollectionName: $.name("mutated_collection"),
    Initialization: "parallel",
  };
}

export async function Setup() {
  await $.db.createCollection(
    mutatedCollection.name,
    {
      vector: {
        service: {
          provider: "nvidia",
          modelName: "nvidia/nv-embedqa-e5-v5",
        },
      },
    },
  );
}

export async function Teardown() {
  await mutatedCollection.drop();
}
