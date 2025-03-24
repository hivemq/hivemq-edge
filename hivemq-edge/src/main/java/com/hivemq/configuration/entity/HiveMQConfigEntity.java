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

import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.entity.api.AdminApiEntity;
import com.hivemq.configuration.entity.bridge.MqttBridgeEntity;
import com.hivemq.configuration.entity.combining.DataCombinerEntity;
import com.hivemq.configuration.entity.listener.ListenerEntity;
import com.hivemq.configuration.entity.uns.UnsConfigEntity;
import com.hivemq.configuration.reader.ArbitraryValuesMapAdapter;
import org.jetbrains.annotations.NotNull;

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
import java.util.Objects;

/**
 * @author Dominik Obermaier
 * @author Lukas brandl
 */
@XmlRootElement(name = "hivemq")
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class HiveMQConfigEntity {

    public static final int CURRENT_CONFIG_VERSION = 1;

    @XmlElement(name = "config-version", defaultValue = "" + CURRENT_CONFIG_VERSION)
    private int version = CURRENT_CONFIG_VERSION;

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

    @XmlElementWrapper(name = "protocol-adapters")
    @XmlElement(name = "protocol-adapter")
    private @NotNull List<ProtocolAdapterEntity> protocolAdapterConfig = new ArrayList<>();

    @XmlElementWrapper(name = "data-combiners")
    @XmlElement(name = "data-combiner")
    private @NotNull List<DataCombinerEntity> dataCombinerEntities = new ArrayList<>();

    @XmlElement(name = "modules")
    @XmlJavaTypeAdapter(ArbitraryValuesMapAdapter.class)
    private @NotNull Map<String, Object> moduleConfigs = new HashMap<>();

    @XmlElementRef(required = false)
    private final @NotNull InternalConfigEntity internal = new InternalConfigEntity();

    // no-arg constructor as JaxB does need one
    public HiveMQConfigEntity() {

    }

    public HiveMQConfigEntity(
            final @NotNull Integer version,
            final @NotNull AdminApiEntity api,
            final @NotNull DynamicConfigEntity gateway,
            final @NotNull Map<String, Object> moduleConfigs,
            final @NotNull MqttConfigEntity mqtt,
            final @NotNull List<MqttBridgeEntity> mqttBridges,
            final @NotNull List<ListenerEntity> mqttListeners,
            final @NotNull MqttSnConfigEntity mqttsn,
            final @NotNull List<ListenerEntity> mqttsnListeners,
            final @NotNull PersistenceEntity persistence,
            final @NotNull List<ProtocolAdapterEntity> protocolAdapterConfig,
            final @NotNull RestrictionsEntity restrictions,
            final @NotNull SecurityConfigEntity security,
            final @NotNull UnsConfigEntity uns,
            final @NotNull UsageTrackingConfigEntity usageTracking,
            final @NotNull List<DataCombinerEntity> dataCombinerEntities) {
        this.version = Objects.requireNonNullElse(version, CURRENT_CONFIG_VERSION);
        this.api = api;
        this.gateway = gateway;
        this.moduleConfigs = moduleConfigs;
        this.mqtt = mqtt;
        this.mqttBridges = mqttBridges;
        this.mqttListeners = mqttListeners;
        this.mqttsn = mqttsn;
        this.mqttsnListeners = mqttsnListeners;
        this.persistence = persistence;
        this.protocolAdapterConfig = protocolAdapterConfig;
        this.restrictions = restrictions;
        this.security = security;
        this.uns = uns;
        this.usageTracking = usageTracking;
        this.dataCombinerEntities = dataCombinerEntities;
    }

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

    public @NotNull List<ProtocolAdapterEntity> getProtocolAdapterConfig() {
        return protocolAdapterConfig;
    }

    public @NotNull Map<String, Object> getModuleConfigs() {
        return moduleConfigs;
    }

    public @NotNull UnsConfigEntity getUns() {
        return uns;
    }

    public @NotNull DynamicConfigEntity getGatewayConfig() {
        return gateway;
    }

    public @NotNull UsageTrackingConfigEntity getUsageTracking() {
        return usageTracking;
    }

    public @NotNull InternalConfigEntity getInternal() {
        return internal;
    }

    public @NotNull List<DataCombinerEntity> getDataCombinerEntities() {
        return dataCombinerEntities;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final HiveMQConfigEntity that = (HiveMQConfigEntity) o;
        return getVersion() == that.getVersion() &&
                Objects.equals(mqttListeners, that.mqttListeners) &&
                Objects.equals(mqttsnListeners, that.mqttsnListeners) &&
                Objects.equals(mqtt, that.mqtt) &&
                Objects.equals(mqttsn, that.mqttsn) &&
                Objects.equals(restrictions, that.restrictions) &&
                Objects.equals(security, that.security) &&
                Objects.equals(persistence, that.persistence) &&
                Objects.equals(mqttBridges, that.mqttBridges) &&
                Objects.equals(api, that.api) &&
                Objects.equals(getUns(), that.getUns()) &&
                Objects.equals(gateway, that.gateway) &&
                Objects.equals(getUsageTracking(), that.getUsageTracking()) &&
                Objects.equals(getProtocolAdapterConfig(), that.getProtocolAdapterConfig()) &&
                Objects.equals(getDataCombinerEntities(), that.getDataCombinerEntities()) &&
                Objects.equals(getModuleConfigs(), that.getModuleConfigs()) &&
                Objects.equals(getInternal(), that.getInternal());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getVersion(),
                mqttListeners,
                mqttsnListeners,
                mqtt,
                mqttsn,
                restrictions,
                security,
                persistence,
                mqttBridges,
                api,
                getUns(),
                gateway,
                getUsageTracking(),
                getProtocolAdapterConfig(),
                getDataCombinerEntities(),
                getModuleConfigs(),
                getInternal());
    }
}
