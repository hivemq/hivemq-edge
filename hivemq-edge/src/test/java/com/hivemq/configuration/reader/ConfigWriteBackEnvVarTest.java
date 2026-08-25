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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.google.common.io.Files;
import com.hivemq.exceptions.UnrecoverableException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import util.LogbackCapturingAppender;

/**
 * What writing the configuration back out does to the operator's {@code ${ENV:...}} placeholders
 * (EDG-882 QA round 2).
 * <p>
 * Rendering happens once, on the whole file, before it is parsed, so the configuration Edge holds in
 * memory contains the values. Marshalling that back out — which any REST change to any subsystem does,
 * including one to an unrelated subsystem — replaced the placeholders with what they resolved to. A
 * bridge password supplied through an environment variable therefore ended up in {@code config.xml} in
 * plain text, and the indirection the operator chose was gone for good.
 * <p>
 * {@link com.hivemq.util.render.EnvVarUtil#getValue} reads Java system properties before the
 * environment, which is how these tests supply the values.
 */
public class ConfigWriteBackEnvVarTest extends AbstractConfigurationTest {

    private static final @NotNull String PASSWORD_VAR = "EDG882_BRIDGE_PW";
    private static final @NotNull String OTHER_VAR = "EDG882_OTHER";
    private static final @NotNull String SECRET = "s3cr3t-do-not-write-me";

    /** What is written in place of a credential whose placeholder could not be restored. */
    private static final @NotNull String UNRESTORED = "${ENV:EDGE_UNRESTORED_SECRET}";

    private @NotNull LogbackCapturingAppender logCapture;

    @BeforeEach
    public void captureTheRestoreLog() {
        logCapture = LogbackCapturingAppender.Factory.weaveInto(
                LoggerFactory.getLogger(com.hivemq.util.render.EnvVarUtil.class));
    }

    @AfterEach
    public void clearProperties() {
        LogbackCapturingAppender.Factory.cleanUp();
        System.clearProperty(PASSWORD_VAR);
        System.clearProperty(OTHER_VAR);
    }

    private static @NotNull String config(final @NotNull String password) {
        return "" + "<hivemq>\n"
                + "<mqtt-bridges>\n"
                + "    <mqtt-bridge>\n"
                + "        <id>edg-882-env-bridge</id>\n"
                + "        <remote-broker>\n"
                + "            <host>testhost</host>\n"
                + "            <authentication>\n"
                + "                <mqtt-simple-authentication>\n"
                + "                    <username>hivemq-edge</username>\n"
                + "                    <password>"
                + password
                + "</password>\n"
                + "                </mqtt-simple-authentication>\n"
                + "            </authentication>\n"
                + "        </remote-broker>\n"
                + "        <forwarded-topics>\n"
                + "            <forwarded-topic>\n"
                + "                <filters>\n"
                + "                    <mqtt-topic-filter>plant/#</mqtt-topic-filter>\n"
                + "                </filters>\n"
                + "                <destination>{#}</destination>\n"
                + "                <max-qos>1</max-qos>\n"
                + "            </forwarded-topic>\n"
                + "        </forwarded-topics>\n"
                + "    </mqtt-bridge>\n"
                + "</mqtt-bridges>"
                + "</hivemq>";
    }

    private @NotNull String loadAndWriteBack(final @NotNull String configXml) throws IOException {
        Files.write(configXml.getBytes(UTF_8), xmlFile);
        reader.applyConfig();

        reader.writeConfigWithSync();

        return java.nio.file.Files.readString(Path.of(xmlFile.getPath()));
    }

    @Test
    public void whenAPasswordComesFromAnEnvironmentVariable_thenItIsNotWrittenToTheFile() throws IOException {
        System.setProperty(PASSWORD_VAR, SECRET);

        final String written = loadAndWriteBack(config("${ENV:" + PASSWORD_VAR + "}"));

        assertThat(written).doesNotContain(SECRET);
        assertThat(written).contains("${ENV:" + PASSWORD_VAR + "}");
    }

