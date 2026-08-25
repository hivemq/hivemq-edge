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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.api.PreLoginNoticeEntity;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
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

        // identical(), not similar(): XMLUnit calls a different child sequence a *recoverable* difference,
        // so a write-back that reordered the operator's <mqtt-topic-filter> elements satisfied similar()
        // -- which is one of the two regressions this test is named for (EDG-882 review v02, R2-19).
        //
        // Whitespace and comments are normalised first, or identical() fails on every indentation
        // difference between the operator's file and the marshaller's output: it compares child *indices*,
        // and a text node between two elements shifts all of them. Element order is what has to be
        // significant here, not formatting.
        //
        // The flags are static and global to the JVM, so they are restored -- Gradle runs many classes per
        // fork and a class that leaves them set changes what every later comparison means.
        //
        // The fixture's *elements* are in the order the marshaller emits them, deliberately: the write-back
        // reorders the children of <hivemq> and of <forwarded-topic> to the schema's order, which is
        // cosmetic churn of the operator's file and pre-existing, and leaving it in would make this test
        // fail for a reason it is not about. What is deliberately *not* canonical is the order of the two
        // <mqtt-topic-filter> elements inside <filters> -- that one has to survive verbatim, because a
        // reorder there reads as a changed bridge on the next reload and restarts it (EDG-882 F-07).
        final boolean previousIgnoreWhitespace = XMLUnit.getIgnoreWhitespace();
        final boolean previousIgnoreComments = XMLUnit.getIgnoreComments();
        final Diff diff;
        try {
            XMLUnit.setIgnoreWhitespace(true);
            XMLUnit.setIgnoreComments(true);
            diff = XMLUnit.compareXML(originalXml, copiedFileContent);
            if (!diff.identical()) {
                System.err.println("xml diff found " + diff);
                System.err.println(originalXml);
                System.err.println(copiedFileContent);
            }
            assertTrue(diff.identical(), "XML Content Should Match");
        } finally {
            XMLUnit.setIgnoreWhitespace(previousIgnoreWhitespace);
            XMLUnit.setIgnoreComments(previousIgnoreComments);
        }
    }

    /**
     * The write replaces {@code config.xml} with a file written beside it, and a new file carries the
     * process umask rather than the mode of the file it replaces. {@code config.xml} holds bridge
     * passwords, keystore passwords and client secrets, so widening a deliberately restricted file on
     * the first REST write of any subsystem would undo what the operator set — silently, and on a file
     * this branch is otherwise working to keep secrets out of.
     */
    @Test
    public void writeConfigWithSync_keepsTheModeOfTheFileItReplaces() throws IOException {
        final File tempFile = loadTestConfigFile();
        final Path path = tempFile.toPath();
        assumeTrue(
                path.getFileSystem().supportedFileAttributeViews().contains("posix"),
                "no POSIX permissions on this file system, so there is nothing to carry over");

        final Set<PosixFilePermission> restricted = PosixFilePermissions.fromString("rw-------");
        Files.setPosixFilePermissions(path, restricted);

        final ConfigFileReaderWriter configFileReader = createFileReaderWriter(tempFile);
        configFileReader.applyConfig();
        configFileReader.writeConfigWithSync();

        assertEquals(restricted, Files.getPosixFilePermissions(path), "the operator's mode must survive a write");
    }

    /**
     * A symbolic link is a path whose last component <em>is</em> the link, so replacing the path
     * replaces the link. Writing into the file followed it, which is what a mounted configuration
     * directory or an operator's link into a versioned tree relies on; the replace-by-move has to keep
     * doing that or the link is gone after the first write.
     */
    @Test
    public void writeConfigWithSync_writesThroughASymbolicLink() throws IOException {
        final File target = loadTestConfigFile();
        final Path link = target.toPath().resolveSibling("linked-config.xml");
        try {
            Files.createSymbolicLink(link, target.toPath());
        } catch (final UnsupportedOperationException | IOException e) {
            assumeTrue(false, "symbolic links are not available here: " + e.getMessage());
        }

        final ConfigFileReaderWriter configFileReader = createFileReaderWriter(link.toFile());
        configFileReader.applyConfig();
        configFileReader.writeConfigWithSync();

        assertTrue(Files.isSymbolicLink(link), "the link must still be a link");
        assertTrue(
                FileUtils.readFileToString(target, UTF_8).contains("<hivemq"),
                "the write must have gone through the link to the file it points at");
    }
}
