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
package com.hivemq.embedded.internal;

import com.hivemq.configuration.service.entity.Listener;
import com.hivemq.configuration.service.impl.listener.ListenerConfigurationService;
import com.hivemq.extension.sdk.api.ExtensionMain;
import org.jetbrains.annotations.NotNull;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopOutput;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import util.RandomPortGenerator;
import util.TestExtensionUtil;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class EmbeddedHiveMQImplTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private final String extensionName = "extension-1";
    private File data;
    private File license;
    private File extensions;
    private File conf;
    private int randomPort;
    private int randomApiPort;

    @Before
    public void setUp() throws Exception {
        data = tmp.newFolder("data");
        license = tmp.newFolder("license");
        extensions = tmp.newFolder("extensions");
        conf = tmp.newFolder("conf");
        randomPort = RandomPortGenerator.get();
        randomApiPort = RandomPortGenerator.get();

        final String noListenerConfig = "" +
                "<hivemq>\n" +
                "    <mqtt-listeners>\n" +
                "        <tcp-listener>\n" +
                "            <port>"+randomPort+"</port>\n" +
                "            <bind-address>0.0.0.0</bind-address>\n" +
                "        </tcp-listener>\n" +
                "    </mqtt-listeners>\n" +
                "    <admin-api>\n" +
                "        <listeners>\n" +
                "            <http-listener>\n" +
                "                <port>"+randomApiPort+"</port>\n" +
                "            </http-listener>\n" +
                "        </listeners>\n" +
                "    </admin-api>\n" +
                "</hivemq>";
        FileUtils.write(new File(conf, "config.xml"), noListenerConfig, StandardCharsets.UTF_8);

        TestExtensionUtil.shrinkwrapExtension(extensions, extensionName, Main.class, true);
    }

    @Test(timeout = 20000L)
    public void embeddedHiveMQ_readsConfig() {
        final EmbeddedHiveMQImpl embeddedHiveMQ = new EmbeddedHiveMQImpl(conf, data, extensions, license);
        embeddedHiveMQ.start().join();

        final ListenerConfigurationService listenerConfigurationService =
                embeddedHiveMQ.bootstrapConfig().listenerConfiguration();
        final List<Listener> listeners = listenerConfigurationService.getListeners();

        assertEquals(1, listeners.size());
        assertEquals(randomPort, listeners.get(0).getPort());

        embeddedHiveMQ.stop().join();
    }

    @Test(timeout = 20000L)
    public void embeddedHiveMQ_usesExtensionsFolder() {
        final EmbeddedHiveMQImpl embeddedHiveMQ = new EmbeddedHiveMQImpl(conf, data, extensions, license);
        embeddedHiveMQ.start().join();

        final HiveMQExtensions hiveMQExtensions = embeddedHiveMQ.getInjector().extensions().hivemqExtensions();

        final HiveMQExtension extension = hiveMQExtensions.getExtension(extensionName);
        assertNotNull(extension);

        embeddedHiveMQ.stop().join();
    }

    @Test(timeout = 20000L)
    public void start_multipleStartsAreIdempotent() {
        final EmbeddedHiveMQImpl embeddedHiveMQ = new EmbeddedHiveMQImpl(conf, data, extensions, license);
        final CountDownLatch blockingLatch = new CountDownLatch(1);

        embeddedHiveMQ.stateChangeExecutor.submit(() -> {
            blockingLatch.await();
            return null;
        });

        embeddedHiveMQ.start();
        final CompletableFuture<Void> future = embeddedHiveMQ.start();

        blockingLatch.countDown();
        future.join();
        embeddedHiveMQ.stop().join();
    }

    @Test(timeout = 20000L)
    public void stop_multipleStopsAreIdempotent() {
        final EmbeddedHiveMQImpl embeddedHiveMQ = new EmbeddedHiveMQImpl(conf, data, extensions, license);
        embeddedHiveMQ.start().join();

        final CountDownLatch blockingLatch = new CountDownLatch(1);

        embeddedHiveMQ.stateChangeExecutor.submit(() -> {
            blockingLatch.await();
            return null;
        });

        embeddedHiveMQ.stop();
        final CompletableFuture<Void> future = embeddedHiveMQ.stop();

        blockingLatch.countDown();
        future.join();
    }

    @Test(timeout = 20000L)
    public void start_startCancelsStop() {
        final EmbeddedHiveMQImpl embeddedHiveMQ = new EmbeddedHiveMQImpl(conf, data, extensions, license);
        embeddedHiveMQ.start().join();

        final CountDownLatch blockingLatch = new CountDownLatch(1);

        embeddedHiveMQ.stateChangeExecutor.submit(() -> {
            blockingLatch.await();
            return null;
        });

        final CompletableFuture<Void> stop = embeddedHiveMQ.stop();
        final CompletableFuture<Void> start = embeddedHiveMQ.start();

        blockingLatch.countDown();
        start.join();

        assertTrue(stop.isCompletedExceptionally());
    }

    @Test(timeout = 20000L)
    public void stop_stopCancelsStart() {
        final EmbeddedHiveMQImpl embeddedHiveMQ = new EmbeddedHiveMQImpl(conf, data, extensions, license);

        final CountDownLatch blockingLatch = new CountDownLatch(1);

        embeddedHiveMQ.stateChangeExecutor.submit(() -> {
            blockingLatch.await();
            return null;
        });

        final CompletableFuture<Void> start = embeddedHiveMQ.start();
        final CompletableFuture<Void> stop = embeddedHiveMQ.stop();

        blockingLatch.countDown();
        stop.join();

        assertTrue(start.isCompletedExceptionally());
    }

    @Test(timeout = 20000L)
    public void close_preventsStart() throws ExecutionException, InterruptedException {
        final EmbeddedHiveMQImpl embeddedHiveMQ = new EmbeddedHiveMQImpl(conf, data, extensions, license);

        embeddedHiveMQ.close();
        final CompletableFuture<Void> start = embeddedHiveMQ.start();

        assertTrue(start.isCompletedExceptionally());
    }

    @Test(timeout = 20000L)
    public void close_preventsStop() throws ExecutionException, InterruptedException {
        final EmbeddedHiveMQImpl embeddedHiveMQ = new EmbeddedHiveMQImpl(conf, data, extensions, license);

        embeddedHiveMQ.close();
        final CompletableFuture<Void> stop = embeddedHiveMQ.stop();

        assertTrue(stop.isCompletedExceptionally());
    }

    @Test(timeout = 20000L)
    public void close_calledMultipleTimes() throws InterruptedException {
        final EmbeddedHiveMQImpl embeddedHiveMQ = new EmbeddedHiveMQImpl(conf, data, extensions, license);
        final CountDownLatch blockingLatch = new CountDownLatch(1);

        embeddedHiveMQ.stateChangeExecutor.submit(() -> {
            blockingLatch.await();
            return null;
        });

        new Thread(() -> {
            try {
                embeddedHiveMQ.close();
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            try {
                embeddedHiveMQ.close();
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }).start();

        Thread.sleep(100);

        final List<Runnable> runnableList = embeddedHiveMQ.stateChangeExecutor.shutdownNow();

        // the blocking Latch callable is already executed, so one embeddedHiveMQ.stateChange and one executor.shutdown
        // are expected
        assertEquals(2, runnableList.size());
    }

    @Test(timeout = 20000L)
    public void test_hivemq_uses_embedded_extension_with_normal() throws ExecutionException, InterruptedException {

        final EmbeddedMain embeddedMain = new EmbeddedMain();

        final EmbeddedExtensionImpl extension =
                new EmbeddedExtensionImpl("id", "name", "123", "luke_skywalker", 0, 1000, embeddedMain);

        final EmbeddedHiveMQImpl embeddedHiveMQ = new EmbeddedHiveMQImpl(conf, data, extensions, license, extension);
        embeddedHiveMQ.start().get();

        assertTrue(embeddedMain.running.get());

        final HiveMQExtensions hiveMQExtensions = embeddedHiveMQ.getInjector().extensions().hivemqExtensions();

        final HiveMQExtension extension1 = hiveMQExtensions.getExtension(extensionName);
        final HiveMQExtension extension2 = hiveMQExtensions.getExtension("id");
        assertNotNull(extension1);
        assertNotNull(extension2);

        embeddedHiveMQ.stop().get();

    }

    public static class Main implements ExtensionMain {

        @Override
        public void extensionStart(
                final @NotNull ExtensionStartInput extensionStartInput,
                final @NotNull ExtensionStartOutput extensionStartOutput) {
        }

        @Override
        public void extensionStop(
                final @NotNull ExtensionStopInput extensionStopInput,
                final @NotNull ExtensionStopOutput extensionStopOutput) {
        }
    }

    public static class EmbeddedMain implements ExtensionMain {

        public final @NotNull AtomicBoolean running = new AtomicBoolean();

        @Override
        public void extensionStart(
                final @NotNull ExtensionStartInput extensionStartInput,
                final @NotNull ExtensionStartOutput extensionStartOutput) {
            running.set(true);
        }

        @Override
        public void extensionStop(
                final @NotNull ExtensionStopInput extensionStopInput,
                final @NotNull ExtensionStopOutput extensionStopOutput) {
            running.set(false);
        }
    }
}
