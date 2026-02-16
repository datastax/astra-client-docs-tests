import * as $ from "../_base/prelude";

export const basicTable = $.withUtils(
  $.db.table(Meta().TableName)
);

export function Meta() {
  return {
    TableName: $.name("basic_table"),
    Initialization: "parallel",
  };
}

export async function Setup() {
  await $.db.createTable(basicTable.name, {
    definition: {
      columns: {
        id: "text",
        name: "text",
        age: "int",
      },
      primaryKey: "id",
    },
    ifNotExists: true,
  });
}

export async function Teardown() {
  await basicTable.drop();
}
