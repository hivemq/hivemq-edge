package com.hivemq.configuration.entity.adapter;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.persistence.fieldmapping.Transformation;

public class TransformationEntity {

    // default constructor needed for JaxB
    TransformationEntity(){}

    @SuppressWarnings("InstantiationOfUtilityClass")
    public static TransformationEntity from(final @NotNull Transformation transformation) {
        return new TransformationEntity();
    }
}
