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
package com.hivemq.edge.adapters.http.v2;

import com.hivemq.adapter.sdk.api.ProtocolAdapterCategory;
import com.hivemq.adapter.sdk.api.ProtocolAdapterTag;
import com.hivemq.adapter.sdk.api.v2.ProtocolAdapterCapability;
import com.hivemq.adapter.sdk.api.v2.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import java.util.EnumSet;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * The {@link ProtocolAdapterInformation} of the v2 HTTP adapter type. It reports the {@code http-v2} protocol id, the
 * {@link HttpNode} node class, and an empty capability set — the adapter is northbound poll-only, with no write,
 * browse, or subscription support — and a {@code config-version} of {@code 2}, marking it a v2 type.
 */
public final class HttpProtocolAdapterInformation implements ProtocolAdapterInformation {

    /**
     * The single shared instance of the HTTP adapter type information.
     */
    public static final @NotNull ProtocolAdapterInformation INSTANCE = new HttpProtocolAdapterInformation();

    /**
     * The {@code protocol-id} the v2 HTTP adapter is registered under, distinct from the v1 {@code http}.
     */
    public static final @NotNull String PROTOCOL_ID = "http-v2";

    private HttpProtocolAdapterInformation() {}

    @Override
    public @NotNull String protocolId() {
        return PROTOCOL_ID;
    }

    @Override
    public @NotNull String displayName() {
        return "HTTP(s) to MQTT (v2)";
    }

    @Override
    public @NotNull String description() {
        return "Connects HiveMQ Edge to arbitrary web endpoint URLs via HTTP(s), consuming structured JSON or plain"
                + " data.";
    }

    @Override
    public @NotNull String version() {
        return "1.0.0";
    }

    @Override
    public @NotNull String logoUrl() {
        return "/images/http-icon.png";
    }

    @Override
    public @NotNull String author() {
        return "HiveMQ";
    }

    @Override
    public @NotNull ProtocolAdapterCategory category() {
        return ProtocolAdapterCategory.CONNECTIVITY;
    }

    @Override
    public @NotNull List<ProtocolAdapterTag> tags() {
        return List.of(ProtocolAdapterTag.INTERNET, ProtocolAdapterTag.TCP, ProtocolAdapterTag.WEB);
    }

    @Override
    public @NotNull EnumSet<ProtocolAdapterCapability> capabilities() {
        return EnumSet.noneOf(ProtocolAdapterCapability.class);
    }

    @Override
    public @NotNull Class<? extends Node> nodeClass() {
        return HttpNode.class;
    }

    @Override
    public int currentConfigVersion() {
        return 2;
    }
}
