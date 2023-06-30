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
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
@XmlRootElement(name = "tls")
@XmlAccessorType(XmlAccessType.NONE)
public class BridgeTlsEntity {

    @XmlElement(name = "enabled")
    private boolean enabled = false;

    @XmlElementRef(required = false)
    private @Nullable KeystoreEntity keyStore;

    @XmlElementRef(required = false)
    private @Nullable TruststoreEntity trustStore;

    @XmlElementWrapper(name = "protocols")
    @XmlElement(name = "protocol")
    private @NotNull List<String> protocols = new ArrayList<>();

    @XmlElementWrapper(name = "cipher-suites")
    @XmlElement(name = "cipher-suite")
    private @NotNull List<String> cipherSuites = new ArrayList<>();

    @XmlElement(name = "handshake-timeout", defaultValue = "10")
    private int handshakeTimeout = 10;

    @XmlElement(name = "verify-hostname", defaultValue = "true")
    private boolean verifyHostname = true;

    public boolean isEnabled() {
        return enabled;
    }

    public @Nullable KeystoreEntity getKeyStore() {
        return keyStore;
    }

    public @Nullable TruststoreEntity getTrustStore() {
        return trustStore;
    }

    public @NotNull List<String> getCipherSuites() {
        return cipherSuites;
    }

    public @NotNull List<String> getProtocols() {
        return protocols;
    }

    public int getHandshakeTimeout() {
        return handshakeTimeout;
    }

    public boolean isVerifyHostname() {
        return verifyHostname;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public void setKeyStore(final KeystoreEntity keyStore) {
        this.keyStore = keyStore;
    }

    public void setTrustStore(final TruststoreEntity trustStore) {
        this.trustStore = trustStore;
    }

    public void setProtocols(final List<String> protocols) {
        this.protocols = protocols;
    }

    public void setCipherSuites(final List<String> cipherSuites) {
        this.cipherSuites = cipherSuites;
    }

    public void setHandshakeTimeout(final int handshakeTimeout) {
        this.handshakeTimeout = handshakeTimeout;
    }

    public void setVerifyHostname(final boolean verifyHostname) {
        this.verifyHostname = verifyHostname;
    }
}
