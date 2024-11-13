package com.hivemq.persistence.fieldmapping;

import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.configuration.entity.adapter.TransformationEntity;
import com.hivemq.extension.sdk.api.annotations.NotNull;

@SuppressWarnings("InstantiationOfUtilityClass")
public class Transformation {

    // currently there is no transformation present at all

    public static Transformation from(final @NotNull TransformationEntity transformation) {
        return new Transformation();
    }

    public static Transformation fromModel(final @NotNull JsonNode transformation) {
        return new Transformation();
    }
}
