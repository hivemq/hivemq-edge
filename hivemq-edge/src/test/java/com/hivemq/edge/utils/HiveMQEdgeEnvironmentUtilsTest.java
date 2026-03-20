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
package com.hivemq.edge.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class HiveMQEdgeEnvironmentUtilsTest {

    @Test
    void installationToken_returnsSameValueOnRepeatedCalls() {
        final String first = HiveMQEdgeEnvironmentUtils.generateInstallationToken();
        final String second = HiveMQEdgeEnvironmentUtils.generateInstallationToken();
        final String third = HiveMQEdgeEnvironmentUtils.generateInstallationToken();

        assertNotNull(first);
        assertFalse(first.isBlank());
        assertEquals(first, second, "Installation token must be stable across calls");
        assertEquals(first, third, "Installation token must be stable across calls");
    }

    @Test
    void installationToken_stableUnderConcurrentAccess() throws InterruptedException {
        final int threadCount = 32;
        final Set<String> tokens = ConcurrentHashMap.newKeySet();
        final CountDownLatch ready = new CountDownLatch(threadCount);
        final CountDownLatch go = new CountDownLatch(1);
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    go.await(5, TimeUnit.SECONDS);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                tokens.add(HiveMQEdgeEnvironmentUtils.generateInstallationToken());
            });
        }

        ready.await(5, TimeUnit.SECONDS);
        go.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        assertEquals(
                1,
                tokens.size(),
                "All threads must observe the same installation token, but got " + tokens.size() + " distinct values");
    }

    @Test
    void sessionToken_returnsSameValueOnRepeatedCalls() {
        final String first = HiveMQEdgeEnvironmentUtils.getSessionToken();
        final String second = HiveMQEdgeEnvironmentUtils.getSessionToken();

        assertNotNull(first);
        assertFalse(first.isBlank());
        assertEquals(first, second, "Session token must be stable across calls");
    }
}
