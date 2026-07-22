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
package com.hivemq.protocols.v2.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.adapter.sdk.api.v2.node.AccessTriState;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import java.io.StringReader;
import javax.xml.transform.stream.StreamSource;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * JAXB parsing of the {@code <v2>} section: the section parses, the documented defaults
 * apply when elements are omitted, and an old config carrying no section is unaffected. Uses a self-contained JAXB
 * context (the closed {@code config.xsd} {@code <xs:all>} is extended to admit {@code <v2>} when the
 * extractor is wired into the reader, a later task).
 */
class V2ProtocolAdaptersParsingTest {

    @Test
    void fullSection_parsesEveryField() throws JAXBException {
        final ProtocolAdapterEntity entity = parse("""
                <protocol-adapter>
                  <adapter-id>chaos-1</adapter-id>
                  <protocol-id>chaos</protocol-id>
                  <config-version>2</config-version>
                  <northbound-activated>true</northbound-activated>
                  <southbound-activated>false</southbound-activated>
                  <skip-verification>true</skip-verification>
                  <adapter-configuration>
                    <host>localhost</host>
                    <port>4840</port>
                  </adapter-configuration>
                  <retry-policy initial-millis="2000" factor="2.0" ceiling-millis="64000" maximum-retries="5"/>
                  <watchdog-timeout-millis>45000</watchdog-timeout-millis>
                  <command-timeout-millis>15000</command-timeout-millis>
                  <tags>
                    <tag>
                      <name>temperature</name>
                      <node-string>{"id":"ns=2;i=1"}</node-string>
                      <read-activated>true</read-activated>
                      <write-activated>false</write-activated>
                      <pollable>true</pollable>
                      <subscribable>false</subscribable>
                      <poll-interval-millis>3000</poll-interval-millis>
                      <access readable="YES" writable="NO" pollable="YES" subscribable="WILL_NOT_USE"/>
                    </tag>
                  </tags>
                  <northbound-mappings>
                    <northbound-mapping tag-name="temperature" topic="plant/a/temperature"/>
                  </northbound-mappings>
                  <southbound-mappings>
                    <southbound-mapping topic="plant/a/setpoint" tag-name="temperature"/>
                  </southbound-mappings>
                </protocol-adapter>
                """);

        assertThat(entity.getAdapterId()).isEqualTo("chaos-1");
        assertThat(entity.getProtocolId()).isEqualTo("chaos");
        assertThat(entity.getConfigVersion()).isEqualTo(2);
        assertThat(entity.isNorthboundActivated()).isTrue();
        assertThat(entity.isSouthboundActivated()).isFalse();
        assertThat(entity.isSkipVerification()).isTrue();
        assertThat(entity.getAdapterConfiguration()).containsKeys("host", "port");
        assertThat(entity.getWatchdogTimeoutMillis()).isEqualTo(45_000);
        assertThat(entity.getCommandTimeoutMillis()).isEqualTo(15_000);

        assertThat(entity.getRetryPolicy().getInitialMillis()).isEqualTo(2_000);
        assertThat(entity.getRetryPolicy().getFactor()).isEqualTo(2.0);
        assertThat(entity.getRetryPolicy().getCeilingMillis()).isEqualTo(64_000);
        assertThat(entity.getRetryPolicy().getMaximumRetries()).isEqualTo(5);

        assertThat(entity.getTags()).hasSize(1);
        final TagEntity tag = entity.getTags().getFirst();
        assertThat(tag.getName()).isEqualTo("temperature");
        assertThat(tag.getNodeString()).isEqualTo("{\"id\":\"ns=2;i=1\"}");
        assertThat(tag.isReadActivated()).isTrue();
        assertThat(tag.isWriteActivated()).isFalse();
        assertThat(tag.isPollable()).isTrue();
        assertThat(tag.isSubscribable()).isFalse();
        assertThat(tag.getPollIntervalMillis()).isEqualTo(3_000);
        assertThat(tag.getAccess().getReadable()).isEqualTo(AccessTriState.YES);
        assertThat(tag.getAccess().getWritable()).isEqualTo(AccessTriState.NO);
        assertThat(tag.getAccess().getPollable()).isEqualTo(AccessTriState.YES);
        assertThat(tag.getAccess().getSubscribable()).isEqualTo(AccessTriState.WILL_NOT_USE);

        assertThat(entity.getNorthboundMappings()).hasSize(1);
        assertThat(entity.getNorthboundMappings().getFirst().getTagName()).isEqualTo("temperature");
        assertThat(entity.getNorthboundMappings().getFirst().getTopic()).isEqualTo("plant/a/temperature");
        assertThat(entity.getSouthboundMappings()).hasSize(1);
        assertThat(entity.getSouthboundMappings().getFirst().getTopic()).isEqualTo("plant/a/setpoint");
        assertThat(entity.getSouthboundMappings().getFirst().getTagName()).isEqualTo("temperature");
    }

    @Test
    void minimalSection_appliesTheDocumentedDefaults() throws JAXBException {
        final ProtocolAdapterEntity entity = parse("""
                <protocol-adapter>
                  <adapter-id>chaos-min</adapter-id>
                  <protocol-id>chaos</protocol-id>
                  <tags>
                    <tag>
                      <name>t1</name>
                      <node-string>{}</node-string>
                    </tag>
                  </tags>
                </protocol-adapter>
                """);

        assertThat(entity.getConfigVersion()).isEqualTo(ProtocolAdapterEntity.DEFAULT_CONFIG_VERSION);
        assertThat(entity.isNorthboundActivated()).isTrue();
        assertThat(entity.isSouthboundActivated()).isTrue();
        assertThat(entity.isSkipVerification()).isFalse();
        assertThat(entity.getWatchdogTimeoutMillis()).isEqualTo(ProtocolAdapterEntity.DEFAULT_WATCHDOG_TIMEOUT_MILLIS);
        assertThat(entity.getCommandTimeoutMillis()).isEqualTo(ProtocolAdapterEntity.DEFAULT_COMMAND_TIMEOUT_MILLIS);
        assertThat(entity.getRetryPolicy()).isEqualTo(new RetryPolicyEntity());

        final TagEntity tag = entity.getTags().getFirst();
        // missing read-activated / write-activated default to true
        assertThat(tag.isReadActivated()).isTrue();
        assertThat(tag.isWriteActivated()).isTrue();
        // an omitted <access> element means "no access declaration" — unconstrained, so enforcement (EDG-824 #14)
        // never silently deactivates a tag whose config predates the access model
        assertThat(tag.getAccess()).isEqualTo(AccessFlagsEntity.unrestricted());
    }

    @Test
    void aConfigWithoutTheSection_yieldsAnEmptyList() {
        // touchpoint 1: absent ⇒ empty list, so old configs parse unchanged
        assertThat(new HiveMQConfigEntity().getV2().getProtocolAdapters()).isEmpty();
    }

    private static @NotNull ProtocolAdapterEntity parse(final @NotNull String xml) throws JAXBException {
        final JAXBContext context = JAXBContext.newInstance(ProtocolAdapterEntity.class);
        final Unmarshaller unmarshaller = context.createUnmarshaller();
        return unmarshaller
                .unmarshal(new StreamSource(new StringReader(xml)), ProtocolAdapterEntity.class)
                .getValue();
    }
}
