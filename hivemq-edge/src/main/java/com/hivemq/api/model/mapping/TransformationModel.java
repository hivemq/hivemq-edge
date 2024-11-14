package com.hivemq.api.model.mapping;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.hivemq.persistence.fieldmapping.Transformation;

public class TransformationModel {

    @JsonCreator
    public TransformationModel() {
    }

    public static TransformationModel from(Transformation transformation) {
        return new TransformationModel();
    }
}
