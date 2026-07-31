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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hivemq.configuration.info.SystemInformation;
import java.io.File;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConfigFileReaderWriterTest {

    @Test
    public void test_alltags() throws Exception {
        final var systemInformation = mock(SystemInformation.class);
        when(systemInformation.isConfigFragmentBase64Zip()).thenReturn(false);
        final var reader = new ConfigFileReaderWriter(systemInformation, null, List.of());
        final var configFile = new File(getClass()
                .getClassLoader()
                .getResource("configs/testing/alltags.xml")
                .toURI());
        final var configEntity = reader.loadConfigFromXML(configFile);
        assertThat(configEntity).isTrue();
    }

    @Test
    public void test_empty() throws Exception {
        final var systemInformation = mock(SystemInformation.class);
        when(systemInformation.isConfigFragmentBase64Zip()).thenReturn(false);
        final var reader = new ConfigFileReaderWriter(systemInformation, null, List.of());
        final var configFile = new File(getClass()
                .getClassLoader()
                .getResource("configs/testing/empty.xml")
                .toURI());
        final var configEntity = reader.loadConfigFromXML(configFile);
        assertThat(configEntity).isTrue();
    }

    @Test
    public void test_datacombiners_no_source() throws Exception {
        final var systemInformation = mock(SystemInformation.class);
        when(systemInformation.isConfigFragmentBase64Zip()).thenReturn(false);
        final var reader = new ConfigFileReaderWriter(systemInformation, null, List.of());
        final var configFile = new File(getClass()
                .getClassLoader()
                .getResource("configs/testing/datacombiners_no_source.xml")
                .toURI());
        final var configEntity = reader.loadConfigFromXML(configFile);
        // This will break as soon as the xsd is fixed
        assertThat(configEntity).isFalse();
    }

    @Test
    public void disabledOidcStanza_roundTripsWithoutSyncFailure() throws Exception {
        // A pre-staged, disabled <oidc-authentication> (no <role-mappings>) must be readable AND writable:
        // the config-file sync re-marshals the whole config on every change, so if this cannot be written back
        // the change is lost on restart. The entity must not emit an empty <role-mappings></role-mappings>
        // (which the schema rejects); it must omit the element entirely when there are no mappings.
        final var systemInformation = mock(SystemInformation.class);
        when(systemInformation.isConfigFragmentBase64Zip()).thenReturn(false);
        final var reader = new ConfigFileReaderWriter(systemInformation, null, List.of());
        final var configFile = new File(getClass()
                .getClassLoader()
                .getResource("configs/testing/oidc_disabled_no_role_mappings.xml")
                .toURI());

        assertThat(reader.loadConfigFromXML(configFile)).isTrue();

        // Re-marshal (this is what config sync does). The marshaller validates against the schema, so an empty
        // <role-mappings/> would throw here. It must succeed and omit the element.
        final var writer = new java.io.StringWriter();
        reader.writeConfigToXML(writer);
        assertThat(writer.toString()).doesNotContain("<role-mappings");
    }
}
