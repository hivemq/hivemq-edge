package com.hivemq.combining.runtime;

import com.hivemq.combining.model.DataIdentifierReference;
import org.jetbrains.annotations.NotNull;

public class SourceSanitizer {

    public static @NotNull String sanitize(final @NotNull DataIdentifierReference dataIdentifierReference) {
        return dataIdentifierReference.type() + ":" + sanitizeId(dataIdentifierReference.id());
    }

    private static @NotNull String sanitizeId(final @NotNull String id){
        final String escaped = id.replaceAll("//.", "/");
        return escaped;
    }
}
