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

import com.hivemq.configuration.info.SystemInformation;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfigFileReaderWriterTest {

    @Test
    public void test_alltags() throws Exception{
        final var systemInformation = mock(SystemInformation.class);
        when(systemInformation.isConfigFragmentBase64Zip()).thenReturn(false);
        final var reader = new ConfigFileReaderWriter(systemInformation, null, List.of());
        final var configFile = new File(getClass().getClassLoader().getResource("configs/testing/alltags.xml").toURI());
        final var configEntity = reader.readConfigFromXML(configFile);
        assertThat(configEntity).isNotNull();
    }

    @Test
    public void test_empty() throws Exception{
        final var systemInformation = mock(SystemInformation.class);
        when(systemInformation.isConfigFragmentBase64Zip()).thenReturn(false);
        final var reader = new ConfigFileReaderWriter(systemInformation, null, List.of());
        final var configFile = new File(getClass().getClassLoader().getResource("configs/testing/empty.xml").toURI());
        final var configEntity = reader.readConfigFromXML(configFile);
        assertThat(configEntity).isNotNull();
    }

    @Test
    public void test_datacombiners_no_source() throws Exception{
        final var systemInformation = mock(SystemInformation.class);
        when(systemInformation.isConfigFragmentBase64Zip()).thenReturn(false);
        final var reader = new ConfigFileReaderWriter(systemInformation, null, List.of());
        final var configFile = new File(getClass().getClassLoader().getResource("configs/testing/datacombiners_no_source.xml").toURI());
        final var configEntity = reader.readConfigFromXML(configFile);
        //This will break as soon as the xsd is fixed
        assertThat(configEntity).isNotNull();
    }

}
