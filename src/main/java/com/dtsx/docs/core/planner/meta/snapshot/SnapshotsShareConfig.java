package com.dtsx.docs.core.planner.meta.snapshot;

import com.dtsx.docs.commands.test.TestCtx;
import com.dtsx.docs.core.runner.drivers.ClientLanguage;
import com.dtsx.docs.lib.JacksonUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.val;
import tools.jackson.core.type.TypeReference;

import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toMap;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SnapshotsShareConfig {
    private final Map<ClientLanguage, Boolean> languages;

    public boolean isShared(ClientLanguage language) {
        return languages.getOrDefault(language, true);
    }

    public static SnapshotsShareConfig parse(TestCtx ctx, Optional<Object> maybeRaw) {
        val raw = maybeRaw.orElse(true);

        if (raw instanceof Boolean bool) {
            return new SnapshotsShareConfig(
                ctx.languages().stream().collect(toMap(lang -> lang, _ -> bool))
            );
        }

        try {
            return new SnapshotsShareConfig(
                JacksonUtils.convertValue(raw, new TypeReference<>() {})
            );
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Invalid share config: " + raw, e);
        }
    }
}
