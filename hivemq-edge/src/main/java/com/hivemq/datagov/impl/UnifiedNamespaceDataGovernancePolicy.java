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

import com.google.common.collect.ImmutableMap;
import com.hivemq.client.mqtt.datatypes.MqttTopic;
import com.hivemq.common.topic.TopicFilterProcessor;
import com.hivemq.datagov.DataGovernanceContext;
import com.hivemq.datagov.model.DataGovernanceData;
import com.hivemq.datagov.model.DataGovernancePolicy;
import com.hivemq.datagov.model.impl.DataGovernancePolicyImpl;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.uns.UnifiedNamespaceService;
import com.hivemq.uns.config.ISA95;

import javax.inject.Inject;
import java.util.Map;

/**
 * @author Simon L Johnson
 */
public class UnifiedNamespaceDataGovernancePolicy extends DataGovernancePolicyImpl implements DataGovernancePolicy {

    static final String ID = "unified.namespace.policy";
    static final String NAME = "Unified Namespace Policy";

    private final UnifiedNamespaceService unifiedNamespaceService;

    @Inject
    public UnifiedNamespaceDataGovernancePolicy(
            final UnifiedNamespaceService unifiedNamespaceService) {
        super(ID, NAME);
        this.unifiedNamespaceService = unifiedNamespaceService;
    }

    public void execute(final DataGovernanceContext context, final DataGovernanceData input){

        ImmutableMap.Builder builder = ImmutableMap.<String, String>builder();
        Map<String, String> tokens = context.getTokenProvider().getTokenReplacements(context);
        if(tokens != null){
            builder.putAll(tokens);
        }

        MqttTopic mqttTopic = MqttTopic.of(input.getPublish().getTopic());
        //-- Topic modifications
        ISA95 isa95 = unifiedNamespaceService.getISA95();
        if(isa95.isEnabled()){
            builder.putAll(unifiedNamespaceService.getTopicReplacements(isa95));
            if(isa95.isPrefixAllTopics()){
                //-- Add a topic prefix regardless of the templates being used
                mqttTopic = unifiedNamespaceService.prefixISA95(mqttTopic);
            }
        }

        //-- Apply topic transformations from the context
        mqttTopic = TopicFilterProcessor.applyDestinationModifier(mqttTopic,
                mqttTopic.toString(),
                builder.build());

        //-- Update the Resulting Object If Aspects Have Changed
        if(!MqttTopic.of(input.getPublish().getTopic()).equals(mqttTopic)){
            context.getResult().getOutput().setPublish(new PUBLISHFactory.Mqtt5Builder().fromPublish(
                    context.getResult().getOutput().getPublish()).withTopic(mqttTopic.toString()).build());
        }
    }
}
