# Schema Update Plan for meta.schema.json

## Overview
This document outlines the required updates to `resources/mock_examples/_base/meta.schema.json` based on the parameter definitions found in the Java source code.

## Current State Analysis

The current schema defines snapshot sources but is missing several optional and required parameters that are used by the Java implementation in `SnapshotSourcesParser.java` and related source classes.

## Required Changes

### 1. collection::definition
**Current:** Empty object with no properties
**Required:** Add optional `collection` and `keyspace` parameters

**Source:** `CollectionDefinitionSource.java` uses `WithNameAndKeyspace.CollectionImpl`
- Constructor: `CollectionDefinitionSource(String name, WithNameAndKeyspace.CollectionImpl nameAndKeyspace)`
- Parameters from `CollectionImpl(Optional<String> collection, Optional<String> keyspace)`

**Schema Update:**
```json
"collection::definition": {
  "type": ["object", "null"],
  "properties": {
    "collection": {
      "type": "string",
      "description": "Optional collection name override"
    },
    "keyspace": {
      "type": "string",
      "description": "Optional keyspace name override"
    }
  },
  "additionalProperties": false,
  "description": "Capture collection definition/schema"
}
```

### 2. table::definition
**Current:** Empty object with no properties
**Required:** Add optional `table` and `keyspace` parameters

**Source:** `TableDefinitionSource.java` uses `WithNameAndKeyspace.TableImpl`
- Constructor: `TableDefinitionSource(String name, WithNameAndKeyspace.TableImpl nameAndKeyspace)`
- Parameters from `TableImpl(Optional<String> table, Optional<String> keyspace)`

**Schema Update:**
```json
"table::definition": {
  "type": ["object", "null"],
  "properties": {
    "table": {
      "type": "string",
      "description": "Optional table name override"
    },
    "keyspace": {
      "type": "string",
      "description": "Optional keyspace name override"
    }
  },
  "additionalProperties": false,
  "description": "Capture table definition/schema"
}
```

### 3. collection::names
**Current:** Empty object with no properties
**Required:** Add optional `keyspace` parameter

**Source:** `CollectionNamesSource.java` uses `WithKeyspace.Impl`
- Constructor: `CollectionNamesSource(String name, WithKeyspace.Impl keyspace)`
- Parameters from `Impl(Optional<String> keyspace)`

**Schema Update:**
```json
"collection::names": {
  "type": ["object", "null"],
  "properties": {
    "keyspace": {
      "type": "string",
      "description": "Optional keyspace name override"
    }
  },
  "additionalProperties": false,
  "description": "Capture collection names"
}
```

### 4. table::names
**Current:** Empty object with no properties
**Required:** Add optional `keyspace` parameter

**Source:** `TableNamesSource.java` uses `WithKeyspace.Impl`
- Constructor: `TableNamesSource(String name, WithKeyspace.Impl keyspace)`
- Parameters from `Impl(Optional<String> keyspace)`

**Schema Update:**
```json
"table::names": {
  "type": ["object", "null"],
  "properties": {
    "keyspace": {
      "type": "string",
      "description": "Optional keyspace name override"
    }
  },
  "additionalProperties": false,
  "description": "Capture table names"
}
```

### 5. udt::names
**Current:** Empty object with no properties
**Required:** Add optional `keyspace` parameter

**Source:** `UdtNamesSource.java` uses `WithKeyspace.Impl`
- Constructor: `UdtNamesSource(String name, WithKeyspace.Impl keyspace)`
- Parameters from `Impl(Optional<String> keyspace)`

**Schema Update:**
```json
"udt::names": {
  "type": ["object", "null"],
  "properties": {
    "keyspace": {
      "type": "string",
      "description": "Optional keyspace name override"
    }
  },
  "additionalProperties": false,
  "description": "Capture user-defined type names"
}
```

### 6. udt::definitions
**Current:** Empty object with no properties
**Required:** Add required `types` array and optional `keyspace` parameter

**Source:** `UdtDefinitionsSource.java` uses `UdtDefinitionSourceMeta`
- Constructor: `UdtDefinitionsSource(String name, UdtDefinitionSourceMeta meta)`
- Parameters from `UdtDefinitionSourceMeta(List<String> types, Optional<String> keyspace)`

**Schema Update:**
```json
"udt::definitions": {
  "type": ["object", "null"],
  "required": ["types"],
  "properties": {
    "types": {
      "type": "array",
      "items": {
        "type": "string"
      },
      "description": "List of UDT type names to capture definitions for"
    },
    "keyspace": {
      "type": "string",
      "description": "Optional keyspace name override"
    }
  },
  "additionalProperties": false,
  "description": "Capture user-defined type definitions"
}
```

## Summary of Changes

| Snapshot Source | Current State | New Parameters | Notes |
|----------------|---------------|----------------|-------|
| `collection::definition` | No properties | `collection` (optional), `keyspace` (optional) | Allows overriding collection/keyspace names |
| `table::definition` | No properties | `table` (optional), `keyspace` (optional) | Allows overriding table/keyspace names |
| `collection::names` | No properties | `keyspace` (optional) | Allows overriding keyspace name |
| `table::names` | No properties | `keyspace` (optional) | Allows overriding keyspace name |
| `udt::names` | No properties | `keyspace` (optional) | Allows overriding keyspace name |
| `udt::definitions` | No properties | `types` (required array), `keyspace` (optional) | **Breaking change** - types is required |

## Implementation Notes

1. **Breaking Change:** The `udt::definitions` source now requires a `types` array parameter. Any existing meta.yml files using this source will need to be updated.

2. **Consistency:** All schema-related sources now support an optional `keyspace` parameter for overriding the default keyspace.

3. **Name Overrides:** Definition sources for collections and tables support overriding the specific object name (collection/table) in addition to the keyspace.

4. **Backward Compatibility:** All new parameters except `types` in `udt::definitions` are optional, maintaining backward compatibility for existing configurations.

## Next Steps

1. Review this plan for accuracy and completeness
2. Switch to code mode to implement the schema changes
3. Validate the updated schema against existing meta.yml files
4. Update any documentation referencing these snapshot sources
