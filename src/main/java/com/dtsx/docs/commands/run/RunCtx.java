package com.dtsx.docs.commands.run;

import com.dtsx.docs.config.ctx.BaseScriptRunnerCtx;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.core.runner.Placeholders;
import com.dtsx.docs.core.runner.drivers.ClientDriver;
import com.dtsx.docs.core.runner.drivers.ClientLanguage;
import com.dtsx.docs.core.runner.scripts.reporter.DetailedScriptReporter;
import com.dtsx.docs.core.runner.scripts.reporter.PlainScriptReporter;
import com.dtsx.docs.core.runner.scripts.reporter.ScriptReporter;
import lombok.Getter;
import lombok.val;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

import static java.util.stream.Collectors.toSet;

@Getter
public class RunCtx extends BaseScriptRunnerCtx {
    private final Map<ClientDriver, Set<Path>> scripts;
    private final Placeholders placeholders;
    private final ScriptReporter reporter;

    public RunCtx(RunArgs args, CommandSpec spec) {
        super(args, spec);

        this.placeholders = mkPlaceholders(args);
        this.scripts = associateDriversToFiles(mkScriptPaths(args), args);
        this.reporter = mkReporter(args);

        verifyRequiredProgramsAvailable(cmd);
    }

    private Placeholders mkPlaceholders(RunArgs args) {
        val collectionName = args.$collection.orElse(null);
        val tableName = args.$table.orElse(null);
        val keyspaceName = args.$keyspace;

        if (collectionName != null || tableName != null) {
            return new Placeholders(collectionName, tableName, keyspaceName);
        }

        return Placeholders.EMPTY;
    }

    private Set<Path> mkScriptPaths(RunArgs args) {
        if (args.$scripts == null || args.$scripts.isEmpty()) {
            throw new ParameterException(cmd, "Must provide at least one file to run.");
        }

        return args.$scripts.stream()
            .map(this::resolveAbsFilePath)
            .collect(toSet());
    }

    private Path resolveAbsFilePath(String fileStr) {
        val roots = List.of(
            examplesFolder(),
            Path.of(System.getenv("USERS_ACTUAL_CWD"))
        );

        val triedPaths = new HashSet<String>();

        for (val root : roots) {
            val candidate = root.resolve(fileStr);

            if (!candidate.startsWith(examplesFolder())) {
                throw new ParameterException(cmd, "Invalid file path '" + candidate + "'. Must be inside examples folder: " + examplesFolder());
            }

            if (Files.exists(candidate)) {
                return candidate;
            }

            triedPaths.add(candidate.toAbsolutePath().normalize().toString());
        }

        throw new ParameterException(cmd, "Could not resolve '" + fileStr + "'. Tried: " + String.join(", ", triedPaths));
    }

    private Map<ClientDriver, Set<Path>> associateDriversToFiles(Set<Path> files, RunArgs args) {
        val langsToFiles = new HashMap<ClientLanguage, Set<Path>>() {{
            for (val file : files) {
                computeIfAbsent(resolveLanguageForFile(file), _ -> new HashSet<>()).add(file);
            }
        }};

        return new HashMap<>() {{
            langsToFiles.forEach((lang, paths) -> {
                put(mkDriverForLanguage(cmd, lang, args), paths);
            });
        }};
    }

    private ClientLanguage resolveLanguageForFile(Path path) {
        val fileName = path.getFileName().toString().toLowerCase();

        for (val lang : ClientLanguage.values()) {
            if (fileName.endsWith(lang.extension())) {
                return lang;
            }
        }

        throw new ParameterException(cmd, "Unknown language extension for file '" + path + "'");
    }

    private ScriptReporter mkReporter(RunArgs args) {
        if (args.$plain) {
            if (args.$scripts.size() > 1) {
                throw new ParameterException(cmd, "The --plain option can only be used when running a single script file.");
            }
            return new PlainScriptReporter();
        }
        return new DetailedScriptReporter();
    }

    private void verifyRequiredProgramsAvailable(CommandLine cmd) {
        val requiredPrograms = new HashSet<Function<BaseScriptRunnerCtx, ExternalProgram>>() {{
            for (val driver : scripts.keySet()) {
                addAll(driver.requiredPrograms());
            }
        }};

        verifyRequiredProgramsAvailable(requiredPrograms, cmd);
    }
}
