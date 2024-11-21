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
package com.hivemq.edge.adapters.opcua.client;

import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.SecPolicy;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.eclipse.milo.opcua.stack.core.util.EndpointUtil;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class OpcUaEndpointFilter implements Function<List<EndpointDescription>, Optional<EndpointDescription>> {
    private static final Logger log = LoggerFactory.getLogger(OpcUaEndpointFilter.class);

    private final @NotNull String adapterId;
    private final @NotNull String configPolicyUri;
    private final @NotNull OpcUaSpecificAdapterConfig adapterConfig;

    public OpcUaEndpointFilter(
            final @NotNull String adapterId,
            @NotNull String configPolicyUri,
            @NotNull OpcUaSpecificAdapterConfig adapterConfig) {
        this.adapterId = adapterId;
        this.configPolicyUri = configPolicyUri;
        this.adapterConfig = adapterConfig;
    }

    @Override
    public @NotNull Optional<EndpointDescription> apply(final List<EndpointDescription> endpointDescriptions) {
        return endpointDescriptions.stream().filter(endpointDescription -> {
            final String policyUri = endpointDescription.getSecurityPolicyUri();
            if (!configPolicyUri.equals(policyUri)) {
                return false;
            }
            if (policyUri.equals(SecurityPolicy.None.getUri())) {
                return true;
            }
            if (isKeystoreAvailable()) {
                //if security policy is not 'None', then skip the policy if no keystore is available
                return true;
            }
            log.warn("OPC UA Security policy '{}' for protocol adapter '{}' requires a keystore, cannot connect.",
                    policyUri,
                    adapterId);
            return false;
        }).min((o1, o2) -> {
            final SecPolicy policy1 = SecPolicy.forUri(o1.getSecurityPolicyUri());
            final SecPolicy policy2 = SecPolicy.forUri(o2.getSecurityPolicyUri());
            if (policy1 == null) {
                return -1;
            }
            if (policy2 == null) {
                return 1;
            }
            return -1 * Integer.compare(policy1.getPriority(), policy2.getPriority());
        }).map(endpointDescription -> endpointUpdater(endpointDescription));
    }

    private EndpointDescription endpointUpdater(EndpointDescription endpoint) {
        if (adapterConfig.getOverrideUri()) {
            final EndpointDescription endpointDescription = EndpointUtil.updateUrl(endpoint,
                    EndpointUtil.getHost(adapterConfig.getUri()),
                    EndpointUtil.getPort(adapterConfig.getUri()));
            log.info("Overwriting returned endpoint {} with {}", endpoint, endpointDescription);
            return endpointDescription;
        }
        return endpoint;
    }

    private boolean isKeystoreAvailable() {
        return adapterConfig.getTls() != null &&
                adapterConfig.getTls().isEnabled() &&
                adapterConfig.getTls().getKeystore() != null &&
                adapterConfig.getTls().getKeystore().getPath() != null;
    }
}
