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
package com.hivemq.configuration.entity.api;

import com.hivemq.configuration.entity.listener.tls.KeystoreEntity;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Simon L Johnson
 */
@XmlRootElement(name = "tls")
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class ApiTlsEntity {

    @XmlElementRef
    private @Nullable KeystoreEntity keystoreEntity;

    @XmlElementWrapper(name = "protocols")
    @XmlElement(name = "protocol")
    private @NotNull List<String> protocols = new ArrayList<>();

    @XmlElementWrapper(name = "cipher-suites")
    @XmlElement(name = "cipher-suite")
    private @NotNull List<String> cipherSuites = new ArrayList<>();

    public @Nullable KeystoreEntity getKeystoreEntity() {
        return keystoreEntity;
    }

    public @NotNull List<String> getProtocols() {
        return protocols;
    }

    public @NotNull List<String> getCipherSuites() {
        return cipherSuites;
    }

}
