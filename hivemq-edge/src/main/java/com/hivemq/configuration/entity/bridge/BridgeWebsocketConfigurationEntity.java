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

import com.hivemq.configuration.entity.listener.tls.KeystoreEntity;
import com.hivemq.configuration.entity.listener.tls.TruststoreEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
@XmlRootElement(name = "websocket")
@XmlAccessorType(XmlAccessType.NONE)
public class BridgeWebsocketConfigurationEntity {

    @XmlElement(name = "enabled")
    private boolean enabled = false;

    @XmlElement(name = "server-path")
    private @NotNull String serverPath = "/mqtt";

    @XmlElement(name = "subprotocol")
    private @NotNull String subProtocol = "mqtt";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public @NotNull String getServerPath() {
        return serverPath;
    }

    public void setServerPath(final @NotNull String serverPath) {
        this.serverPath = serverPath;
    }

    public @NotNull String getSubProtocol() {
        return subProtocol;
    }

    public void setSubProtocol(final @NotNull String subProtocol) {
        this.subProtocol = subProtocol;
    }
}
