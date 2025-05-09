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
package com.hivemq.configuration.entity.bridge;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jakarta.xml.bind.annotation.*;
import java.util.Objects;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
@XmlRootElement(name = "remote-broker")
@XmlAccessorType(XmlAccessType.NONE)
public class RemoteBrokerEntity {

    @XmlElement(name = "port", defaultValue = "1883")
    private int port = 1883;

    @XmlElement(name = "host", required = true)
    private @NotNull String host = "";

    @XmlElementRef
    private @NotNull BridgeMqttEntity mqtt = new BridgeMqttEntity();

    @XmlElementRef
    private @Nullable BridgeAuthenticationEntity authentication;

    @XmlElementRef
    private @Nullable BridgeWebsocketConfigurationEntity bridgeWebsocketConfig;

    @XmlElementRef
    private @Nullable BridgeTlsEntity tls;

    public int getPort() {
        return port;
    }

    public @NotNull String getHost() {
        return host;
    }

    public @NotNull BridgeMqttEntity getMqtt() {
        return mqtt;
    }

    public @Nullable BridgeAuthenticationEntity getAuthentication() {
        return authentication;
    }

    public @Nullable BridgeWebsocketConfigurationEntity getBridgeWebsocketConfig() {
        return bridgeWebsocketConfig;
    }

    public void setBridgeWebsocketConfig(final @Nullable BridgeWebsocketConfigurationEntity bridgeWebsocketConfig) {
        this.bridgeWebsocketConfig = bridgeWebsocketConfig;
    }

    public @Nullable BridgeTlsEntity getTls() {
        return tls;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    public void setHost(final String host) {
        this.host = host;
    }

    public void setMqtt(final BridgeMqttEntity mqtt) {
        this.mqtt = mqtt;
    }

    public void setAuthentication(final BridgeAuthenticationEntity authentication) {
        this.authentication = authentication;
    }

    public void setTls(final BridgeTlsEntity tls) {
        this.tls = tls;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final RemoteBrokerEntity that = (RemoteBrokerEntity) o;
        return getPort() == that.getPort() &&
                Objects.equals(getHost(), that.getHost()) &&
                Objects.equals(getMqtt(), that.getMqtt()) &&
                Objects.equals(getAuthentication(), that.getAuthentication()) &&
                Objects.equals(getBridgeWebsocketConfig(), that.getBridgeWebsocketConfig()) &&
                Objects.equals(getTls(), that.getTls());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPort(), getHost(), getMqtt(), getAuthentication(), getBridgeWebsocketConfig(), getTls());
    }
}
