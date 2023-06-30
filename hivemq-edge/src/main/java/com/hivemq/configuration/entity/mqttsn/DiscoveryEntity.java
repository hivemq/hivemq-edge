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
package com.hivemq.configuration.entity.mqttsn;

import com.hivemq.configuration.entity.DisabledEntity;
import com.hivemq.extension.sdk.api.annotations.NotNull;

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
@XmlRootElement(name = "discovery")
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class DiscoveryEntity extends DisabledEntity {

    static final int DEFAULT_BROADCAST_INTERVAL_SECONDS = 30;

    @XmlElementWrapper(name = "broadcast-addresses")
    @XmlElementRef(required = false)
    private @NotNull List<BroadcastAddress> broadcastAddresses = new ArrayList<>();

    @XmlElement(name = "discovery-interval-seconds", defaultValue = "30")
    private int discoveryInterval = DEFAULT_BROADCAST_INTERVAL_SECONDS;

    public int getDiscoveryInterval() {
        return discoveryInterval;
    }

    public List<BroadcastAddress> getBroadcastAddresses() {
        return broadcastAddresses;
    }

}
