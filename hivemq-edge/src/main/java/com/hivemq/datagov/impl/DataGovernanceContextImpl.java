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
package com.hivemq.datagov.impl;

import com.hivemq.datagov.DataGovernanceContext;
import com.hivemq.datagov.model.DataGovernanceData;
import com.hivemq.datagov.model.DataGovernanceResult;
import com.hivemq.datagov.provider.DataGovernanceTokenProvider;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * @author Simon L Johnson
 */
public class DataGovernanceContextImpl implements DataGovernanceContext {

    private final @NotNull Map<String, String> tokenReplacements;
    private final @NotNull DataGovernanceData input;
    private @NotNull DataGovernanceResult result;
    private @NotNull ExecutorService executorService;

    public DataGovernanceContextImpl(final @NotNull DataGovernanceData input) {
        this.input = input;
        this.tokenReplacements = Collections.emptyMap();
    }

    public DataGovernanceContextImpl(final @NotNull DataGovernanceData input,
                                     final @NotNull Map<String, String> tokenReplacements) {
        this.input = input;
        this.tokenReplacements = tokenReplacements;
    }

    @Override
    public @NotNull DataGovernanceData getInput() {
        return input;
    }

    @Override
    public @Nullable DataGovernanceResult getResult() {
        return result;
    }

    @Override
    public void setResult(final @NotNull DataGovernanceResult result) {
        this.result = result;
    }

    @Override
    public DataGovernanceTokenProvider getTokenProvider() {
        return context -> tokenReplacements;
    }

    @Override
    public ExecutorService getExecutorService() {
        return executorService;
    }

    @Override
    public void setExecutorService(final ExecutorService executorService) {
        this.executorService = executorService;
    }
}
