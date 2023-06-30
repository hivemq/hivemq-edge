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

import com.google.common.base.Preconditions;
import com.hivemq.datagov.model.DataGovernanceData;
import com.hivemq.datagov.model.DataGovernanceError;
import com.hivemq.datagov.model.DataGovernanceResult;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author Simon L Johnson
 */
public class DataGoveranceResultImpl implements DataGovernanceResult {

    private List<DataGovernanceError> errors;
    private String message;
    private STATUS status;
    private DataGovernanceData output;

    public DataGoveranceResultImpl(final @NotNull DataGovernanceData output) {
        this.status = STATUS.PENDING;
        this.output = output;
    }

    @Override
    public List<DataGovernanceError> getErrors() {
        return errors == null ? Collections.emptyList() : errors;
    }

    public void addError(final DataGovernanceError error, boolean fatal) {
        Preconditions.checkNotNull(error);
        if(fatal){
            status = STATUS.FAILURE;
        }
        if(errors == null){
            errors = new ArrayList<>();
        }
        errors.add(error);
    }

    @Override
    public Optional<String> getMessage() {
        return Optional.ofNullable(message);
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    @Override
    public STATUS getStatus() {
        return status;
    }

    public void setStatus(final STATUS status) {
        this.status = status;
    }

    @Override
    public DataGovernanceData getOutput() {
        return output;
    }

    @Override
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
