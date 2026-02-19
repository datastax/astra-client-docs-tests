package com.dtsx.docs.commands.duplicates;

import com.dtsx.docs.commands.BaseCmd;
import com.dtsx.docs.lib.CliLogger;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.val;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.dtsx.docs.lib.ColorUtils.color;
import static picocli.CommandLine.Help.Ansi.Style;

@Command(
    name = "duplicates",
    description = "Detect language-specific approved files that are identical to shared.approved.txt"
)
public class DuplicatesCmd extends BaseCmd<DuplicatesCtx> {
    @Mixin
    @Getter
    private DuplicatesArgs $args;

    @Override
    @SneakyThrows
    protected int run() {
        CliLogger.println(false, "@|bold Scanning for duplicate snapshots...|@");
        CliLogger.println(false);

        val duplicates = findDuplicates(ctx.examplesFolder());

        if (duplicates.isEmpty()) {
            CliLogger.println(false, "@|green ✓|@ No duplicates found!");
            return 0;
        }

        CliLogger.println(false, "@|bold Found @!" + duplicates.size() + "!@ duplicate(s):|@");
        CliLogger.println(false);

        for (val duplicate : duplicates) {
            printDuplicate(duplicate);
        }

        CliLogger.println(false);
        CliLogger.println(false, "@|bold Summary:|@");
        CliLogger.println(false, "@!-!@ Total duplicates: " + duplicates.size());

        return 1;
    }

    @SneakyThrows
    private List<Duplicate> findDuplicates(Path examplesRoot) {
        val duplicates = new ArrayList<Duplicate>();

        try (val paths = Files.walk(examplesRoot)) {
            paths.filter(Files::isDirectory)
                .forEach(dir -> checkDirectoryForDuplicates(examplesRoot, dir, duplicates));
        }

        return duplicates;
    }

    @SneakyThrows
    private void checkDirectoryForDuplicates(Path snapshotsRoot, Path dir, List<Duplicate> duplicates) {
        val sharedFile = dir.resolve("shared.approved.txt");

        if (!Files.exists(sharedFile)) {
            return;
        }

        val sharedContent = Files.readString(sharedFile);

        try (val files = Files.list(dir)) {
            files.filter(f -> f.getFileName().toString().matches("\\w+\\.approved\\.txt"))
                .filter(f -> !f.getFileName().toString().equals("shared.approved.txt"))
                .forEach(langFile -> checkFileForDuplicate(snapshotsRoot, dir, langFile, sharedContent, duplicates));
        }
    }

    @SneakyThrows
    private void checkFileForDuplicate(Path snapshotsRoot, Path dir, Path langFile, String sharedContent, List<Duplicate> duplicates) {
        val langContent = Files.readString(langFile);
        if (sharedContent.equals(langContent)) {
            val relativePath = snapshotsRoot.relativize(dir);
            val language = langFile.getFileName().toString().replace(".approved.txt", "");
            duplicates.add(new Duplicate(relativePath, language, langFile));
        }
    }

    private void printDuplicate(Duplicate duplicate) {
        CliLogger.println(false, "  @|red ✗|@ " + duplicate.testPath());
        CliLogger.println(false, "    " + color(Style.faint, "Language: ") + "@|yellow " + duplicate.language() + "|@");
        CliLogger.println(false, "    " + color(Style.faint, "File: ") + duplicate.filePath());
        CliLogger.println(false);
    }

    private record Duplicate(Path testPath, String language, Path filePath) {}
}
