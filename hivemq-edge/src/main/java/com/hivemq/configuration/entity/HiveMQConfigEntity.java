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
package com.hivemq.configuration.entity;

import com.hivemq.configuration.entity.api.AdminApiEntity;
import com.hivemq.configuration.entity.bridge.MqttBridgeEntity;
import com.hivemq.configuration.entity.listener.ListenerEntity;
import com.hivemq.configuration.entity.uns.UnsConfigEntity;
import com.hivemq.configuration.reader.ArbitraryValuesMapAdapter;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Dominik Obermaier
 * @author Lukas brandl
 */
@XmlRootElement(name = "hivemq")
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class HiveMQConfigEntity {

    @XmlElementWrapper(name = "mqtt-listeners", required = true)
    @XmlElementRef(required = false)
    private @NotNull List<ListenerEntity> mqttListeners = new ArrayList<>();

    @XmlElementWrapper(name = "mqtt-sn-listeners", required = true)
    @XmlElementRef(required = false)
    private @NotNull List<ListenerEntity> mqttsnListeners = new ArrayList<>();

    @XmlElementRef(required = false)
    private @NotNull MqttConfigEntity mqtt = new MqttConfigEntity();

    @XmlElementRef(required = false)
    private @NotNull MqttSnConfigEntity mqttsn = new MqttSnConfigEntity();

    @XmlElementRef(required = false)
    private @NotNull RestrictionsEntity restrictions = new RestrictionsEntity();

    @XmlElementRef(required = false)
    private @NotNull SecurityConfigEntity security = new SecurityConfigEntity();

    @XmlElementRef(required = false)
    private @NotNull PersistenceEntity persistence = new PersistenceEntity();

    @XmlElementWrapper(name = "mqtt-bridges", required = true)
    @XmlElementRef(required = false)
    private @NotNull List<MqttBridgeEntity> mqttBridges = new ArrayList<>();

    @XmlElementRef(required = false)
    private @NotNull AdminApiEntity api = new AdminApiEntity();

    @XmlElementRef(required = false)
    private @NotNull UnsConfigEntity uns = new UnsConfigEntity();

    @XmlElementRef(required = false)
    private @NotNull DynamicConfigEntity gateway = new DynamicConfigEntity();

    @XmlElementRef(required = false)
    private @NotNull UsageTrackingConfigEntity usageTracking = new UsageTrackingConfigEntity();

    @XmlElement(name = "protocol-adapters")
    @XmlJavaTypeAdapter(ArbitraryValuesMapAdapter.class)
    private @NotNull Map<String, Object> protocolAdapterConfig = new HashMap<>();

    @XmlElementRef(required = false)
    private final @NotNull InternalConfigEntity internal = new InternalConfigEntity();

    public @NotNull List<ListenerEntity> getMqttListenerConfig() {
        return mqttListeners;
    }

    public @NotNull List<ListenerEntity> getMqttsnListenerConfig() {
        return mqttsnListeners;
    }

    public @NotNull MqttConfigEntity getMqttConfig() {
        return mqtt;
    }

    public @NotNull MqttSnConfigEntity getMqttsnConfig() {
        return mqttsn;
    }

    public @NotNull RestrictionsEntity getRestrictionsConfig() {
        return restrictions;
    }

    public @NotNull SecurityConfigEntity getSecurityConfig() {
        return security;
    }

    public @NotNull PersistenceEntity getPersistenceConfig() {
        return persistence;
    }

    public @NotNull List<MqttBridgeEntity> getBridgeConfig() {
        return mqttBridges;
    }

    public @NotNull AdminApiEntity getApiConfig() {
        return api;
    }

    public @NotNull Map<String, Object> getProtocolAdapterConfig() {
        return protocolAdapterConfig;
    }

    public @NotNull UnsConfigEntity getUns() { return uns; }

    public @NotNull DynamicConfigEntity getGatewayConfig() { return gateway;}

    public @NotNull UsageTrackingConfigEntity getUsageTracking() {
        return usageTracking;
    }

    public @NotNull InternalConfigEntity getInternal() {
        return internal;
    }
}
