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
package com.hivemq.configuration.service.impl;

import com.hivemq.configuration.service.UnsConfigurationService;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.uns.config.ISA88;
import com.hivemq.uns.config.ISA95;

/**
 * @author Simon L Johnson
 */
public class UnsConfigurationServiceImpl implements UnsConfigurationService {

    private @Nullable ISA95 isa95;
    private @Nullable ISA88 isa88;

    @Override
    public ISA95 getISA95() {
        return isa95;
    }

    @Override
    public ISA88 getISA88() {
        return isa88;
    }

    public void setISA95(final ISA95 isa95) {
        this.isa95 = isa95;
    }

    public void setISA88(final ISA88 isa88) {
        this.isa88 = isa88;
    }
}
