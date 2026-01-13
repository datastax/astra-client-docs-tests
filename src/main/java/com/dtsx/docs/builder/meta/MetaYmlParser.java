package com.dtsx.docs.builder.meta;

import com.dtsx.docs.builder.TestPlanException;
import com.dtsx.docs.builder.meta.impls.BaseMetaYml;
import com.dtsx.docs.builder.meta.impls.CompilesTestMetaYml;
import com.dtsx.docs.builder.meta.impls.SnapshotTestMetaYml;
import com.dtsx.docs.builder.meta.reps.BaseMetaYmlRep;
import com.dtsx.docs.builder.meta.reps.CompilesTestMetaYmlRep;
import com.dtsx.docs.builder.meta.reps.SnapshotTestMetaYmlRep;
import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.lib.JacksonUtils;
import lombok.val;
import org.jetbrains.annotations.Nullable;
import tools.jackson.core.JacksonException;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.dtsx.docs.runner.VerifyMode.COMPILE_ONLY;

public class MetaYmlParser {
    public static @Nullable BaseMetaYml parseMetaYml(VerifierCtx ctx, Path ymlFile) {
        val rep = parseRep(ymlFile);

        validateSchemaPath(rep, ymlFile);

        if (rep.test().type() != rep.expectTestType()) {
            throw new TestPlanException("'" + ymlFile + "' was parsed as a '" + rep.expectTestType() + "' test descriptor, but was actually a '" + rep.test().type() + "' test descriptor");
        }

        if (rep.test().skip().orElse(false)) {
            return null;
        }

        if (ctx.verifyMode() == COMPILE_ONLY) {
            return new CompilesTestMetaYml(); // Dependent on the invariant that all snapshot tests can be run as compilation tests
        }

        return switch (rep) {
            case SnapshotTestMetaYmlRep m -> new SnapshotTestMetaYml(ctx, ymlFile.getParent(), m);
            case CompilesTestMetaYmlRep _ -> new CompilesTestMetaYml();
        };
    }

    private static BaseMetaYmlRep parseRep(Path file) {
        try {
            return JacksonUtils.parseYaml(file, SnapshotTestMetaYmlRep.class);
        } catch (JacksonException se) {
            try {
                return JacksonUtils.parseYaml(file, CompilesTestMetaYmlRep.class);
            } catch (JacksonException ce) {
                throw new TestPlanException("Failed to parse meta.yml file at '" + file + "'; errors:\n" +
                    "- SnapshotTestMetaYml: " + se.getMessage() + "\n" +
                    "- CompilesTestMetaYml: " + ce.getMessage());
            }
        }
    }

    private static void validateSchemaPath(BaseMetaYmlRep rep, Path ymlFile) {
        val schemaPath = ymlFile.getParent().resolve(rep.$schema());

        if (!Files.exists(schemaPath)) {
            throw new TestPlanException("Invalid $schema path for '" + ymlFile + "'");
        }
    }
}
