package com.dtsx.docs.core.planner.meta;

import com.dtsx.docs.core.planner.fixtures.JSFixture;
import com.dtsx.docs.core.planner.meta.BaseMetaYml.BaseMetaYmlRep.TestBlock.SkipConfig;
import com.dtsx.docs.core.runner.drivers.ClientLanguage;
import lombok.NonNull;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.Map;
import java.util.Optional;

@JsonDeserialize()
public interface BaseMetaYml {
    JSFixture baseFixture();

    SkipConfig skipConfig();

    interface BaseMetaYmlRep {
        String $schema();

        TestBlock test();

        TestType expectTestType();

        record TestBlock(
            @NonNull TestType type,
            @NonNull Optional<Object> skipConfig
        ) {
            public static class SkipConfig extends PerLanguageToggle {
                public SkipConfig(Map<ClientLanguage, Boolean> languages) {
                    super(languages);
                }

                public boolean isSkipped(ClientLanguage language) {
                    return languages.getOrDefault(language, false);
                }
            }
        }

        enum TestType {
            SNAPSHOT,
            COMPILES
        }
    }
}
