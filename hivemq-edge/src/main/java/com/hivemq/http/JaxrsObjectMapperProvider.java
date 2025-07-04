/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.http;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import com.hivemq.edge.api.model.DataPolicyValidator;
import com.hivemq.http.custom.CustomaPolicyValidatorTypeEnumSerializer;
import org.jetbrains.annotations.NotNull;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;

/**
 * Sensible defaults for the JAXRS Object Mapper
 *
 * @author Simon L Johnson
 */
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JaxrsObjectMapperProvider extends JacksonJsonProvider {

    private final @NotNull ObjectMapper mapper;

    public JaxrsObjectMapperProvider() {
        mapper = JsonMapper.builder()
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .visibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY)
                .configure(MapperFeature.AUTO_DETECT_GETTERS, false)
                .build();
        mapper.registerModule(new JavaTimeModule());

        final SimpleModule module = new SimpleModule();
        module.addDeserializer(DataPolicyValidator.TypeEnum.class, new CustomaPolicyValidatorTypeEnumSerializer());
        mapper.registerModule(module);

        setMapper(mapper);
    }

    public JaxrsObjectMapperProvider(final @NotNull ObjectMapper mapper) {
        this.mapper = mapper;
        setMapper(this.mapper);
    }

    public @NotNull ObjectMapper getMapper() {
        return mapper;
    }
}
