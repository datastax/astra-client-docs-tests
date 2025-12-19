package com.dtsx.docs.lib;

import lombok.val;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Yaml {
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    public static <T> T parse(Path file, Class<T> clazz) {
        try (val reader = Files.newBufferedReader(file)) {
            return YAML.readValue(reader, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse YAML file: " + file, e);
        }
    }
}
