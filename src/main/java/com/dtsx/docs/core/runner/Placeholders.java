package com.dtsx.docs.core.runner;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Placeholders {
    public static final Placeholders EMPTY = new Placeholders();

    protected Optional<String> collectionName = Optional.empty();

    protected Optional<String> tableName = Optional.empty();

    protected String keyspaceName = "default_keyspace";
}
