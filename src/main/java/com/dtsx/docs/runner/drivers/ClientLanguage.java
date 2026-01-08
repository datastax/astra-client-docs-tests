package com.dtsx.docs.runner.drivers;

import com.dtsx.docs.runner.drivers.impls.BashDriver;
import com.dtsx.docs.runner.drivers.impls.JavaDriver;
import com.dtsx.docs.runner.drivers.impls.PythonDriver;
import com.dtsx.docs.runner.drivers.impls.TypeScriptDriver;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

@Getter
@RequiredArgsConstructor
public enum ClientLanguage {
    JAVA(".java", "com.datastax.astra:astra-db-java:+", JavaDriver::new),
    PYTHON(".py", "", PythonDriver::new),
    TYPESCRIPT(".ts", "@datastax/astra-db-ts", TypeScriptDriver::new),
    CSHARP(".cs", "", null),
    BASH(".sh", null, BashDriver::new),
    GO(".go", "", null);

    private final String extension;
    private final @Nullable String defaultArtifact;
    private final Function<String, ClientDriver> mkDriver;
}
