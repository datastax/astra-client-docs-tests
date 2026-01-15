package com.dtsx.docs.runner.scripts.reporter;

import com.dtsx.docs.lib.CliLogger;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;

public class PlainScriptReporter extends ScriptReporter {
    @Override
    public void printResult(String scriptName, RunResult result) {
        CliLogger.debug("Script '" + scriptName + "' finished with exit code " + result.exitCode());
        CliLogger.println(true, result.output());
    }

    @Override
    public void printBailMessage() {
        // noop
    }
}
