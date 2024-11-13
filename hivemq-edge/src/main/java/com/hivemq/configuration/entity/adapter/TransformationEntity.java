package com.hivemq.configuration.entity.adapter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.persistence.fieldmapping.Transformation;

public class TransformationEntity {

    @JsonCreator
    TransformationEntity(){}

    @SuppressWarnings("InstantiationOfUtilityClass")
    public static TransformationEntity from(final @NotNull Transformation transformation) {
        return new TransformationEntity();
    }
}
