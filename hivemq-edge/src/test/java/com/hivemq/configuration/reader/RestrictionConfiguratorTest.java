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
package com.hivemq.configuration.reader;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.io.Files;
import com.hivemq.exceptions.UnrecoverableException;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

@SuppressWarnings("UnstableApiUsage")
public class RestrictionConfiguratorTest extends AbstractConfigurationTest {

    @Test
    public void test_restrictions_xml() throws Exception {

        final String contents = "<hivemq>" + "<restrictions>"
                + "<max-connections>500</max-connections>"
                + "<max-client-id-length>400</max-client-id-length>"
                + "<max-topic-length>400</max-topic-length>"
                + "<no-connect-idle-timeout>300</no-connect-idle-timeout>"
                + "<incoming-bandwidth-throttling>200</incoming-bandwidth-throttling>"
                + "</restrictions>"
                + "</hivemq>";
        Files.write(contents.getBytes(UTF_8), xmlFile);

        reader.applyConfig();

        assertEquals(500, restrictionsConfigurationService.maxConnections());
        assertEquals(400, restrictionsConfigurationService.maxClientIdLength());
        assertEquals(400, restrictionsConfigurationService.maxTopicLength());
        assertEquals(300, restrictionsConfigurationService.noConnectIdleTimeout());
        assertEquals(200, restrictionsConfigurationService.incomingLimit());
    }

    @Test
    public void test_restriction_negative_values() throws Exception {

        final String contents = "<hivemq>" + "<restrictions>"
                + "<max-connections>-100</max-connections>"
                + "<max-client-id-length>-100</max-client-id-length>"
                + "<max-topic-length>-100</max-topic-length>"
                + "<no-connect-idle-timeout>-100</no-connect-idle-timeout>"
                + "<incoming-bandwidth-throttling>-100</incoming-bandwidth-throttling>"
                + "</restrictions>"
                + "</hivemq>";
        Files.write(contents.getBytes(UTF_8), xmlFile);

        assertThrows(UnrecoverableException.class, () -> reader.applyConfig());
    }

    @Test
    public void test_tooHigh() throws IOException {
        final String contents = "<hivemq>" + "<restrictions>"
                + "<max-connections>500</max-connections>"
                + "<max-client-id-length>123456</max-client-id-length>"
                + "<max-topic-length>123456</max-topic-length>"
                + "<no-connect-idle-timeout>300</no-connect-idle-timeout>"
                + "<incoming-bandwidth-throttling>200</incoming-bandwidth-throttling>"
                + "</restrictions>"
                + "</hivemq>";
        Files.write(contents.getBytes(UTF_8), xmlFile);

        assertThrows(UnrecoverableException.class, () -> reader.applyConfig());
    }

    @Test
    public void test_topic_filter_and_qos2_flow_limits() throws Exception {
        final String contents = "<hivemq><restrictions>"
                + "<max-topic-filters-per-packet>50</max-topic-filters-per-packet>"
                + "<max-incoming-qos2-flows>2000</max-incoming-qos2-flows>"
                + "<max-incoming-qos2-flows-per-client>20</max-incoming-qos2-flows-per-client>"
                + "</restrictions></hivemq>";
        Files.write(contents.getBytes(UTF_8), xmlFile);

        reader.applyConfig();

        assertEquals(50, restrictionsConfigurationService.maxTopicFiltersPerPacket());
        assertEquals(2000, restrictionsConfigurationService.maxIncomingQos2Flows());
        assertEquals(20, restrictionsConfigurationService.maxIncomingQos2FlowsPerClient());
    }

    @Test
    public void test_topic_filter_and_qos2_flow_limits_change_only_with_a_restart() throws Exception {
        Files.write("<hivemq><restrictions/></hivemq>".getBytes(UTF_8), xmlFile);
        reader.applyConfig();

        Files.write("<hivemq><restrictions/></hivemq>".getBytes(UTF_8), xmlFile);
        assertEquals(ConfigFileReaderWriter.ReloadOutcome.APPLIED, reader.loadConfigFromXML(xmlFile));

        for (final String changed : new String[] {
            "<max-topic-filters-per-packet>50</max-topic-filters-per-packet>",
            "<max-incoming-qos2-flows>2000</max-incoming-qos2-flows>",
            "<max-incoming-qos2-flows-per-client>20</max-incoming-qos2-flows-per-client>"
        }) {
            Files.write(("<hivemq><restrictions>" + changed + "</restrictions></hivemq>").getBytes(UTF_8), xmlFile);
            assertEquals(
                    ConfigFileReaderWriter.ReloadOutcome.NEEDS_RESTART, reader.loadConfigFromXML(xmlFile), changed);
        }
        // the running limits are the ones read at start
        assertEquals(10_000, restrictionsConfigurationService.maxTopicFiltersPerPacket());
        assertEquals(100_000, restrictionsConfigurationService.maxIncomingQos2Flows());
        assertEquals(1_000, restrictionsConfigurationService.maxIncomingQos2FlowsPerClient());
    }

