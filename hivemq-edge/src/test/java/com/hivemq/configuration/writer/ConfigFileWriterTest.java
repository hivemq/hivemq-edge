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

import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.reader.ConfigurationFile;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;
import wiremock.org.custommonkey.xmlunit.Diff;
import wiremock.org.custommonkey.xmlunit.XMLUnit;

import java.io.File;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;

@SuppressWarnings("NullabilityAnnotations")
public class ConfigFileWriterTest extends AbstractConfigWriterTest {


    @Test
    public void rewriteUnchangedConfigurationYieldsSameXML() throws IOException, SAXException {

        File tempFile = loadTestConfigFile();
        String originalXml = FileUtils.readFileToString(tempFile, UTF_8);

        final ConfigFileReaderWriter configFileReader = createFileReaderWriter(tempFile);
        HiveMQConfigEntity hiveMQConfigEntity = configFileReader.applyConfig();

        final File tempCopyFile = new File(System.getProperty("java.io.tmpdir"), "copy-config.xml");
        tempFile.deleteOnExit();
        configFileReader.writeConfig(new ConfigurationFile(tempCopyFile), false);

        String copiedFileContent = FileUtils.readFileToString(tempCopyFile, UTF_8);
        Diff diff = XMLUnit.compareXML(originalXml, copiedFileContent);
        if(!diff.identical()){
            System.err.println("xml diff found " + diff);
//            System.err.println(originalXml);
//            System.err.println(copiedFileContent);
        }
        Assert.assertTrue("XML Content Should Match", diff.similar());

    }
}
