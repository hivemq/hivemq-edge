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
package com.hivemq.extensions.packets.pubrel;

import com.hivemq.configuration.service.ConfigurationService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.pubrel.ModifiablePubrelPacket;
import com.hivemq.extension.sdk.api.packets.pubrel.PubrelReasonCode;
import com.hivemq.extensions.packets.general.ModifiableUserPropertiesImpl;
import com.hivemq.extensions.services.builder.PluginBuilderUtil;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Yannick Weber
 * @author Silvio Giebl
 */
public class ModifiablePubrelPacketImpl implements ModifiablePubrelPacket {

    private final int packetIdentifier;
    private final @NotNull PubrelReasonCode reasonCode;
    private @Nullable String reasonString;
    private final @NotNull ModifiableUserPropertiesImpl userProperties;

    private final @NotNull ConfigurationService configurationService;
    private boolean modified = false;

    public ModifiablePubrelPacketImpl(
            final @NotNull PubrelPacketImpl packet,
            final @NotNull ConfigurationService configurationService) {

        packetIdentifier = packet.packetIdentifier;
        reasonCode = packet.reasonCode;
        reasonString = packet.reasonString;
        userProperties = new ModifiableUserPropertiesImpl(
                packet.userProperties.asInternalList(), configurationService.securityConfiguration().validateUTF8());

        this.configurationService = configurationService;
    }

    @Override
    public int getPacketIdentifier() {
        return packetIdentifier;
    }

    @Override
    public @NotNull PubrelReasonCode getReasonCode() {
        return reasonCode;
    }

    @Override
    public @NotNull Optional<String> getReasonString() {
        return Optional.ofNullable(reasonString);
    }

    @Override
    public void setReasonString(final @Nullable String reasonString) {
        PluginBuilderUtil.checkReasonString(reasonString, configurationService.securityConfiguration().validateUTF8());
        if (Objects.equals(this.reasonString, reasonString)) {
            return;
        }
        this.reasonString = reasonString;
        modified = true;
    }

    @Override
    public @NotNull ModifiableUserPropertiesImpl getUserProperties() {
        return userProperties;
    }

    public boolean isModified() {
        return modified || userProperties.isModified();
    }

    public @NotNull PubrelPacketImpl copy() {
        return new PubrelPacketImpl(packetIdentifier, reasonCode, reasonString, userProperties.copy());
    }

    public @NotNull ModifiablePubrelPacketImpl update(final @NotNull PubrelPacketImpl packet) {
        return new ModifiablePubrelPacketImpl(packet, configurationService);
    }
}
