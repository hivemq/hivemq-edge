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
package com.hivemq.extensions;

import static junit.framework.TestCase.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopOutput;
import com.hivemq.extensions.config.HiveMQExtensionXMLReader;
import com.hivemq.extensions.parameter.ExtensionStartOutputImpl;
import com.hivemq.extensions.parameter.ExtensionStartStopInputImpl;
import com.hivemq.extensions.parameter.ExtensionStopOutputImpl;
import java.io.File;
import java.util.Collections;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import util.TestExtensionUtil;

/**
 * @author Georg Held
 */
@SuppressWarnings("NullabilityAnnotations")
public class HiveMQExtensionExtensionTest extends AbstractExtensionTest {

    @TempDir
    public File tmpFolder;

    private final @NotNull ServerInformation serverInformation = mock(ServerInformation.class);

    private HiveMQExtension startExtension;
    private HiveMQExtension stopExtension;
    private HiveMQExtension reasonExtension;

    private ExtensionStartOutputImpl extensionStartOutput;
    private ExtensionStopOutputImpl extensionStopOutput;
    private ExtensionStartStopInputImpl extensionStartStopInput;

    @BeforeEach
    public void setUp() throws Exception {
        var extensionFolder = new File(tmpFolder, "extension");
        extensionFolder.mkdir();
        final File validExtensionFolder = TestExtensionUtil.createValidExtension(extensionFolder, "id");
        final Optional<HiveMQExtensionEntity> extensionEntityFromXML =
                HiveMQExtensionXMLReader.getExtensionEntityFromXML(validExtensionFolder.toPath(), true);
        assertTrue(extensionEntityFromXML.isPresent());
        final HiveMQExtensionEntity hiveMQExtensionEntity = extensionEntityFromXML.get();

        startExtension = new HiveMQExtensionImpl(
                hiveMQExtensionEntity, validExtensionFolder.toPath(), new StartTestExtension(), true);
        stopExtension = new HiveMQExtensionImpl(
                hiveMQExtensionEntity, validExtensionFolder.toPath(), new StopTestExtension(), true);
        reasonExtension = new HiveMQExtensionImpl(
                hiveMQExtensionEntity, validExtensionFolder.toPath(), new ReasonTestExtension(), true);

        extensionStartOutput = new ExtensionStartOutputImpl();
        extensionStopOutput = new ExtensionStopOutputImpl();
        extensionStartStopInput = new ExtensionStartStopInputImpl(
                startExtension, Collections.singletonMap(startExtension.getId(), startExtension), serverInformation);
    }

    @Test
    @Timeout(5)
    public void test_instantiate_and_start() throws Throwable {
        startExtension.start(extensionStartStopInput, extensionStartOutput);
        assertTrue(StartTestExtension.start);
    }

    @Test
    @Timeout(5)
    public void test_plugin_stop() throws Throwable {
        stopExtension.start(extensionStartStopInput, extensionStartOutput);
        stopExtension.stop(extensionStartStopInput, extensionStopOutput);

        assertTrue(StopTestExtension.stop);
    }

    @Test
    @Timeout(5)
    public void test_start_reason_gets_set() throws Throwable {
        reasonExtension.start(extensionStartStopInput, extensionStartOutput);

        assertTrue(extensionStartOutput.getReason().isPresent());
        assertSame(ReasonTestExtension.reason, extensionStartOutput.getReason().get());
    }

    public static class StartTestExtension implements ExtensionMain {

        private static boolean start = false;

        @Override
        public void extensionStart(
                final @NotNull ExtensionStartInput input, final @NotNull ExtensionStartOutput output) {
            start = true;
        }

        @Override
        public void extensionStop(final @NotNull ExtensionStopInput input, final @NotNull ExtensionStopOutput output) {}
    }

    public static class StopTestExtension implements ExtensionMain {

        private static boolean stop = false;

        @Override
        public void extensionStart(
                final @NotNull ExtensionStartInput input, final @NotNull ExtensionStartOutput output) {}

        @Override
        public void extensionStop(final @NotNull ExtensionStopInput input, final @NotNull ExtensionStopOutput output) {
            stop = true;
        }
    }

    public static class ReasonTestExtension implements ExtensionMain {

        private static final String reason = "REASON";

        @Override
        public void extensionStart(
                final @NotNull ExtensionStartInput input, final @NotNull ExtensionStartOutput output) {
            output.preventExtensionStartup(reason);
        }

        @Override
        public void extensionStop(final @NotNull ExtensionStopInput input, final @NotNull ExtensionStopOutput output) {}
    }
}
