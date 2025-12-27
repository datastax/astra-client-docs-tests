package com.dtsx.docs.lib;

import lombok.val;
import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JacksonUtils {
    private static final YAMLMapper YAML = YAMLMapper.builder()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
        .build();

    public static <T> T parseYaml(Path file, Class<T> clazz) {
        try (val reader = Files.newBufferedReader(file)) {
            return YAML.readValue(reader, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse YAML file: " + file, e);
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

    public static String prettyPrintJson(Object obj) {
        try {
            return PRETTY_JSON.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize object to YAML", e);
        }
    }
}
