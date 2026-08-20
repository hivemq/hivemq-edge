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

import com.google.common.io.Files;
import java.io.IOException;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

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
    private static final @NotNull String SECRET = "s3cr3t-do-not-write-me";

    @AfterEach
    public void clearProperties() {
        System.clearProperty(PASSWORD_VAR);
        System.clearProperty("EDG882_OTHER");
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
     * The conservative half of the rule. When the same value also appears where the operator wrote it
     * literally, there is no way to tell the two apart in the rendered document, and rewriting the
     * literal one into a variable reference would be a defect of its own — so the placeholder is left
     * resolved instead, and the operator is told.
     */
    @Test
    public void whenTheValueAlsoAppearsLiterally_thenNothingIsRewrittenBlindly() throws IOException {
        System.setProperty(PASSWORD_VAR, "hivemq-edge"); // the same string the username uses

        final String written = loadAndWriteBack(config("${ENV:" + PASSWORD_VAR + "}"));

        assertThat(written)
                .as("the username the operator wrote literally must not become a variable reference")
                .contains("<username>hivemq-edge</username>");
        assertThat(written).doesNotContain("${ENV:" + PASSWORD_VAR + "}");
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
}
