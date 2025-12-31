package com.dtsx.docs.runner.drivers;

import com.dtsx.docs.runner.drivers.impls.BashDriver;
import com.dtsx.docs.runner.drivers.impls.JavaDriver;
import com.dtsx.docs.runner.drivers.impls.PythonDriver;
import com.dtsx.docs.runner.drivers.impls.TypeScriptDriver;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.util.function.Function;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum ClientLanguage {
    JAVA(".java", "com.datastax.astra:astra-db-java:+", JavaDriver::new),
    PYTHON(".py", "", PythonDriver::new),
    TYPESCRIPT(".ts", "@datastax/astra-db-ts", TypeScriptDriver::new),
    CSHARP(".cs", "", null),
    BASH(".sh", "", BashDriver::new),
    GO(".go", "", null);

    private final String extension;
    private final String defaultVersion;
    private final Function<String, ClientDriver> mkDriver;
}
