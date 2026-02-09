import * as $ from "../_base/prelude";
import {
  Table,
} from "@datastax/astra-db-ts";

export const booksTable = $.db.table(Meta().TableName);

booksTable.truncate = async function () {
  await $.truncate(this);
};

export function Meta() {
  return {
    TableName: "books_table",
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
    },
    primaryKey: {
      partitionBy: ["title", "author"],
    },
  });

  await $.db.createTable(booksTable.name, {
    definition: tableDefinition,
  });

  await booksTable.createIndex(`${booksTable.name}_rating_index`, "rating");

  await booksTable.createIndex(`${booksTable.name}_number_of_pages_index`, "number_of_pages");

  await booksTable.createVectorIndex(
    `${booksTable.name}_summary_genres_vector_index`,
    "summary_genres_vector",
    {
      options: {
        metric: "cosine",
      },
    },
  );
}

export async function Teardown() {
  await booksTable.drop();
}
