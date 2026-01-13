import * as $ from '../_base/prelude';

export const table = $.db.table(Meta().TableName);

export function Meta() {
  return {
    TableName: 'basic_collection',
  };
}

export async function Setup() {
  await $.db.createTable(table.name, {
    definition: {
      columns: {
        id: 'text',
        name: 'text',
        age: 'int',
      },
      primaryKey: 'id',
    },
  });
}

export async function Teardown() {
  await table.drop();
}
