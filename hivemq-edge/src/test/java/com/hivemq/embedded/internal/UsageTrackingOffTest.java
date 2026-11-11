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

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.edge.utils.InstallationId;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import util.RandomPortGenerator;

/// The listener that turns usage tracking off is registered in this suite's test services file; this proves it
/// runs, and that its forced-off flag holds against a configuration that enables tracking explicitly.
class UsageTrackingOffTest {

    @TempDir
    File tmp;

    /// Nothing in this suite writes the flags, so these values can only have come from the listener.
    @Test
    void whenTheSuiteRuns_usageTrackingIsOffWithoutTheHarnessDoingAnything() {
        assertThat(InternalConfigurations.DEFAULT_USAGE_EVENTS_ENABLED.get()).isFalse();
        assertThat(InternalConfigurations.USAGE_TRACKING_FORCED_OFF.get()).isTrue();
    }

    /// The installation id is written only when the remote service starts, before it queues its startup event, so
    /// its absence shows nothing was queued or sent although the configuration still reads as enabled.
    @Test
    @Timeout(20)
    void whenConfigEnablesUsageTracking_edgeStartsWithUsageTrackingOff() throws Exception {
        final File conf = newFolder("conf");
        final File data = newFolder("data");
        final String config = "<hivemq>\n"
                + "    <mqtt-listeners>\n"
                + "        <tcp-listener>\n"
                + "            <port>" + RandomPortGenerator.get() + "</port>\n"
                + "            <bind-address>127.0.0.1</bind-address>\n"
                + "        </tcp-listener>\n"
                + "    </mqtt-listeners>\n"
                + "    <admin-api>\n"
                + "        <listeners>\n"
                + "            <http-listener>\n"
                + "                <port>" + RandomPortGenerator.get() + "</port>\n"
                + "            </http-listener>\n"
                + "        </listeners>\n"
                + "    </admin-api>\n"
                + "    <usage-tracking>\n"
                + "        <enabled>true</enabled>\n"
                + "    </usage-tracking>\n"
                + "</hivemq>";
        FileUtils.write(new File(conf, "config.xml"), config, StandardCharsets.UTF_8);

        // close() waits interruptibly, so the interrupt @Timeout sends ends a stuck start; 15 s stays inside its 20 s
        try (final EmbeddedHiveMQImpl embeddedHiveMQ =
                new EmbeddedHiveMQImpl(conf, data, newFolder("extensions"), newFolder("license"))) {
            embeddedHiveMQ.start().get(15, TimeUnit.SECONDS);

            assertThat(embeddedHiveMQ
                            .bootstrapConfig()
                            .usageTrackingConfiguration()
                            .isUsageTrackingEnabled())
                    .isTrue();
            assertThat(new File(data, InstallationId.FILE_NAME)).doesNotExist();
        }
    }

    private @NotNull File newFolder(final @NotNull String name) {
        final File folder = new File(tmp, name);
        assertThat(folder.mkdir()).isTrue();
        return folder;
    }
}