    /** And the configuration still means the same thing after the round trip. */
    @Test
    public void whenTheFileIsWrittenBack_thenTheRenderedConfigurationIsUnchanged() throws IOException {
        System.setProperty(PASSWORD_VAR, SECRET);
        Files.write(config("${ENV:" + PASSWORD_VAR + "}").getBytes(UTF_8), xmlFile);
        reader.applyConfig();
        final String passwordAsRead = bridgeConfiguration.getBridges().get(0).getPassword();

        reader.writeConfigWithSync();
        reader.applyConfig();

        assertThat(passwordAsRead).isEqualTo(SECRET);
        assertThat(bridgeConfiguration.getBridges().get(0).getPassword()).isEqualTo(SECRET);
    }

    /** A value the operator wrote literally is not turned into a variable reference. */
    @Test
    public void whenAValueWasNotAPlaceholder_thenItIsWrittenOutUnchanged() throws IOException {
        final String written = loadAndWriteBack(config("literal-password"));

        assertThat(written).contains("literal-password");
        assertThat(written).doesNotContain("${ENV:");
    }

    /**
     * A value that also appears where the operator wrote it literally, in a different element.
     * <p>
     * Restoration is anchored to the element the placeholder occupied, so the two are distinguishable
     * and both are handled correctly: the password goes back to being a variable reference and the
     * username stays exactly as the operator wrote it. Matching on the value alone could not tell them
     * apart — it either left the secret in the file or rewrote the username into a variable reference
     * nobody asked for (EDG-882 QA round 3).
     */
    @Test
    public void whenTheValueAlsoAppearsInAnotherElement_thenOnlyThePlaceholdersElementIsRestored() throws IOException {
        System.setProperty(PASSWORD_VAR, "hivemq-edge"); // the same string the username uses

        final String written = loadAndWriteBack(config("${ENV:" + PASSWORD_VAR + "}"));

        assertThat(written)
                .as("the username the operator wrote literally must not become a variable reference")
                .contains("<username>hivemq-edge</username>");
        assertThat(written)
                .as("the password came from a variable and must go back to being one")
                .contains("<password>${ENV:" + PASSWORD_VAR + "}</password>");
    }

    /**
     * A placeholder in a commented-out block is not part of the configuration and never reaches the
     * marshalled document; it must not make the live one look ambiguous. Commenting a bridge out is an
     * ordinary thing for an operator to do (EDG-882 QA round 3).
     */
    @Test
    public void whenACommentedOutBlockRepeatsThePlaceholder_thenTheLiveOneIsStillRestored() throws IOException {
        System.setProperty(PASSWORD_VAR, SECRET);
        final String placeholder = "${ENV:" + PASSWORD_VAR + "}";
        final String configXml = config(placeholder)
                .replace("<mqtt-bridges>", "<mqtt-bridges>\n<!-- <password>" + placeholder + "</password> -->");

        Files.write(configXml.getBytes(UTF_8), xmlFile);
        reader.applyConfig();
        reader.writeConfigWithSync();

        final String written = java.nio.file.Files.readString(Path.of(xmlFile.getPath()));
        assertThat(written).doesNotContain(SECRET);
        assertThat(written).contains("<password>" + placeholder + "</password>");
    }

    /** Two elements fed by the same variable are both restored — the count matches. */
    @Test
    public void whenOneVariableFeedsTwoElements_thenBothArePutBack() throws IOException {
        System.setProperty(PASSWORD_VAR, SECRET);
        final String placeholder = "${ENV:" + PASSWORD_VAR + "}";
        final String configXml =
                config(placeholder).replace("<host>testhost</host>", "<host>" + placeholder + "</host>");

        Files.write(configXml.getBytes(UTF_8), xmlFile);
        reader.applyConfig();
        reader.writeConfigWithSync();
        final String written = java.nio.file.Files.readString(Path.of(xmlFile.getPath()));

        assertThat(written).doesNotContain(SECRET);
        assertThat(written).contains("<host>" + placeholder + "</host>");
        assertThat(written).contains("<password>" + placeholder + "</password>");
    }

