package com.hivemq.edge.adapters.file.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.edge.adapters.file.convertion.MappingException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.Function;

@SuppressWarnings("unused")
public enum ContentType {

    BINARY(ContentType::mapBinary),
    TEXT_PLAIN(ContentType::mapPlainText),
    TEXT_JSON(ContentType::mapJson),
    TEXT_XML(ContentType::mapPlainText),
    TEXT_CSV(ContentType::mapPlainText);

    private static final @NotNull ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(ContentType.class);

    private final @NotNull Function<byte[], Object> mapperFunction;

    ContentType(final @NotNull Function<byte[], Object> mapperFunction) {
        this.mapperFunction = mapperFunction;
    }

    public @Nullable Object map(byte @NotNull [] fileContent) {
        return mapperFunction.apply(fileContent);
    }

    public static @NotNull Object mapBinary(final byte @NotNull [] data) {
        return Base64.getEncoder().encode(data);
    }

    public static @NotNull Object mapPlainText(final byte @NotNull [] data) {
        return new String(data, StandardCharsets.UTF_8);
    }

    public static @NotNull Object mapJson(final byte @NotNull [] data) {
        try {
            return OBJECT_MAPPER.readTree(data);
        } catch (IOException e) {
            throw new MappingException("The content of the file could not be parsed to a JSON:" + e.getMessage());
        }
    }
}


