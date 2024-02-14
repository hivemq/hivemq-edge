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
package com.hivemq.uns;

import com.hivemq.client.mqtt.datatypes.MqttTopic;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.uns.config.ISA95;
import com.hivemq.uns.config.NamespaceProfile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Simon L Johnson
 */
public interface UnifiedNamespaceService {

    ISA95 getISA95();

    void setISA95(ISA95 isa95);

    Map<String, String> getTopicReplacements(@NotNull ISA95 isa95);

    MqttTopic prefixISA95(MqttTopic topic);

    List<NamespaceProfile> getAvailableProfiles();

    List<NamespaceProfile> getConfiguredProfiles();

    void setConfiguredProfiles(List<NamespaceProfile> profiles);

    Optional<NamespaceProfile> getConfiguredProfileByType(String type) ;

    Optional<NamespaceProfile> getActiveProfile();

}
