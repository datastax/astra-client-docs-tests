import { mutatedCollection } from "../../../_fixtures/mutated-collection";

export async function BeforeEach() {
  await mutatedCollection.truncate();
}
