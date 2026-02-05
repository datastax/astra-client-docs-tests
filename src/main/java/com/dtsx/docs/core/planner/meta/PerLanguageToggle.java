package com.dtsx.docs.core.planner.meta;

import com.dtsx.docs.commands.test.TestCtx;
import com.dtsx.docs.core.runner.drivers.ClientLanguage;
import com.dtsx.docs.lib.JacksonUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.val;
import tools.jackson.core.type.TypeReference;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class PerLanguageToggle {
    protected final Map<ClientLanguage, Boolean> languages;

    public static <T extends PerLanguageToggle> T parse(Function<Map<ClientLanguage, Boolean>, T> cons, TestCtx ctx, Optional<Object> maybeRaw) {
        if (maybeRaw.isEmpty()) {
            return cons.apply(Map.of());
        }

        val raw = maybeRaw.get();

        if (raw instanceof Boolean bool) {
            return cons.apply(
                ctx.languages().stream().collect(toMap(lang -> lang, _ -> bool))
            );
        }

        try {
            return cons.apply(
                JacksonUtils.convertValue(raw, new TypeReference<>() {})
            );
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Invalid share config: " + raw, e);
        }
    }
}
