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

import com.hivemq.extension.sdk.api.annotations.Nullable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
@XmlRootElement(name = "mqtt")
@XmlAccessorType(XmlAccessType.NONE)
public class BridgeMqttEntity {

    @XmlElement(name = "client-id", required = false)
    private @Nullable String clientId = null;

    @XmlElement(name = "clean-start", defaultValue = "false")
    private boolean cleanStart = false;

    @XmlElement(name = "session-expiry", defaultValue = "3600")
    private int sessionExpiry = 3600;

    @XmlElement(name = "keep-alive", defaultValue = "60")
    private int keepAlive = 60;

    public @Nullable String getClientId() {
        return clientId;
    }

    public boolean isCleanStart() {
        return cleanStart;
    }

    public int getSessionExpiry() {
        return sessionExpiry;
    }

    public int getKeepAlive() {
        return keepAlive;
    }

    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }

    public void setCleanStart(final boolean cleanStart) {
        this.cleanStart = cleanStart;
    }

    public void setSessionExpiry(final int sessionExpiry) {
        this.sessionExpiry = sessionExpiry;
    }

    public void setKeepAlive(final int keepAlive) {
        this.keepAlive = keepAlive;
    }
}
