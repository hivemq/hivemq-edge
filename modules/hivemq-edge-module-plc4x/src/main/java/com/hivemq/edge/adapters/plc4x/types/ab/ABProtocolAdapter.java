/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.plc4x.types.ab;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hivemq.edge.adapters.plc4x.Plc4xException;
import com.hivemq.edge.adapters.plc4x.impl.AbstractPlc4xAdapter;
import com.hivemq.edge.adapters.plc4x.impl.Plc4xConnection;
import com.hivemq.edge.adapters.plc4x.impl.Plc4xDataUtils;
import com.hivemq.edge.adapters.plc4x.model.Plc4xAdapterConfig;
import com.hivemq.edge.adapters.plc4x.model.Plc4xDataType;
import com.hivemq.edge.modules.adapters.annotations.ModuleConfigField;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterInformation;
import com.hivemq.edge.modules.config.impl.AbstractProtocolAdapterConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author HiveMQ Adapter Generator
 */
public class ABProtocolAdapter extends AbstractPlc4xAdapter<ABAdapterConfig> {

    public ABProtocolAdapter(
            final ProtocolAdapterInformation adapterInformation,
            final ABAdapterConfig adapterConfig,
            final MetricRegistry metricRegistry) {
        super(adapterInformation, adapterConfig, metricRegistry);
    }

    @Override
    protected String getProtocolHandler() {
        return "ab-eth";
    }

    @Override
    protected ReadType getReadType() {
        return ReadType.Read;
    }


    protected Plc4xConnection<?> createConnection() throws Plc4xException {
        return new Plc4xConnection<>(driverManager,
                adapterConfig,
                plc4xAdapterConfig -> Plc4xDataUtils.createQueryString(createQueryStringParams(
                        plc4xAdapterConfig), true)) {
            @Override
            protected String getProtocol() {
                return getProtocolHandler();
            }

            @Override
            protected String getTagAddressForSubscription(final Plc4xAdapterConfig.Subscription subscription) {
                return createTagAddressForSubscription(subscription);
            }

//            @Override
//            protected String createConnectionString(final @NotNull ABAdapterConfig config){
//                String queryString = connectionQueryStringProvider.getConnectionQueryString(config);
//                if(queryString != null && !queryString.trim().equals("")){
//                    return String.format("%s://%s:%s/%s?%s",
//                            getProtocol().trim(),
//                            config.getHost().trim(),
//                            config.getPort(),
//                            config.getStation().trim(),
//                            queryString.trim());
//                } else {
//                    return String.format("%s://%s:%s/%s",
//                            getProtocol().trim(),
//                            config.getHost().trim(),
//                            config.getPort(),
//                            config.getStation().trim());
//                }
//            }
        };
    }

        /*
    The connection string looks as follows: ab-eth://<ip-address>/<station>
    The field address: N<file>:<offset></bitnumber>:<datatype>[<numberofbytes>]. The following data types are available at the moment: SINBLEBIT (requires bitnumber to be set), WORD (2 byte integer), DWORD (4 byte integer), INTEGER (returns the number of bytes requested as an array, for other data types <numberofbytes> will be ignored).
    Example of a SINGLEBIT read: N10:22/5:SINGLEBIT (file 10, offset 22, bitnumber 5)
    Example of a WORD read: N10:84:WORD (file 10, offset 84, 2 byte integer)
    */
}
