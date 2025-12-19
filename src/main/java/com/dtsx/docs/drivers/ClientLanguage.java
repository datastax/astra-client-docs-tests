package com.dtsx.docs.drivers;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum ClientLanguage {
    JAVA(".java"),
    PYTHON(".py"),
    TYPESCRIPT(".ts"),
    CSHARP(".cs"),
    SHELL(".sh"),
    GO(".go");

    private final String extension;
}