    @Test
    public void test_topic_filter_and_qos2_flow_limit_defaults() throws Exception {
        Files.write("<hivemq><restrictions/></hivemq>".getBytes(UTF_8), xmlFile);

        reader.applyConfig();

        assertEquals(10_000, restrictionsConfigurationService.maxTopicFiltersPerPacket());
        assertEquals(100_000, restrictionsConfigurationService.maxIncomingQos2Flows());
        assertEquals(1_000, restrictionsConfigurationService.maxIncomingQos2FlowsPerClient());
    }

    @Test
    public void test_topic_filter_limit_zero_is_refused() throws IOException {
        Files.write(
                "<hivemq><restrictions><max-topic-filters-per-packet>0</max-topic-filters-per-packet></restrictions></hivemq>"
                        .getBytes(UTF_8),
                xmlFile);

        assertThrows(UnrecoverableException.class, () -> reader.applyConfig());
    }

    @Test
    public void test_qos2_flow_limit_zero_is_refused() throws IOException {
        Files.write(
                "<hivemq><restrictions><max-incoming-qos2-flows>0</max-incoming-qos2-flows></restrictions></hivemq>"
                        .getBytes(UTF_8),
                xmlFile);

        assertThrows(UnrecoverableException.class, () -> reader.applyConfig());
    }

    @Test
    public void test_qos2_flow_limit_per_client_out_of_range_is_refused() throws IOException {
        Files.write(
                "<hivemq><restrictions><max-incoming-qos2-flows-per-client>0</max-incoming-qos2-flows-per-client></restrictions></hivemq>"
                        .getBytes(UTF_8),
                xmlFile);
        assertThrows(UnrecoverableException.class, () -> reader.applyConfig());

        Files.write(
                "<hivemq><restrictions><max-incoming-qos2-flows-per-client>65536</max-incoming-qos2-flows-per-client></restrictions></hivemq>"
                        .getBytes(UTF_8),
                xmlFile);
        assertThrows(UnrecoverableException.class, () -> reader.applyConfig());
    }

    // 2026.14 refuses to start on a config.xml that names any of the three limits added in 2026.15, so a write-back
    // must not add them when nobody set them -- otherwise a rollback after any REST config save fails.
    @Test
    public void test_unset_topic_filter_and_qos2_flow_limits_are_not_written_back() throws Exception {
        Files.write(
                "<hivemq><restrictions><max-connections>500</max-connections></restrictions></hivemq>".getBytes(UTF_8),
                xmlFile);
        reader.applyConfig();

        reader.writeConfigWithSync();

        final String written = java.nio.file.Files.readString(Path.of(xmlFile.getPath()));
        assertThat(written).contains("<max-connections>500</max-connections>");
        assertThat(written).doesNotContain("max-topic-filters-per-packet").doesNotContain("max-incoming-qos2-flows");
        assertEquals(10_000, restrictionsConfigurationService.maxTopicFiltersPerPacket());
        assertEquals(100_000, restrictionsConfigurationService.maxIncomingQos2Flows());
        assertEquals(1_000, restrictionsConfigurationService.maxIncomingQos2FlowsPerClient());
    }

    @Test
    public void test_set_topic_filter_and_qos2_flow_limits_survive_a_write_back() throws Exception {
        // the per-client value is the default, set explicitly: it stays in the file
        Files.write(
                ("<hivemq><restrictions>"
                                + "<max-topic-filters-per-packet>50</max-topic-filters-per-packet>"
                                + "<max-incoming-qos2-flows>2000</max-incoming-qos2-flows>"
                                + "<max-incoming-qos2-flows-per-client>1000</max-incoming-qos2-flows-per-client>"
                                + "</restrictions></hivemq>")
                        .getBytes(UTF_8),
                xmlFile);
        reader.applyConfig();

        reader.writeConfigWithSync();

        final String written = java.nio.file.Files.readString(Path.of(xmlFile.getPath()));
        assertThat(written)
                .contains("<max-topic-filters-per-packet>50</max-topic-filters-per-packet>")
                .contains("<max-incoming-qos2-flows>2000</max-incoming-qos2-flows>")
                .contains("<max-incoming-qos2-flows-per-client>1000</max-incoming-qos2-flows-per-client>");
        assertEquals(ConfigFileReaderWriter.ReloadOutcome.APPLIED, reader.loadConfigFromXML(xmlFile));
    }
}
