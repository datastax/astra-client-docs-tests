import * as $ from '../_base/prelude';

export const basicTable = $.db.table(Meta().TableName);

basicTable.truncate = async function () {
  await $.truncate(this, 'id');
}

export function Meta() {
  return {
    TableName: 'basic_table',
  };
}

export async function Setup() {
  await $.db.createTable(basicTable.name, {
    definition: {
      columns: {
        id: 'text',
        name: 'text',
        age: 'int',
      },
      primaryKey: 'id',
    },
    ifNotExists: true,
  });
}

export async function Teardown() {
  await basicTable.drop();
}
