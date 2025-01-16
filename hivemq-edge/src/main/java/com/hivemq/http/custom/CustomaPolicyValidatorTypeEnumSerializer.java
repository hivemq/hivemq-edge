package com.hivemq.http.custom;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.hivemq.edge.api.model.DataPolicyValidator;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class CustomaPolicyValidatorTypeEnumSerializer extends StdDeserializer<DataPolicyValidator.TypeEnum> {

    public CustomaPolicyValidatorTypeEnumSerializer() {
        this(null);
    }

    public CustomaPolicyValidatorTypeEnumSerializer(final @NotNull Class<?> vc) {
        super(vc);
    }

    @Override
    public DataPolicyValidator.@NotNull TypeEnum deserialize(
            final @NotNull JsonParser jp, final @NotNull DeserializationContext ctxt)
            throws IOException {
        final JsonNode node = jp.getCodec().readTree(jp);
        return DataPolicyValidator.TypeEnum.fromString(node.asText().toUpperCase());
    }


}
