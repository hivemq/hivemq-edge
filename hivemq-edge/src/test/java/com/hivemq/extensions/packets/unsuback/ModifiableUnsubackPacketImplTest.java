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
package com.hivemq.extensions.packets.unsuback;

import com.google.common.collect.ImmutableList;
import com.hivemq.configuration.service.ConfigurationService;
import org.jetbrains.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.unsuback.UnsubackReasonCode;
import com.hivemq.extensions.packets.general.UserPropertiesImpl;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import org.junit.Before;
import org.junit.Test;
import util.TestConfigurationBootstrap;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 * @author Robin Atherton
 * @author Silvio Giebl
 */
public class ModifiableUnsubackPacketImplTest {

    private @NotNull ConfigurationService configurationService;

    @Before
    public void setUp() {
        configurationService = new TestConfigurationBootstrap().getConfigurationService();
    }

    @Test
    public void setReasonCodes() {
        final UnsubackPacketImpl packet = new UnsubackPacketImpl(
                ImmutableList.of(UnsubackReasonCode.SUCCESS, UnsubackReasonCode.IMPLEMENTATION_SPECIFIC_ERROR),
                "reason",
                1,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableUnsubackPacketImpl modifiablePacket =
                new ModifiableUnsubackPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setReasonCodes(
                ImmutableList.of(UnsubackReasonCode.NO_SUBSCRIPTIONS_EXISTED, UnsubackReasonCode.NOT_AUTHORIZED));

        assertTrue(modifiablePacket.isModified());
        assertEquals(
                ImmutableList.of(UnsubackReasonCode.NO_SUBSCRIPTIONS_EXISTED, UnsubackReasonCode.NOT_AUTHORIZED),
                modifiablePacket.getReasonCodes());
    }

    @Test
    public void setReasonCodes_same() {
        final UnsubackPacketImpl packet = new UnsubackPacketImpl(
                ImmutableList.of(UnsubackReasonCode.SUCCESS, UnsubackReasonCode.IMPLEMENTATION_SPECIFIC_ERROR),
                "reason",
                1,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableUnsubackPacketImpl modifiablePacket =
                new ModifiableUnsubackPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setReasonCodes(
                ImmutableList.of(UnsubackReasonCode.SUCCESS, UnsubackReasonCode.IMPLEMENTATION_SPECIFIC_ERROR));

        assertFalse(modifiablePacket.isModified());
        assertEquals(
                ImmutableList.of(UnsubackReasonCode.SUCCESS, UnsubackReasonCode.IMPLEMENTATION_SPECIFIC_ERROR),
                modifiablePacket.getReasonCodes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void setReasonCodes_tooMany() {
        final UnsubackPacketImpl packet = new UnsubackPacketImpl(
                ImmutableList.of(UnsubackReasonCode.SUCCESS, UnsubackReasonCode.IMPLEMENTATION_SPECIFIC_ERROR),
                "reason",
                1,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableUnsubackPacketImpl modifiablePacket =
                new ModifiableUnsubackPacketImpl(packet, configurationService);

        modifiablePacket.setReasonCodes(ImmutableList.of(
                UnsubackReasonCode.NO_SUBSCRIPTIONS_EXISTED,
                UnsubackReasonCode.NOT_AUTHORIZED,
                UnsubackReasonCode.UNSPECIFIED_ERROR));
    }

    @Test(expected = IllegalArgumentException.class)
    public void setReasonCodes_tooFew() {
        final UnsubackPacketImpl packet = new UnsubackPacketImpl(
                ImmutableList.of(UnsubackReasonCode.SUCCESS, UnsubackReasonCode.IMPLEMENTATION_SPECIFIC_ERROR),
                "reason",
                1,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableUnsubackPacketImpl modifiablePacket =
                new ModifiableUnsubackPacketImpl(packet, configurationService);

        modifiablePacket.setReasonCodes(ImmutableList.of(UnsubackReasonCode.NO_SUBSCRIPTIONS_EXISTED));
    }

    @Test(expected = NullPointerException.class)
    public void setReasonCodes_null() {
        final UnsubackPacketImpl packet = new UnsubackPacketImpl(
                ImmutableList.of(UnsubackReasonCode.SUCCESS, UnsubackReasonCode.IMPLEMENTATION_SPECIFIC_ERROR),
                "reason",
                1,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableUnsubackPacketImpl modifiablePacket =
                new ModifiableUnsubackPacketImpl(packet, configurationService);

        modifiablePacket.setReasonCodes(null);
    }

    @Test(expected = NullPointerException.class)
    public void setReasonCodes_nullElement() {
        final UnsubackPacketImpl packet = new UnsubackPacketImpl(
                ImmutableList.of(UnsubackReasonCode.SUCCESS, UnsubackReasonCode.IMPLEMENTATION_SPECIFIC_ERROR),
                "reason",
                1,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableUnsubackPacketImpl modifiablePacket =
                new ModifiableUnsubackPacketImpl(packet, configurationService);

        modifiablePacket.setReasonCodes(Arrays.asList(UnsubackReasonCode.SUCCESS, null));
    }

    @Test
    public void setReasonString() {
        final UnsubackPacketImpl packet = new UnsubackPacketImpl(
                ImmutableList.of(UnsubackReasonCode.SUCCESS), null, 1, UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableUnsubackPacketImpl modifiablePacket =
                new ModifiableUnsubackPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setReasonString("reason");

        assertEquals(Optional.of("reason"), modifiablePacket.getReasonString());
        assertTrue(modifiablePacket.isModified());
    }

    @Test
    public void setReasonString_null() {
        final UnsubackPacketImpl packet = new UnsubackPacketImpl(
                ImmutableList.of(UnsubackReasonCode.SUCCESS), "reason", 1, UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableUnsubackPacketImpl modifiablePacket =
                new ModifiableUnsubackPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setReasonString(null);

        assertEquals(Optional.empty(), modifiablePacket.getReasonString());
        assertTrue(modifiablePacket.isModified());
    }

    @Test
    public void setReasonString_same() {
        final UnsubackPacketImpl packet = new UnsubackPacketImpl(
                ImmutableList.of(UnsubackReasonCode.SUCCESS), "same", 1, UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableUnsubackPacketImpl modifiablePacket =
                new ModifiableUnsubackPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setReasonString("same");

        assertEquals(Optional.of("same"), modifiablePacket.getReasonString());
        assertFalse(modifiablePacket.isModified());
    }

    @Test(expected = IllegalArgumentException.class)
    public void setReasonString_invalid() {
        final UnsubackPacketImpl packet = new UnsubackPacketImpl(
                ImmutableList.of(UnsubackReasonCode.SUCCESS), "same", 1, UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableUnsubackPacketImpl modifiablePacket =
                new ModifiableUnsubackPacketImpl(packet, configurationService);

        modifiablePacket.setReasonString("topic" + '\u0001');
    }

    @Test(expected = IllegalArgumentException.class)
    public void setReasonString_exceedsMaxLength() {
        final UnsubackPacketImpl packet = new UnsubackPacketImpl(
                ImmutableList.of(UnsubackReasonCode.SUCCESS), "same", 1, UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableUnsubackPacketImpl modifiablePacket =
                new ModifiableUnsubackPacketImpl(packet, configurationService);

        final StringBuilder s = new StringBuilder("s");
        for (int i = 0; i < 65535; i++) {
            s.append("s");
        }
        modifiablePacket.setReasonString(s.toString());
    }

    @Test
    public void copy_noChanges() {
        final UnsubackPacketImpl packet = new UnsubackPacketImpl(
                ImmutableList.of(UnsubackReasonCode.SUCCESS, UnsubackReasonCode.IMPLEMENTATION_SPECIFIC_ERROR),
                "reason",
                1,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableUnsubackPacketImpl modifiablePacket =
                new ModifiableUnsubackPacketImpl(packet, configurationService);

        final UnsubackPacketImpl copy = modifiablePacket.copy();

        assertEquals(packet, copy);
    }

    @Test
    public void copy_changes() {
        final UnsubackPacketImpl packet = new UnsubackPacketImpl(
                ImmutableList.of(UnsubackReasonCode.SUCCESS, UnsubackReasonCode.IMPLEMENTATION_SPECIFIC_ERROR),
                "reason",
                1,
                UserPropertiesImpl.of(ImmutableList.of()));
        final ModifiableUnsubackPacketImpl modifiablePacket =
                new ModifiableUnsubackPacketImpl(packet, configurationService);

        modifiablePacket.setReasonCodes(
                ImmutableList.of(UnsubackReasonCode.NO_SUBSCRIPTIONS_EXISTED, UnsubackReasonCode.NOT_AUTHORIZED));
        modifiablePacket.setReasonString("testReason");
        modifiablePacket.getUserProperties().addUserProperty("testName", "testValue");
        final UnsubackPacketImpl copy = modifiablePacket.copy();

        final UnsubackPacketImpl expectedPacket = new UnsubackPacketImpl(
                ImmutableList.of(UnsubackReasonCode.NO_SUBSCRIPTIONS_EXISTED, UnsubackReasonCode.NOT_AUTHORIZED),
                "testReason",
                1,
                UserPropertiesImpl.of(ImmutableList.of(new MqttUserProperty("testName", "testValue"))));
        assertEquals(expectedPacket, copy);
    }
}
