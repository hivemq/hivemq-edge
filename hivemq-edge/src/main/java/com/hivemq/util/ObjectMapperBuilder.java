/*
 *  Copyright 2019-present HiveMQ GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.hivemq.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.jetbrains.annotations.NotNull;

public final class ObjectMapperBuilder {
    private boolean indentOutput;
    private boolean javaTimeModule;
    private boolean failOnEmptyBeans;

    private ObjectMapperBuilder() {
        indentOutput = false;
        javaTimeModule = false;
        failOnEmptyBeans = false;
    }

    public static @NotNull ObjectMapperBuilder builder() {
        return new ObjectMapperBuilder();
    }

    public @NotNull ObjectMapperBuilder enableIndentOutput() {
        indentOutput = true;
        return this;
    }

    public @NotNull ObjectMapperBuilder enableJavaTimeModule() {
        javaTimeModule = true;
        return this;
    }

    public @NotNull ObjectMapperBuilder enableFailOnEmptyBeans() {
        failOnEmptyBeans = true;
        return this;
    }

    public @NotNull ObjectMapper build() {
        final ObjectMapper objectMapper = new ObjectMapper();
        if (indentOutput) {
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        } else {
            objectMapper.disable(SerializationFeature.INDENT_OUTPUT);
        }
        if (javaTimeModule) {
            objectMapper.registerModule(new JavaTimeModule());
        }
        if (failOnEmptyBeans) {
            objectMapper.enable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        } else {
            objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        }
        return objectMapper;
    }
}
