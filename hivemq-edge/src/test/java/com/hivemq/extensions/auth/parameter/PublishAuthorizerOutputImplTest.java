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
package com.hivemq.extensions.auth.parameter;

import static com.hivemq.extensions.auth.parameter.PublishAuthorizerOutputImpl.AuthorizationState.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectReasonCode;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * @author Christoph Schäbel
 */
@SuppressWarnings("NullabilityAnnotations")
public class PublishAuthorizerOutputImplTest {

    @Mock
    private PluginOutPutAsyncer asyncer;

    private PublishAuthorizerOutputImpl output;

    @BeforeEach
    public void before() {
        MockitoAnnotations.initMocks(this);
        output = new PublishAuthorizerOutputImpl(asyncer);
    }

    @Test
    public void test_output_success() {
        output.authorizeSuccessfully();
        assertEquals(SUCCESS, output.getAuthorizationState());
    }

    @Test
    public void test_output_none() {
        assertEquals(UNDECIDED, output.getAuthorizationState());
    }

    @Test
    public void test_output_continue() {
        output.nextExtensionOrDefault();
        assertEquals(CONTINUE, output.getAuthorizationState());
        assertFalse(output.isCompleted());
    }

    @Test
    public void test_output_fail() {
        output.failAuthorization();
        assertEquals(FAIL, output.getAuthorizationState());
        assertTrue(output.isCompleted());
    }

    @Test
    public void test_output_force_fail() {
        output.authorizeSuccessfully();
        output.forceFailedAuthorization();
        assertEquals(FAIL, output.getAuthorizationState());
        assertTrue(output.isCompleted());
    }

    @Test
    public void test_output_fail_code() {
        output.failAuthorization(AckReasonCode.QUOTA_EXCEEDED);
        assertEquals(FAIL, output.getAuthorizationState());
        assertEquals(AckReasonCode.QUOTA_EXCEEDED, output.getAckReasonCode());
        assertTrue(output.isCompleted());
    }

    @Test
    public void test_output_fail_code_string() {
        output.failAuthorization(AckReasonCode.QUOTA_EXCEEDED, "test-string");
        assertEquals(FAIL, output.getAuthorizationState());
        assertEquals(AckReasonCode.QUOTA_EXCEEDED, output.getAckReasonCode());
        assertEquals("test-string", output.getReasonString());
        assertTrue(output.isCompleted());
    }

    @Test
    public void test_output_disconnect() {
        output.disconnectClient();
        assertEquals(DISCONNECT, output.getAuthorizationState());
        assertTrue(output.isCompleted());
    }

    @Test
    public void test_output_disconnect_code() {
        output.disconnectClient(DisconnectReasonCode.CONNECTION_RATE_EXCEEDED);
        assertEquals(DISCONNECT, output.getAuthorizationState());
        assertEquals(DisconnectReasonCode.CONNECTION_RATE_EXCEEDED, output.getDisconnectReasonCode());
        assertTrue(output.isCompleted());
    }

    @Test
    public void test_output_disconnect_code_string() {
        output.disconnectClient(DisconnectReasonCode.CONNECTION_RATE_EXCEEDED, "test-string");
        assertEquals(DISCONNECT, output.getAuthorizationState());
        assertEquals(DisconnectReasonCode.CONNECTION_RATE_EXCEEDED, output.getDisconnectReasonCode());
        assertEquals("test-string", output.getReasonString());
        assertTrue(output.isCompleted());
    }

    @Test
    public void test_exception_multiple_result_next() {
        output.authorizeSuccessfully();
        assertThatThrownBy(() -> output.nextExtensionOrDefault()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void test_exception_multiple_result_success() {
        output.authorizeSuccessfully();
        assertThatThrownBy(() -> output.authorizeSuccessfully()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void test_exception_multiple_result_fail() {
        output.authorizeSuccessfully();
        assertThatThrownBy(() -> output.failAuthorization()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void test_exception_multiple_result_disconnect() {
        output.authorizeSuccessfully();
        assertThatThrownBy(() -> output.disconnectClient()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void test_fail_sucess_code() {

        assertThrows(IllegalArgumentException.class, () -> output.failAuthorization(AckReasonCode.SUCCESS));
    }

    @Test
    public void test_fail_string_sucess_code() {

        assertThrows(
                IllegalArgumentException.class, () -> output.failAuthorization(AckReasonCode.SUCCESS, "test-string"));
    }
}
