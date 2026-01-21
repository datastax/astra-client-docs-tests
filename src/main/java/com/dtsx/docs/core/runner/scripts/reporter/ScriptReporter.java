package com.dtsx.docs.core.runner.scripts.reporter;

import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class ScriptReporter {
    public abstract void printResult(String scriptName, RunResult result);
    public abstract void printBailMessage();
}
