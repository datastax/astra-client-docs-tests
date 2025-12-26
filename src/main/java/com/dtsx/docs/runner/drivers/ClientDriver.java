package com.dtsx.docs.runner.drivers;

import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import com.dtsx.docs.runner.ExecutionEnvironment;
import com.dtsx.docs.runner.drivers.impls.BashDriver;
import com.dtsx.docs.runner.drivers.impls.JavaDriver;
import com.dtsx.docs.runner.drivers.impls.PythonDriver;
import com.dtsx.docs.runner.drivers.impls.TypeScriptDriver;
import lombok.val;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public interface ClientDriver {
    ClientLanguage language();

    List<Function<VerifierCtx, ExternalProgram>> requiredPrograms();

    Path setupExecutionEnvironment(VerifierCtx ctx, ExecutionEnvironment execEnv);

    String preprocessScript(VerifierCtx ignoredCtx, String content);

    RunResult execute(VerifierCtx ctx, ExecutionEnvironment execEnv);

    static ClientDriver parse(String driver) {
        final Map<String, Supplier<ClientDriver>> availableDrivers = Map.of(
            "typescript", TypeScriptDriver::new,
            "bash", BashDriver::new,
            "python", PythonDriver::new,
            "java", JavaDriver::new
        );

        val supplier = availableDrivers.get(driver.toLowerCase());

        if (supplier == null) {
            throw new IllegalArgumentException("Unknown driver: '" + driver + "' (expected one of: " + String.join(", ", availableDrivers.keySet()) + ")");
        }

        return supplier.get();
    }
}
