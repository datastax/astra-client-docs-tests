package com.dtsx.docs.lib;

import lombok.SneakyThrows;
import lombok.val;
import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.nio.file.Files;
import java.nio.file.Path;

public class JacksonUtils {
    private static final YAMLMapper YAML = YAMLMapper.builder()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
        .build();

    @SneakyThrows
    public static <T> T parseYaml(Path file, Class<T> clazz) {
        try (val reader = Files.newBufferedReader(file)) {
            return YAML.readValue(reader, clazz);
        }
    }

    private static final JsonMapper PRETTY_JSON;

    static {
        val pp = new DefaultPrettyPrinter()
            .withObjectIndenter(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE)
            .withArrayIndenter(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);

        PRETTY_JSON = JsonMapper.builder()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            .defaultPrettyPrinter(pp)
            .build();
    }

    @SneakyThrows
    public static String prettyPrintJson(Object obj) {
        return PRETTY_JSON.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
    }
}
