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
package com.hivemq.edge.adapters.file.v2;

import com.hivemq.adapter.sdk.api.ProtocolAdapterCategory;
import com.hivemq.adapter.sdk.api.ProtocolAdapterTag;
import com.hivemq.adapter.sdk.api.v2.ProtocolAdapterCapability;
import com.hivemq.adapter.sdk.api.v2.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import java.util.EnumSet;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * The {@link ProtocolAdapterInformation} of the v2 File adapter type. It reports the {@code file-v2} protocol id, the
 * {@link FileNode} node class, and an empty capability set — the adapter is northbound poll-only, with no write,
 * browse, or subscription support — and a {@code config-version} of {@code 2}, marking it a v2 type.
 */
public final class FileProtocolAdapterInformation implements ProtocolAdapterInformation {

    /**
     * The single shared instance of the File adapter type information.
     */
    public static final @NotNull ProtocolAdapterInformation INSTANCE = new FileProtocolAdapterInformation();

    /**
     * The {@code protocol-id} the v2 File adapter is registered under, distinct from the v1 {@code file}.
     */
    public static final @NotNull String PROTOCOL_ID = "file-v2";

    private FileProtocolAdapterInformation() {}

    @Override
    public @NotNull String protocolId() {
        return PROTOCOL_ID;
    }

    @Override
    public @NotNull String displayName() {
        return "File Adapter (v2)";
    }

    @Override
    public @NotNull String description() {
        return "This adapter polls and publishes the content of files on regular basis.";
    }

    @Override
    public @NotNull String version() {
        return "1.0.0";
    }

    @Override
    public @NotNull String logoUrl() {
        return "/images/file.png";
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
        return List.of(ProtocolAdapterTag.IOT, ProtocolAdapterTag.IIOT);
    }

    @Override
    public @NotNull EnumSet<ProtocolAdapterCapability> capabilities() {
        return EnumSet.noneOf(ProtocolAdapterCapability.class);
    }

    @Override
    public @NotNull Class<? extends Node> nodeClass() {
        return FileNode.class;
    }

    @Override
    public int currentConfigVersion() {
        return 2;
    }
}
