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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import javax.xml.bind.annotation.*;

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
}
