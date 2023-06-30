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
package com.hivemq.datagov.model;

import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * @author Simon L Johnson
 */
public interface DataGovernanceResult {

    enum STATUS {
        SUCCESS(false),
        FAILURE(true),
        PENDING(false);

        private final boolean error;
        STATUS(final boolean error){
            this.error = error;
        }

        public boolean isError(){
            return error;
        }
        public boolean isSuccess(){
            return this == SUCCESS;
        }
    }

    @NotNull STATUS getStatus();
    @NotNull List<DataGovernanceError> getErrors();

    Optional<String> getMessage();
    DataGovernanceData getOutput();

    void setStatus(@NotNull STATUS status);
    void addError(@NotNull DataGovernanceError error, boolean fatal);
    void setMessage(@NotNull String message);
    boolean hasErrors();

}
