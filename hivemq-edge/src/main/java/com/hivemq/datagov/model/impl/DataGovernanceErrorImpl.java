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
package com.hivemq.datagov.model.impl;

import com.hivemq.datagov.model.DataGovernanceError;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * @author Simon L Johnson
 */
public class DataGovernanceErrorImpl implements DataGovernanceError {

    private Optional<Throwable> error;
    private Optional<String> pipelineId;
    private Optional<String> functionId;
    private Optional<String> validatorId;
    private Optional<String> message;

    public DataGovernanceErrorImpl(final @NotNull Throwable error) {
        this.error = Optional.of(error);
        this.message = Optional.ofNullable(error.getMessage());
    }

    public DataGovernanceErrorImpl(final @NotNull String message) {
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

    public static DataGovernanceError ofPipelineAndFunction(final @NotNull Throwable t,
                                                            final @NotNull String pipelineId,
                                                            final @NotNull String functionId){
        DataGovernanceErrorImpl impl = new DataGovernanceErrorImpl(t);
        impl.setFunctionId(functionId);
        impl.setPipelineId(pipelineId);
        return impl;
    }

    public static DataGovernanceError ofPipeline(final @NotNull Throwable t, final @NotNull String pipelineId){
        DataGovernanceErrorImpl impl = new DataGovernanceErrorImpl(t);
        impl.setPipelineId(pipelineId);
        return impl;
    }

    public static DataGovernanceError ofValidator(final @NotNull String message, final @NotNull String validatorId){
        DataGovernanceErrorImpl impl = new DataGovernanceErrorImpl(message);
        impl.setValidatorId(validatorId);
        return impl;
    }
}
