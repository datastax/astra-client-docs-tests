import * as $ from '../_base/prelude';

export async function Setup() {
  await $.db.dropType("member", { ifExists: true });
}
