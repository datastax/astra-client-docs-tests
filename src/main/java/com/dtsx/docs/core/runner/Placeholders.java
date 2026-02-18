package com.dtsx.docs.core.runner;

import com.dtsx.docs.core.planner.fixtures.BaseFixturePool.FixtureIndex;
import com.dtsx.docs.core.runner.drivers.ClientLanguage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Pattern;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Placeholders {
    protected Optional<String> collectionName = Optional.empty();

    protected Optional<String> tableName = Optional.empty();

    protected String keyspaceName = "default_keyspace";

    protected PlaceholderVars vars = new PlaceholderVars();

    @NoArgsConstructor
    public static class PlaceholderVars {
        protected List<Pair<String, String>> vars = new ArrayList<>();

        public PlaceholderVars(Map<String, String> vars) {
            vars.forEach((k, v) -> this.vars.add(Pair.of(k, v)));
        }

        public Optional<String> get(ClientLanguage language, String key) {
            return get(language, null, key);
        }

        public Optional<String> get(ClientLanguage language, @Nullable FixtureIndex index, String key) {
            return vars.stream()
                .filter(p -> p.getKey().equals(key))
                .map(p -> getValue(p, language, index))
                .findFirst();
        }

        public List<Pair<String, String>> getAll(ClientLanguage language) {
            return vars.stream()
                .map(p -> Pair.of(p.getKey(), getValue(p, language, null)))
                .toList();
        }

        private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{.+?}");

        private String getValue(Pair<String, String> p, ClientLanguage language, @Nullable FixtureIndex index) {
            return resolveVariables(this, language, index, p.getValue());
        }

        public static String resolveVariables(PlaceholderVars vars, ClientLanguage language, @Nullable FixtureIndex index, String input) {
            return resolveVariables(vars, language, index, input, new HashSet<>());
        }

        private static String resolveVariables(PlaceholderVars vars, ClientLanguage language, @Nullable FixtureIndex index, String input, Set<String> visitedVars) {
            val matcher = VARIABLE_PATTERN.matcher(input);
            val sb = new StringBuilder();

            while (matcher.find()) {
                val replacement = resolveSingleVariable(vars, language, index, matcher.group(), visitedVars);
                matcher.appendReplacement(sb, replacement);
            }

            matcher.appendTail(sb);
            return sb.toString();
        }

        private static @NotNull String resolveSingleVariable(PlaceholderVars vars, ClientLanguage language, @Nullable FixtureIndex index, String variable, Set<String> visitedVars) {
            if (visitedVars.contains(variable)) {
                throw new RunException("Circular dependency detected in variable resolution: " + variable + " (resolution chain: " + String.join(" -> ", visitedVars) + " -> " + variable + ")");
            }

            val varName = variable.substring(2, variable.length() - 1); // strip ${ and }

            return switch (varName) {
                case "LANGUAGE" -> language.name().toLowerCase();
                case "NAME_ROOT" -> {
                    if (index == null) {
                        throw new RunException("${NAME_ROOT} used in an invalid context. It should only be used under in snapshot source blocks for the time being.");
                    }
                    yield index.toNameRoot();
                }
                default -> {
                    val userVar = vars.vars.stream()
                        .filter(p -> p.getKey().equals(varName))
                        .findFirst();
                    
                    if (userVar.isPresent()) {
                        val newVisitedVars = new HashSet<>(visitedVars);
                        newVisitedVars.add(variable);
                        yield resolveVariables(vars, language, index, userVar.get().getValue(), newVisitedVars);
                    } else {
                        throw new RunException("Unrecognized variable in placeholder key '" + variable + "'. Expected one of ${LANGUAGE}, ${NAME_ROOT}, or a user-defined variable.");
                    }
                }
            };
        }
    }
}
