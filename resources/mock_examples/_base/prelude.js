import { DataAPIClient } from '@datastax/astra-db-ts';

export const Token = process.env.APPLICATION_TOKEN;
export const ApiEndpoint = process.env.API_ENDPOINT;

export const KeyspaceName = process.env.KEYSPACE_NAME;
export const CollectionName = process.env.COLLECTION_NAME;
export const TableName = process.env.TABLE_NAME;

export const client = new DataAPIClient(Token, {
  dbOptions: { keyspace: KeyspaceName },
  timeoutDefaults: {
    generalMethodTimeoutMs: 60000,
    requestTimeoutMs: 60000,
  }
});

export const db = client.db(ApiEndpoint);
