package com.dtsx.docs.drivers;

import java.nio.file.Path;

public class TypeScriptDriver extends ClientDriver {
    @Override
    public ClientLanguage language() {
        return ClientLanguage.TYPESCRIPT;
    }

    @Override
    public Path executionLocation(Path tempEnv) {
        return tempEnv.resolve("main.ts");
    }
}
