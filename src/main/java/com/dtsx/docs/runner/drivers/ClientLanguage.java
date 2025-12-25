package com.dtsx.docs.runner.drivers;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum ClientLanguage {
    JAVA(".java", "com.datastax.astra:astra-db-java:+"),
    PYTHON(".py", ""),
    TYPESCRIPT(".ts", "@datastax/astra-db-ts"),
    CSHARP(".cs", ""),
    BASH(".sh", ""),
    GO(".go", "");

    private final String extension;
    private final String defaultVersion;
}
