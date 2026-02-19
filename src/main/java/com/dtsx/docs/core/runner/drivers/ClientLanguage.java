package com.dtsx.docs.core.runner.drivers;

import com.dtsx.docs.core.runner.drivers.impls.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static com.dtsx.docs.HelperCli.CLI_DIR;

/// Represents the various client languages available in enum form.
///
/// Useful, as `enum`s come with parsing and holding static information for free.
///
/// @see ClientDriver
@Getter
@RequiredArgsConstructor
public enum ClientLanguage {
    JAVA(".java", "\"com.datastax.astra:astra-db-java:2.+\"", JavaDriver::new),
    PYTHON(".py", "astrapy", PythonDriver::new),
    TYPESCRIPT(".ts", "@datastax/astra-db-ts", TypeScriptDriver::new),
    CSHARP(".cs", "<PackageReference Include=\"DataStax.AstraDB.DataApi\" Version=\"2.*-*\"/>", CSharpDriver::new),
//    GO(".go", null, GoDriver::new),
    BASH(".sh", null, BashDriver::new);

    private final String extension;
    private final @Nullable String defaultArtifact;
    private final Function<String, ClientDriver> mkDriver;

    public @Nullable String defaultArtifact() {
        if (this == JAVA) {
            return "files(\"" + CLI_DIR.toAbsolutePath() + "/astra-db-java-2.1.7-patched.jar\")"; // TODO remove this once Java is patched
        }
        return defaultArtifact;
    }

    public static List<String> names() {
        return Arrays.stream(ClientLanguage.values()).map(Enum::name).toList();
    }
}
