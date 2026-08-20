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
package com.hivemq.configuration.writer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.api.PreLoginNoticeEntity;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;
import wiremock.org.custommonkey.xmlunit.Diff;
import wiremock.org.custommonkey.xmlunit.XMLUnit;

@SuppressWarnings("NullabilityAnnotations")
public class ConfigFileWriterTest extends AbstractConfigWriterTest {

    @Test
    public void rewriteUnchangedConfigurationYieldsSameXML() throws IOException, SAXException {

        final File tempFile = loadTestConfigFile();
        final String originalXml = FileUtils.readFileToString(tempFile, UTF_8);

        final ConfigFileReaderWriter configFileReader = createFileReaderWriter(tempFile);
        final HiveMQConfigEntity entity = configFileReader.applyConfig();

        final PreLoginNoticeEntity notice = entity.getApiConfig().getPreLoginNotice();
        Assertions.assertNotNull(notice);

        // writeConfigWithSync, not writeConfigToXML: only this one runs the extractors' sync step, and
        // that step is where the entity is rebuilt from the runtime objects. Marshalling the entity
        // straight back out compares the JAXB round trip with itself and cannot fail -- so this test
        // passed while a write-back was silently dropping a bridge's <queue-limit> and reordering the
        // operator's topic filters (EDG-882 QA round 2). This is the path production takes on every
        // REST write of any subsystem.
        configFileReader.writeConfigWithSync();

        final String copiedFileContent = FileUtils.readFileToString(tempFile, UTF_8);

        final Diff diff = XMLUnit.compareXML(originalXml, copiedFileContent);
        if (!diff.identical()) {
            System.err.println("xml diff found " + diff);
            System.err.println(originalXml);
            System.err.println(copiedFileContent);
        }
        assertTrue(diff.similar(), "XML Content Should Match");
    }
}
