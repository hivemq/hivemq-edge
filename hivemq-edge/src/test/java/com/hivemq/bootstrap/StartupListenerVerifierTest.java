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
package com.hivemq.bootstrap;

import com.google.common.collect.Lists;
import com.hivemq.configuration.service.entity.MqttTcpListener;
import com.hivemq.exceptions.UnrecoverableException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Dominik Obermaier
 */
public class StartupListenerVerifierTest {


    @Test
    public void test_verifier_verify_only_listener_failed(){
        final ListenerStartupInformation failed = ListenerStartupInformation.failedListenerStartup(2000, new MqttTcpListener(2000, "0.0.0.0"), new RuntimeException("reason"));
        assertThatThrownBy(() -> new StartupListenerVerifier(Lists.newArrayList(failed)).verifyAndPrint())
                .isInstanceOf(UnrecoverableException.class);
    }

    @Test
    public void test_verifier_verify_all_listeners_failed() throws Exception {
        final ListenerStartupInformation failed = ListenerStartupInformation.failedListenerStartup(2000, new MqttTcpListener(2000, "0.0.0.0"), new RuntimeException("reason"));
        final ListenerStartupInformation failed2 = ListenerStartupInformation.failedListenerStartup(1234, new MqttTcpListener(1234, "0.0.0.0"), new RuntimeException("anotherreason"));

        assertThatThrownBy(() -> new StartupListenerVerifier(Lists.newArrayList(failed, failed2)).verifyAndPrint())
                .isInstanceOf(UnrecoverableException.class);
    }

    @Test
    public void test_verifier_verify_some_listeners_failed() throws Exception {
        final ListenerStartupInformation failed = ListenerStartupInformation.failedListenerStartup(2000, new MqttTcpListener(2000, "0.0.0.0"), new RuntimeException("reason"));
        final ListenerStartupInformation success = ListenerStartupInformation.successfulListenerStartup(1234, new MqttTcpListener(1234, "0.0.0.0"));

        new StartupListenerVerifier(Lists.newArrayList(failed, success)).verifyAndPrint();

        //We don't receive an exception so everything is good
    }

    @Test
    public void test_verifier_verify_empty_listeners() {
    
        assertThrows(UnrecoverableException.class, () -> new StartupListenerVerifier(new ArrayList<ListenerStartupInformation>()).verifyAndPrint());
    }

    @Test
    public void test_verifier_doesnt_accept_null() {
    
        assertThrows(NullPointerException.class, () -> new StartupListenerVerifier(null));
    }

}
