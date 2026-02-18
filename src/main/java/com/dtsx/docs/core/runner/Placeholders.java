package com.dtsx.docs.core.runner;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Placeholders {
    protected Optional<String> collectionName = Optional.empty();

    protected Optional<String> tableName = Optional.empty();

    protected String keyspaceName = "default_keyspace";

    protected Map<String, String> vars = new HashMap<>();
}