    /**
     * The escaping path, and what it turns out to be worth (EDG-882 review v02, R2-20).
     * <p>
     * The review asked for a value carrying the three characters a marshaller escapes in element text,
     * on the reasoning that restoration finds the element by rebuilding the string JAXB wrote and so
     * has to escape it the same way — and that "finds nothing" means the credential is written out.
     * Writing the test showed the case cannot arise from an environment variable at all:
     * {@code replaceEnvironmentVariablePlaceholders} splices the value into the document as raw text
     * before it is parsed, escaping only the regex metacharacters {@code \} and {@code $}. A value
     * carrying {@code &}, {@code <} or {@code >} therefore produces XML that is not well formed, and
     * the configuration fails to <em>read</em> — long before any of this.
     * <p>
     * So {@code EnvVarUtil.escapeXmlText} is unreachable through this path and defensive only. This is
     * pinned rather than left implicit: it is a pre-existing defect on the inbound side, out of scope
     * here, and whoever fixes it should come back to the write-back side at the same time — at which
     * point this test fails and says so.
     */
    @Test
    public void whenAnEnvironmentValueCarriesXmlSpecialCharacters_thenTheConfigurationCannotEvenBeRead()
            throws IOException {
        System.setProperty(PASSWORD_VAR, "p&ss<w>rd");
        Files.write(config("${ENV:" + PASSWORD_VAR + "}").getBytes(UTF_8), xmlFile);

        assertThatThrownBy(reader::applyConfig)
                .as("an env value with XML special characters is spliced in unescaped, so the file is malformed")
                .isInstanceOf(UnrecoverableException.class);
    }

    // ---------------------------------------------------------------------------------------------
    // The three ways the restore gives up, and what each one does with the value (R2-04).
    //
    // Anchoring to an element name and checking counts is sound when the counts line up and ambiguous
    // when they do not, and no reading of an ambiguous case is right: by the time the document is
    // marshalled, an element holding a resolved variable and one holding a literal that happens to
    // equal it are the same bytes. So the ambiguous case is decided by which mistake can be undone.
    // Writing a credential to disk cannot be undone; replacing one the operator wrote literally can,
    // from the rolling backup the write takes first.
    // ---------------------------------------------------------------------------------------------

    /**
     * Bail-out 1: two variables resolving to the same value in the same element name. Neither can be
     * told from the other, so neither is restored — and because the element holds a credential, neither
     * is written out either.
     */
    @Test
    public void whenTwoVariablesFillTheSameElementWithTheSameValue_thenNeitherSecretIsWritten() throws IOException {
        System.setProperty(PASSWORD_VAR, SECRET);
        System.setProperty(OTHER_VAR, SECRET); // a different variable, the same resolved value

        final String written = loadAndWriteBack(twoBridges("${ENV:" + PASSWORD_VAR + "}", "${ENV:" + OTHER_VAR + "}"));

        assertThat(written).as("the credential must not reach the disk").doesNotContain(SECRET);
        assertThat(written).contains("<password>" + UNRESTORED + "</password>");
        assertThat(errorsFromTheRestore())
                .as("giving up on a credential must be an error, not a warning")
                .anySatisfy(message -> assertThat(message).contains("holds a credential"));
    }

    /**
     * Bail-out 3: the counts disagree, because one bridge takes the value from a variable and the other
     * has the same string written literally. Which occurrence came from the variable cannot be told, so
     * both are poisoned. That does cost the operator the literal they wrote — deliberately, and it is
     * the one direction that is recoverable: the previous file is in the rolling backup, whereas a
     * credential written to config.xml is only fixed by rotating it.
     */
    @Test
    public void whenAnotherElementHoldsTheSameValueLiterally_thenTheSecretIsStillNotWritten() throws IOException {
        System.setProperty(PASSWORD_VAR, SECRET);

        final String written = loadAndWriteBack(twoBridges("${ENV:" + PASSWORD_VAR + "}", SECRET));

        assertThat(written).as("the credential must not reach the disk").doesNotContain(SECRET);
        assertThat(written).contains("<password>" + UNRESTORED + "</password>");
    }

