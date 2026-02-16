import * as $ from "../_base/prelude";
import {
  Table,
} from "@datastax/astra-db-ts";

export const booksTable = $.withUtils(
  $.db.table(Meta().TableName)
);

export function Meta() {
  return {
    TableName: $.name("books_table"),
    Initialization: "parallel",
  };
}

export async function Setup() {
  const tableDefinition = Table.schema({
    columns: {
      title: "text",
      author: "text",
      number_of_pages: "int",
      rating: "float",
      publication_year: "int",
      summary: "text",
      genres: { type: "set", valueType: "text" },
      metadata: {
        type: "map",
        keyType: "text",
        valueType: "text",
      },
      is_checked_out: "boolean",
      borrower: "text",
      due_date: "date",
      summary_genres_vector: {
        type: "vector",
        dimension: 1024,
        service: {
          provider: "nvidia",
          modelName: "nvidia/nv-embedqa-e5-v5",
        },
      },
      summary_genres_original_text: "text",
    },
    primaryKey: {
      partitionBy: ["title", "author"],
    },
  });

  await booksTable.drop({ ifExists: true }); // in case it was modified and still exists from a previous test run

  await $.db.createTable(booksTable.name, {
    definition: tableDefinition,
    ifNotExists: true,
  });

  await booksTable.createIndex(`${booksTable.name}_rating_index`, "rating", { ifNotExists: true });
  await booksTable.createIndex(`${booksTable.name}_genres_index`, "genres", { ifNotExists: true });
  await booksTable.createIndex(`${booksTable.name}_number_of_pages_index`, "number_of_pages", { ifNotExists: true });

  await booksTable.createVectorIndex(
    `${booksTable.name}_summary_genres_vector_index`,
    "summary_genres_vector",
    {
      options: {
        metric: "cosine",
      },
      ifNotExists: true,
    },
  );
}

export async function Teardown() {
  await booksTable.drop();
}
