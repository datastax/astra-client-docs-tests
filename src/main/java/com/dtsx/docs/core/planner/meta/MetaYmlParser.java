package com.dtsx.docs.core.planner.meta;

import com.dtsx.docs.core.planner.PlanException;
import com.dtsx.docs.core.planner.meta.compiles.CompilesTestMeta;
import com.dtsx.docs.core.planner.meta.snapshot.SnapshotTestMeta;
import com.dtsx.docs.core.planner.meta.compiles.CompilesTestMetaRep;
import com.dtsx.docs.core.planner.meta.snapshot.SnapshotTestMetaRep;
import com.dtsx.docs.commands.test.TestCtx;
import com.dtsx.docs.lib.JacksonUtils;
import lombok.val;
import org.jetbrains.annotations.Nullable;
import tools.jackson.core.JacksonException;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.dtsx.docs.core.runner.tests.VerifyMode.COMPILE_ONLY;

public class MetaYmlParser {
    public static @Nullable BaseMetaYml parseMetaYml(TestCtx ctx, Path ymlFile) {
        val rep = parseRep(ymlFile);

        validateSchemaPath(rep, ymlFile);

        if (rep.test().type() != rep.expectTestType()) {
            throw new PlanException("'" + ymlFile + "' was parsed as a '" + rep.expectTestType() + "' test descriptor, but was actually a '" + rep.test().type() + "' test descriptor");
        }

        if (rep.test().skip().orElse(false)) {
            return null;
        }

        if (ctx.verifyMode() == COMPILE_ONLY) {
            return new CompilesTestMeta(); // Dependent on the invariant that all snapshot tests can be run as compilation tests
        }

        return switch (rep) {
            case SnapshotTestMetaRep m -> new SnapshotTestMeta(ctx, ymlFile.getParent(), m);
            case CompilesTestMetaRep _ -> new CompilesTestMeta();
            default ->  null; // unreachable
        };
    }

    private static BaseMetaYml.BaseMetaYmlRep parseRep(Path file) {
        try {
            return JacksonUtils.parseYaml(file, SnapshotTestMetaRep.class);
        } catch (JacksonException se) {
            try {
                return JacksonUtils.parseYaml(file, CompilesTestMetaRep.class);
            } catch (JacksonException ce) {
                throw new PlanException("Failed to parse meta.yml file at '" + file + "'; errors:\n" +
                    "- SnapshotTestMetaYml: " + se.getMessage() + "\n" +
                    "- CompilesTestMetaYml: " + ce.getMessage());
            }
        }
    }

    private static void validateSchemaPath(BaseMetaYml.BaseMetaYmlRep rep, Path ymlFile) {
        val schemaPath = ymlFile.getParent().resolve(rep.$schema());

        if (!Files.exists(schemaPath)) {
            throw new PlanException("Invalid $schema path for '" + ymlFile + "'");
        }
    }
}
