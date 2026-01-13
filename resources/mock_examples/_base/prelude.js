import {Collection, DataAPIClient, Table} from '@datastax/astra-db-ts';

export const Token = process.env.APPLICATION_TOKEN;
export const ApiEndpoint = process.env.API_ENDPOINT;
export const KeyspaceName = process.env.KEYSPACE_NAME;

export const client = new DataAPIClient(Token, {
  dbOptions: { keyspace: KeyspaceName },
  timeoutDefaults: {
    generalMethodTimeoutMs: 60000,
    requestTimeoutMs: 60000,
  }
});

export const db = client.db(ApiEndpoint);

export async function truncate(schemaObj, ...pkeys) {
  if (schemaObj instanceof Collection) {
    await schemaObj.deleteMany({ i_dont_exist_for_sure: { $exists: false } });
  }
  else if (schemaObj instanceof Table) {
    const docs = await schemaObj.find({}).toArray();

    if (pkeys.length === 0) {
      throw new Error('truncate function requires at least one primary key when truncating a Table');
    }

    await Promise.all(docs.map(async (doc) => {
      const filter = {};

      for (const pkey of pkeys) {
        filter[pkey] = doc[pkey];
      }

      await schemaObj.deleteMany(filter);
    }));
  }
}
