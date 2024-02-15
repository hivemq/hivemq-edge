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
import com.hivemq.uns.config.ISA95;
import com.hivemq.uns.config.NamespaceProfile;

import java.util.List;
import java.util.Optional;

/**
 * @author Simon L Johnson
 */
public class UnsConfigurationServiceImpl implements UnsConfigurationService {

    private @Nullable ISA95 isa95;
    private @Nullable List<NamespaceProfile> profiles;
    @Override
    public ISA95 getISA95() {
        return isa95;
    }

    public void setISA95(final ISA95 isa95) {
        this.isa95 = isa95;
    }

    public List<NamespaceProfile> getProfiles() {
        return profiles;
    }

    @Override
    public void setProfiles(final List<NamespaceProfile> profiles) {
        this.profiles = profiles;
    }

}
