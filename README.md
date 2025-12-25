# Client docs examples regression testing

Clients may introduce unintentional breaking changes or other regressions in new releases, or documentation examples
may contain errors themselves. This repository provides a harness to automatically test said documentation examples
against Astra or HCD for all supported clients.

## Architecture

### Examples file structure

The harness will read the code examples from the `astra-vector-docs` repository, expecting the following structure:

```
examples/
  _fixtures/
    _prelude.js
    <fixture>.js
  <example>/
    java/
    csharp/
    example.ts
    example.py
    example.sh
    example.go
    [fixture.js]
    [meta.yml]
  ...
```

Where:
- `_prelude.js` sets up any common logic needed for all fixtures (e.g. client objects)
- Fixture files are explained [here](#fixture-files)
- `<example>/` is a directory for each example to be tested (e.g. `delete-many-filter/`)
- `meta.yml` is explained [here](#metayml)

### Fixture files

Fixture files are javascript files that export functions to set up, reset, or tear down text fixtures for example tests.

They are of the following form, where each function is optional:

```js
export async function Setup() {
  // setup collection/table/db logic
}

export async function Reset() {
  // reset collection/table/db logic
}

export async function Teardown() {
  // delete collection/table/db logic
}
```

Fixture files are placed either in the `_fixtures/` directory (for general fixtures shared between many examples),
or inside an example directory (for specialized fixtures specific to that example test).

See [below](#metayml) for how fixture files are used.

### `meta.yml`

The `meta.yml` file is placed inside an example directory to define how an example should be tested.

It has the following structure:

```yaml
fixtures:
  base: <fixture>      # name of the base fixture file (e.g. 'basic-collection.js')
  test: <fixture>      # name of the test-specific fixture file (optional; defaults to 'fixture.js' if exists)
snapshots:
  share: <boolean>     # whether to share snapshot files between clients (default: true)
  additional:          
    - <snapshot_type>  # things to snapshot after test run (e.g. 'collection', 'table', 'keyspaces', 'types', etc.)
    - ...              # defaults to just ['output'] if not specified
```

#### The `fixtures` field

There are two types of fixture files:
- The required "base" fixture file (living in `_fixtures`)
    - This sets up the general expensive resources shared between many examples
    - e.g. creating a specific collection
- The optional "test-specific" fixture file (living in the example directory)
    - Lightweight fixture logic that builds on top of the vase fixture to set up specific data needed for that specific test
    - e.g. inserting specific documents into a collection

Fixtures are only created and destroyed once per suite run, with the same fixture being reused across
all examples that reference it (hence the `Reset` function to clear data between tests).

#### The `snapshots.share` field

By default, snapshot files are shared between clients to reduce duplication and maintenance burden. 
This means that the same snapshot file is used to validate the output of all clients for a given example.

However, in some cases different clients may have slightly different output (e.g. different error messages,
or different formatting of results). In these cases, set `snapshots.share` to `false` to create separate
snapshot files per client.

#### The `snapshots.additional` field

The `snapshots` list defines exactly what content should be included in the snapshot for that example 
(see [Snapshot testing](#snapshot-testing) for more info/context).

Most commonly, snapshot is taken of the output of the program (i.e. stdout/stderr) to ensure it doesn't change
unexpectedly, but other state may be snapshot too, such as the contents of a collection or a list of the database's
keyspaces or UDTs.

Allowed values include:
- `output` - the stdout/stderr output of the program (this is always included by default)
- `collection` - the contents of the collection created by the fixture
- `table` - the contents of the table created by the fixture
- `collections` - the list of collections in the database
- `tables` - the list of tables in the database
- `keyspaces` - the list of keyspaces in the database
- `types` - the list of UDTs in the database

Order of items in the list does not matter.

### Snapshot testing

Snapshot testing, otherwise known as approval or golden master testing, is a technique where some output of a program
is captured and stored as a "snapshot" file, which is then used as a reference for future test runs to ensure that the
output remains consistent over time.

Snapshot testing is the primary mechanism used by this harness to detect regressions in documentation examples.

When an example is run, the harness captures the specified output (e.g. stdout/stderr, collection contents, etc.)
and compares it against the stored snapshot file for that example and client.

- If the output differs from the snapshot, the test fails, indicating a potential regression or change in behavior.
- If the change is intentional and correct, the snapshot file can be updated to reflect the new expected output.
