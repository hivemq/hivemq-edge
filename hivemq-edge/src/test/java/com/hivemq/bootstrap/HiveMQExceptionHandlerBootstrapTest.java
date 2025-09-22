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

import com.hivemq.exceptions.UnrecoverableException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christoph Schäbel
 */
public class HiveMQExceptionHandlerBootstrapTest {

    @Test
    public void test_unrecoverableException_false() {
        AtomicBoolean invoked = new AtomicBoolean(false);
        HiveMQExceptionHandlerBootstrap.setTerminator(() -> invoked.set(true));
        HiveMQExceptionHandlerBootstrap.handleUncaughtException(Thread.currentThread(),
                new UnrecoverableException(false));
        assertThat(invoked.get()).isTrue();
    }

    @Test
    public void test_unrecoverableException_true() {
        AtomicBoolean invoked = new AtomicBoolean(false);
        HiveMQExceptionHandlerBootstrap.setTerminator(() -> invoked.set(true));
        HiveMQExceptionHandlerBootstrap.handleUncaughtException(Thread.currentThread(),
                new UnrecoverableException(true));
        assertThat(invoked.get()).isTrue();
    }

    @Test
    public void test_unrecoverableException_wrapped() {
        AtomicBoolean invoked = new AtomicBoolean(false);
        HiveMQExceptionHandlerBootstrap.setTerminator(() -> invoked.set(true));
        HiveMQExceptionHandlerBootstrap.handleUncaughtException(Thread.currentThread(),
                new RuntimeException("test", new UnrecoverableException(true)));
        assertThat(invoked.get()).isFalse();
    }
}
