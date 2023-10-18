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
package com.hivemq.context.model.impl;

import com.hivemq.context.model.Error;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.Optional;

/**
 * @author Simon L Johnson
 */
public class ErrorImpl implements Error {

    private Optional<Throwable> error;
    private Optional<String> pipelineId;
    private Optional<String> functionId;
    private Optional<String> validatorId;
    private Optional<String> message;

    public ErrorImpl(final @NotNull Throwable error) {
        this.error = Optional.of(error);
        this.message = Optional.ofNullable(error.getMessage());
    }

    public ErrorImpl(final @NotNull String message) {
        this.message = Optional.of(message);
    }

    public @NotNull Optional<Throwable> getError() {
        return error;
    }

    public @NotNull Optional<String> getPipelineId() {
        return pipelineId;
    }

    public void setPipelineId(final @NotNull String pipelineId) {
        this.pipelineId = Optional.ofNullable(pipelineId);
    }

    public @NotNull Optional<String> getFunctionId() {
        return functionId;
    }

    public void setFunctionId(final @NotNull String functionId) {
        this.functionId = Optional.ofNullable(functionId);
    }

    public Optional<String> getValidatorId() {
        return validatorId;
    }

    public void setValidatorId(final @NotNull String validatorId) {
        this.validatorId = Optional.ofNullable(validatorId);
    }

    public @NotNull Optional<String> getMessage() {
        return message;
    }

    public void setMessage(final @NotNull String message) {
        this.message = Optional.ofNullable(message);
    }

    public static Error ofPipelineAndFunction(@NotNull final Throwable t,
                                              @NotNull final String pipelineId,
                                              @NotNull final String functionId){
        ErrorImpl impl = new ErrorImpl(t);
        impl.setFunctionId(functionId);
        impl.setPipelineId(pipelineId);
        return impl;
    }

    public static Error ofPipeline(@NotNull final Throwable t, @NotNull final String pipelineId){
        ErrorImpl impl = new ErrorImpl(t);
        impl.setPipelineId(pipelineId);
        return impl;
    }

    public static Error ofValidator(@NotNull final String message, @NotNull final String validatorId){
        ErrorImpl impl = new ErrorImpl(message);
        impl.setValidatorId(validatorId);
        return impl;
    }
}