    /**
     * Bail-out 2: the element the placeholder filled is not in the document as written, because the
     * marshaller normalised the value on the way out. Only a typed field does that, so it is never a
     * credential — nothing is poisoned, and the point of the case is that it is reported.
     */
    @Test
    public void whenTheMarshallerNormalisesTheValue_thenItIsReportedAndNothingIsPoisoned() throws IOException {
        System.setProperty(OTHER_VAR, "01883"); // <port> is an int in the schema: JAXB writes 1883

        final String written = loadAndWriteBack(config("literal-password")
                .replace("<host>testhost</host>", "<host>testhost</host><port>${ENV:" + OTHER_VAR + "}</port>"));

        assertThat(written).contains("<port>1883</port>");
        assertThat(written)
                .as("a port is not a credential and must not be poisoned")
                .doesNotContain(UNRESTORED);
        assertThat(errorsFromTheRestore())
                .anySatisfy(message -> assertThat(message).contains("is not in the configuration being written"));
    }

    /**
     * And a non-credential that cannot be restored keeps its value. Losing the indirection is worth an
     * error, but stopping a node from starting over a {@code <host>} would be answering a disclosure
     * that did not happen.
     */
    @Test
    public void whenANonSecretCannotBeRestored_thenItsValueIsWrittenOut() throws IOException {
        System.setProperty(OTHER_VAR, "shared.example.com");

        final String written = loadAndWriteBack(twoBridges("literal-password", "literal-password")
                .replace("<host>testhost</host>", "<host>${ENV:" + OTHER_VAR + "}</host>")
                .replace("<host>testhost2</host>", "<host>shared.example.com</host>"));

        assertThat(written).contains("<host>shared.example.com</host>");
        assertThat(written).doesNotContain(UNRESTORED);
        assertThat(errorsFromTheRestore())
                .anySatisfy(message -> assertThat(message).contains("variable reference is lost"));
    }

    /**
     * A placeholder written with whitespace around it (EDG-882 review v02, R2-20).
     * <p>
     * {@code collectPlaceholders} allows it — {@code <password>\s*${ENV:X}\s*</password>} — because an
     * operator formatting their file that way is ordinary. The value the element resolves to is the
     * trimmed one either way, so the restore has to put back the placeholder and not the padding, and the
     * credential still has to stay off the disk.
     */
    @Test
    public void whenThePlaceholderIsWrittenWithSurroundingWhitespace_thenItIsStillRestored() throws IOException {
        System.setProperty(PASSWORD_VAR, SECRET);

        final String written = loadAndWriteBack(config("\n            ${ENV:" + PASSWORD_VAR + "}\n        "));

        assertThat(written).as("the credential must not reach the disk").doesNotContain(SECRET);
        assertThat(written).contains("${ENV:" + PASSWORD_VAR + "}");
    }

    private @NotNull List<String> errorsFromTheRestore() {
        return logCapture.getCapturedLogs().stream()
                .filter(event -> event.getLevel() == Level.ERROR)
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }

    /** Two bridges, so that one element name can carry two values or two variables. */
    private static @NotNull String twoBridges(final @NotNull String first, final @NotNull String second) {
        return config(first).replace("</mqtt-bridges>", secondBridge(second) + "</mqtt-bridges>");
    }

    private static @NotNull String secondBridge(final @NotNull String password) {
        return config(password)
                .replace("<hivemq>\n<mqtt-bridges>\n", "")
                .replace("</mqtt-bridges></hivemq>", "")
                .replace("edg-882-env-bridge", "edg-882-env-bridge-2")
                .replace("testhost", "testhost2")
                .replace("plant/#", "plant2/#");
    }
}
