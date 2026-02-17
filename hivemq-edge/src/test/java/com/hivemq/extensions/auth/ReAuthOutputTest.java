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
package com.hivemq.extensions.auth;

import static com.hivemq.extensions.auth.AuthenticationState.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.packets.general.DisconnectedReasonCode;
import com.hivemq.extensions.auth.parameter.ModifiableClientSettingsImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.packets.general.ModifiableDefaultPermissionsImpl;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.util.Bytes;
import com.hivemq.util.ReasonStrings;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * @author Florian Limpöck
 */
public class ReAuthOutputTest {

    @Mock
    private PluginOutPutAsyncer asyncer;

    private ReAuthOutput authTaskOutput;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        authTaskOutput = new ReAuthOutput(
                asyncer, true, new ModifiableDefaultPermissionsImpl(), new ModifiableClientSettingsImpl(10, null), 30);
    }

    @Test
    @Timeout(5)
    public void test_user_properties_of_connect_are_not_present() {
        final List<String> one = authTaskOutput.getOutboundUserProperties().getAllForName("one");
        final List<String> two = authTaskOutput.getOutboundUserProperties().getAllForName("two");

        assertEquals(0, one.size());
        assertEquals(0, two.size());
    }

    @Test
    @Timeout(5)
    public void test_copy_constructor_receives_user_properties() {

        authTaskOutput.getOutboundUserProperties().addUserProperty("testA", "valueA");

        authTaskOutput = new ReAuthOutput(authTaskOutput);
        authTaskOutput.getOutboundUserProperties().addUserProperty("testB", "valueB");

        authTaskOutput = new ReAuthOutput(authTaskOutput);
        authTaskOutput.getOutboundUserProperties().addUserProperty("testC", "valueC");

        authTaskOutput = new ReAuthOutput(authTaskOutput);

        assertEquals(3, authTaskOutput.getOutboundUserProperties().asList().size());
    }

    @Test
    @Timeout(5)
    public void test_fail_sets_decided() {
        authTaskOutput.failAuthentication();
        assertEquals(FAILED, authTaskOutput.getAuthenticationState());
        assertEquals(Mqtt5DisconnectReasonCode.NOT_AUTHORIZED, authTaskOutput.getReasonCode());
        assertEquals(ReasonStrings.RE_AUTH_FAILED, authTaskOutput.getReasonString());
    }

    @Test
    @Timeout(5)
    public void test_fail_with_reason() {
        authTaskOutput.failAuthentication(DisconnectedReasonCode.BAD_AUTHENTICATION_METHOD, "REASON");
        assertEquals(FAILED, authTaskOutput.getAuthenticationState());
        assertEquals(Mqtt5DisconnectReasonCode.BAD_AUTHENTICATION_METHOD, authTaskOutput.getReasonCode());
        assertEquals("REASON", authTaskOutput.getReasonString());
    }

    @Test
    @Timeout(5)
    public void test_fail_with_reason_success() {
        assertThatThrownBy(() -> authTaskOutput.failAuthentication(DisconnectedReasonCode.SUCCESS, "REASON"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @Timeout(5)
    public void test_success_sets_decided() {
        authTaskOutput.authenticateSuccessfully();
        assertEquals(SUCCESS, authTaskOutput.getAuthenticationState());
    }

    @Test
    @Timeout(5)
    public void test_success_bytes_data() {
        authTaskOutput.authenticateSuccessfully("data".getBytes());
        assertEquals(SUCCESS, authTaskOutput.getAuthenticationState());
        assertArrayEquals("data".getBytes(), Bytes.fromReadOnlyBuffer(authTaskOutput.getAuthenticationData()));
    }

    @Test
    @Timeout(5)
    public void test_success_buffer_data() {
        authTaskOutput.authenticateSuccessfully(ByteBuffer.wrap("data".getBytes()));
        assertEquals(SUCCESS, authTaskOutput.getAuthenticationState());
        assertArrayEquals("data".getBytes(), Bytes.fromReadOnlyBuffer(authTaskOutput.getAuthenticationData()));
    }

    @Test
    @Timeout(5)
    public void test_continue() {
        authTaskOutput.continueAuthentication();
        assertEquals(CONTINUE, authTaskOutput.getAuthenticationState());
    }

    @Test
    @Timeout(5)
    public void test_continue_bytes_data() {
        authTaskOutput.continueAuthentication("data".getBytes());
        assertEquals(CONTINUE, authTaskOutput.getAuthenticationState());
        assertArrayEquals("data".getBytes(), Bytes.fromReadOnlyBuffer(authTaskOutput.getAuthenticationData()));
    }

    @Test
    @Timeout(5)
    public void test_continue_buffer_data() {
        authTaskOutput.continueAuthentication(ByteBuffer.wrap("data".getBytes()));
        assertEquals(CONTINUE, authTaskOutput.getAuthenticationState());
        assertArrayEquals("data".getBytes(), Bytes.fromReadOnlyBuffer(authTaskOutput.getAuthenticationData()));
    }

    @Test
    @Timeout(5)
    public void test_continue_does_not_decide() {
        authTaskOutput.nextExtensionOrDefault();
        assertEquals(NEXT_EXTENSION_OR_DEFAULT, authTaskOutput.getAuthenticationState());
    }

    @Test
    @Timeout(5)
    public void test_throwable_fails_authentication() {
        authTaskOutput.failByThrowable(new RuntimeException());
        assertEquals(FAILED, authTaskOutput.getAuthenticationState());
        assertEquals(Mqtt5DisconnectReasonCode.UNSPECIFIED_ERROR, authTaskOutput.getReasonCode());
        assertEquals(ReasonStrings.RE_AUTH_FAILED_EXCEPTION, authTaskOutput.getReasonString());
    }

    @Test
    @Timeout(5)
    public void test_async_duration() {

        authTaskOutput.async(Duration.ofSeconds(10));
        authTaskOutput.failByTimeout();

        assertEquals(Mqtt5DisconnectReasonCode.NOT_AUTHORIZED, authTaskOutput.getReasonCode());
        assertEquals(ReasonStrings.RE_AUTH_FAILED_EXTENSION_TIMEOUT, authTaskOutput.getReasonString());
    }

    @Test
    @Timeout(5)
    public void test_async_null() {

        assertThatThrownBy(() -> authTaskOutput.async(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @Timeout(5)
    public void test_async_duration_fallback() {

        authTaskOutput.async(Duration.ofSeconds(10), TimeoutFallback.SUCCESS);
        authTaskOutput.failByTimeout();

        assertEquals(Mqtt5DisconnectReasonCode.NOT_AUTHORIZED, authTaskOutput.getReasonCode());
        assertEquals(ReasonStrings.RE_AUTH_FAILED_EXTENSION_TIMEOUT, authTaskOutput.getReasonString());
    }

    @Test
    @Timeout(5)
    public void test_async_duration_fallback_code() {

        authTaskOutput.async(
                Duration.ofSeconds(10), TimeoutFallback.FAILURE, DisconnectedReasonCode.BAD_AUTHENTICATION_METHOD);
        authTaskOutput.failByTimeout();

        assertEquals(Mqtt5DisconnectReasonCode.BAD_AUTHENTICATION_METHOD, authTaskOutput.getReasonCode());
        assertEquals(ReasonStrings.RE_AUTH_FAILED_EXTENSION_TIMEOUT, authTaskOutput.getReasonString());
    }

    @Test
    @Timeout(5)
    public void test_async_duration_fallback_string() {

        authTaskOutput.async(Duration.ofSeconds(10), TimeoutFallback.FAILURE, "Failed by me");
        authTaskOutput.failByTimeout();

        assertEquals(Mqtt5DisconnectReasonCode.NOT_AUTHORIZED, authTaskOutput.getReasonCode());
        assertEquals("Failed by me", authTaskOutput.getReasonString());
    }

    @Test
    @Timeout(5)
    public void test_async_success_reason_code() {

        assertThatThrownBy(() -> authTaskOutput.async(
                        Duration.ofSeconds(10),
                        TimeoutFallback.SUCCESS,
                        DisconnectedReasonCode.SUCCESS,
                        "Failed by me"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @Timeout(5)
    public void test_async_duration_fallback_code_string() {

        authTaskOutput.async(
                Duration.ofSeconds(10), TimeoutFallback.FAILURE, DisconnectedReasonCode.SERVER_BUSY, "Failed by me");
        authTaskOutput.failByTimeout();

        assertEquals(Mqtt5DisconnectReasonCode.SERVER_BUSY, authTaskOutput.getReasonCode());
        assertEquals("Failed by me", authTaskOutput.getReasonString());
    }
}
