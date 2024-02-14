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
package com.hivemq.configuration.service;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.uns.config.ISA95;
import com.hivemq.uns.config.NamespaceProfile;
import com.hivemq.uns.config.NamespaceSegment;

import java.util.List;
import java.util.Optional;

/**
 * A Configuration service which allows access to API Configuration properties
 */
public interface UnsConfigurationService {

    @NotNull ISA95 getISA95();

    void setISA95(@NotNull ISA95 isa95);

    void setProfiles(List<NamespaceProfile> profiles);

    List<NamespaceProfile> getProfiles();

    Optional<NamespaceProfile> getActiveProfile();

}
