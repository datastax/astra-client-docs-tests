import * as $ from '../_base/prelude';

export const mutatedCollection = $.db.collection(Meta().CollectionName);

mutatedCollection.truncate = async function () {
  await $.truncate(this);
}

export function Meta() {
  return {
    CollectionName: 'mutated_collection',
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
  // await mutatedCollection.drop();
}
