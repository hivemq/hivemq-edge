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

package com.hivemq.extensions.handler.tasks;

import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.common.shutdown.ShutdownHooks;
import org.jetbrains.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.packets.subscribe.SubackReasonCode;
import com.hivemq.extensions.auth.parameter.SubscriptionAuthorizerOutputImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginOutputAsyncerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;

import static com.hivemq.extensions.auth.parameter.SubscriptionAuthorizerOutputImpl.AuthorizationState.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class SubscriptionAuthorizerContextTest {

    private @NotNull SubscriptionAuthorizerContext context;
    private @NotNull SettableFuture<SubscriptionAuthorizerOutputImpl> resultFuture;
    private @NotNull SubscriptionAuthorizerOutputImpl output;
    @BeforeEach
    public void before() {
        final PluginOutPutAsyncer asyncer = new PluginOutputAsyncerImpl(mock(ShutdownHooks.class));
        resultFuture = SettableFuture.create();
        output = new SubscriptionAuthorizerOutputImpl(asyncer);
        context = new SubscriptionAuthorizerContext("clientId", output, resultFuture, 1);
    }

    @Test
    @Timeout(5)
    public void test_async_timeout_fail() throws Exception {
        output.markAsAsync();
        output.markAsTimedOut();

        context.pluginPost(output);

        final SubscriptionAuthorizerOutputImpl result = resultFuture.get();
        assertEquals(FAIL, result.getAuthorizationState());
        assertEquals(SubackReasonCode.NOT_AUTHORIZED, result.getSubackReasonCode());
        assertTrue(result.isCompleted());
    }

    @Test
    @Timeout(5)
    public void test_async_timeout_success() throws Exception {
        output.async(Duration.ofSeconds(10), TimeoutFallback.SUCCESS);
        output.markAsAsync();
        output.markAsTimedOut();

        context.pluginPost(output);

        final SubscriptionAuthorizerOutputImpl result = resultFuture.get();
        assertEquals(UNDECIDED, result.getAuthorizationState());
        assertFalse(result.isCompleted());
    }

    @Test
    @Timeout(5)
    public void test_success() throws Exception {
        output.authorizeSuccessfully();

        context.pluginPost(output);

        final SubscriptionAuthorizerOutputImpl result = resultFuture.get();
        assertEquals(SUCCESS, result.getAuthorizationState());
        assertTrue(result.isCompleted());
    }

    @Test
    @Timeout(5)
    public void test_fail() throws Exception {
        output.failAuthorization();

        context.pluginPost(output);

        final SubscriptionAuthorizerOutputImpl result = resultFuture.get();
        assertEquals(FAIL, result.getAuthorizationState());
        assertTrue(result.isCompleted());
    }

    @Test
    @Timeout(5)
    public void test_disconnect() throws Exception {
        output.disconnectClient();

        context.pluginPost(output);

        final SubscriptionAuthorizerOutputImpl result = resultFuture.get();
        assertEquals(DISCONNECT, result.getAuthorizationState());
        assertTrue(result.isCompleted());
    }

    @Test
    @Timeout(5)
    public void test_undecided() throws Exception {
        context.pluginPost(output);

        final SubscriptionAuthorizerOutputImpl result = resultFuture.get();
        assertEquals(UNDECIDED, result.getAuthorizationState());
        assertFalse(result.isCompleted());
    }

    @Test
    @Timeout(5)
    public void test_increment_future_returns() throws Exception {
        context.increment();

        final SubscriptionAuthorizerOutputImpl result = resultFuture.get();
        assertEquals(UNDECIDED, result.getAuthorizationState());
        assertFalse(result.isCompleted());
    }
}
