package com.dtsx.docs.runner.drivers;

import com.dtsx.docs.runner.drivers.impls.*;
import com.dtsx.docs.runner.snapshots.SnapshotSource;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/// Represents the various client languages available in enum form.
///
/// Useful, as `enum`s come with parsing and holding static information for free.
///
/// @see ClientDriver
@Getter
@RequiredArgsConstructor
public enum ClientLanguage {
    JAVA(".java", "com.datastax.astra:astra-db-java:+", JavaDriver::new),
    PYTHON(".py", null, PythonDriver::new),
    TYPESCRIPT(".ts", "@datastax/astra-db-ts", TypeScriptDriver::new),
    CSHARP(".cs", null, CSharpDriver::new),
    BASH(".sh", null, BashDriver::new),
    GO(".go", null, GoDriver::new);

    private final String extension;
    private final @Nullable String defaultArtifact;
    private final Function<String, ClientDriver> mkDriver;
}
