package com.dtsx.docs.runner.drivers;

import com.dtsx.docs.VerifierConfig;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import com.dtsx.docs.runner.drivers.impls.BashDriver;
import com.dtsx.docs.runner.drivers.impls.TypeScriptDriver;
import lombok.val;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public interface ClientDriver {
    ClientLanguage language();

    List<Function<VerifierConfig, ExternalProgram>> requiredPrograms();

    Path testFileCopyDestination(Path tempEnv);

    default void setup(VerifierConfig cfg, Path tempEnv) {
        // noop
    }

    default String preprocessScript(VerifierConfig ignoredCfg, String content) {
        return content;
    }

    RunResult execute(VerifierConfig cfg, Path script);

    static ClientDriver parse(String driver) {
        final Map<String, Supplier<ClientDriver>> availableDrivers = Map.of(
            "typescript", TypeScriptDriver::new,
            "bash", BashDriver::new
        );

        val supplier = availableDrivers.get(driver.toLowerCase());

        if (supplier == null) {
            throw new IllegalArgumentException("Unknown driver: '" + driver + "' (expected one of: " + String.join(", ", availableDrivers.keySet()) + ")");
        }

        return supplier.get();
    }
}
