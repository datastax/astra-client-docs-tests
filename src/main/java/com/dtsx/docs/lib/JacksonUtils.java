package com.dtsx.docs.lib;

import lombok.SneakyThrows;
import lombok.val;
import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.nio.file.Files;
import java.nio.file.Path;

public class JacksonUtils {
    private static final YAMLMapper YAML = YAMLMapper.builder()
        .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
        .build();

    @SneakyThrows
    public static <T> T parseYaml(Path file, Class<T> clazz) {
        try (val reader = Files.newBufferedReader(file)) {
            return YAML.readValue(reader, clazz);
        }
    }

    private static final JsonMapper JSON;

    static {
        val pp = new DefaultPrettyPrinter()
            .withObjectIndenter(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE)
            .withArrayIndenter(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);

        JSON = JsonMapper.builder()
            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            .defaultPrettyPrinter(pp)
            .build();
    }

    @SneakyThrows
    public static <T> T parseJson(String string, Class<T> clazz) {
        return JSON.readValue(string, clazz);
    }

    @SneakyThrows
    public static String prettyPrintJson(Object obj) {
        return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
    }
}
