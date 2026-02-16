import { Collection, DataAPIClient, Table } from '@datastax/astra-db-ts';

export const Token = process.env.APPLICATION_TOKEN;
export const ApiEndpoint = process.env.API_ENDPOINT;
export const KeyspaceName = process.env.KEYSPACE_NAME;
export const NameRoot = process.env.NAME_ROOT;

export const client = new DataAPIClient(Token, {
  dbOptions: { keyspace: KeyspaceName },
  timeoutDefaults: {
    generalMethodTimeoutMs: 60000,
    requestTimeoutMs: 60000,
  }
});

export const db = client.db(ApiEndpoint);

export function name(name) {
  return `${NameRoot}_${name}`;
}

export function withUtils(schemaObj) {
  schemaObj.truncate = () => truncate(schemaObj);

  if (schemaObj instanceof Table) {
    schemaObj.addColumns = (columns) => addColumns(schemaObj, columns);
    schemaObj.dropColumns = (...columns) => dropColumns(schemaObj, columns);
  }

  return schemaObj;
}

async function truncate(schemaObj) {
  (schemaObj instanceof Collection)
    ? await truncateCollection(schemaObj)
    : await truncateTable(schemaObj);
}

async function truncateCollection(collection) {
  await collection.deleteMany({ i_dont_exist_for_sure: { $exists: false } });
}

async function truncateTable(table) {
  if (table._primaryKeys === undefined) {
    const { primaryKey } = await table.definition();

    table._primaryKeys = [
      primaryKey.partitionBy,
      Object.keys(primaryKey.partitionSort ?? {}),
    ].flat();
  }

  const docs = await table.find({}).toArray();

  await Promise.all(docs.map((doc) => {
    const filter = {};

    for (const pkey of table._primaryKeys) {
      filter[pkey] = doc[pkey];
    }

    return table.deleteMany(filter);
  }));
}

async function addColumns(table, columns) {
  try {
    await table.alter({
      operation: { add: { columns } },
    });
  } catch (e) {
    if (!e.message.includes('Column names must be unique in the table schema.')) {
      throw e;
    }
  }
}

async function dropColumns(table, columns) {
  try {
    await table.alter({
      operation: { drop: { columns } },
    });
  } catch (e) {
    if (!e.message.includes(' The command attempted to drop columns that are not in the table schema.')) {
      throw e;
    }
  }
}
