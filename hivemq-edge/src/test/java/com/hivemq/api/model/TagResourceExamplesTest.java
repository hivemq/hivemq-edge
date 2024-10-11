package com.hivemq.api.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.api.model.tags.DomainTagModelList;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.http.error.Errors;
import org.junit.jupiter.api.Test;

class TagResourceExamplesTest {


    private final @NotNull ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void assertThatExamplesAreParsable() throws JsonProcessingException {
        objectMapper.readValue(TagResourceExamples.EXAMPLE_LIST, DomainTagModelList.class);
        objectMapper.readValue(TagResourceExamples.EXAMPLE_OPC_UA, DomainTagModelList.class);
        objectMapper.readValue(TagResourceExamples.EXAMPLE_ALREADY_PRESENT, Errors.class);
        objectMapper.readValue(TagResourceExamples.EXAMPLE_NOT_PRESENT, Errors.class);
    }
}
