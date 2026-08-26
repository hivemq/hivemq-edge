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
package com.hivemq.util.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hivemq.exceptions.UnrecoverableException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

public class EnvVarUtilTest {

    @Test
    public void test_getValue_existing() throws Exception {

        final HashMap<String, String> map = new HashMap<>();
        map.put("TEST_EXISTING_ENVVAR", "iamset");
        setTempEnvVars(map);

        final String result = EnvVarUtil.getValue("TEST_EXISTING_ENVVAR");

        assertEquals("iamset", result);
    }

    @Test
    public void test_getValue_existing_java_prop() throws Exception {

        System.setProperty("test.existing.envvar", "iamset2");

        final String result = EnvVarUtil.getValue("test.existing.envvar");

        assertEquals("iamset2", result);
    }

    @Test
    public void test_getValue_existing_both() throws Exception {

        final HashMap<String, String> map = new HashMap<>();
        map.put("test.existing.both", "iamset");
        setTempEnvVars(map);

        System.setProperty("test.existing.both", "iamset2");

        final String result = EnvVarUtil.getValue("test.existing.both");

        // expect System.property to win
        assertEquals("iamset2", result);
    }

    @Test
    public void test_getValue_non_existing() throws Exception {

        final String result = EnvVarUtil.getValue("TEST_NON_EXISTING_ENVVAR");

        assertNull(result);
    }

    @Test
    public void test_replaceEnvironmentVariablePlaceholders() throws Exception {
        setTempEnvVars(Map.of("VALUE1", "value$1", "VALUE2", "2", "VALUE3", "value-_/!\"\\'3!§%&/()=?`*,;.:[]|{}"));

        final String testString =
                "<test1><test2 id=\"VALUE1\"><test3>${ENV:VALUE1}${FRAGMENT:FRAGGY}</test3><test4>${ENV:VALUE2}</test4><test5>${ENV:VALUE3}</test5></test2></test1>";

        final String result = EnvVarUtil.replaceEnvironmentVariablePlaceholders(testString);

        final String expected =
                "<test1><test2 id=\"VALUE1\"><test3>value$1${FRAGMENT:FRAGGY}</test3><test4>2</test4><test5>value-_/!\"\\'3!§%&/()=?`*,;.:[]|{}</test5></test2></test1>";

        assertEquals(expected, result);
    }

    @Test
    public void test_replaceEnvironmentVariablePlaceholders_withLegacyAtTheEnd_variablesReplacedCorrectly()
            throws Exception {
        setTempEnvVars(Map.of("VALUE1", "value$1", "VALUE2", "2", "VALUE3", "value-_/!\"\\'3!§%&/()=?`*,;.:[]|{}"));

        final String testString =
                "<test1><test2 id=\"VALUE1\"><test3>${ENV:VALUE1}</test3><test4>${ENV:VALUE2}</test4><test5>${ENV:VALUE3}</test5></test2></test1>";

        final String result = EnvVarUtil.replaceEnvironmentVariablePlaceholders(testString);

        final String expected =
                "<test1><test2 id=\"VALUE1\"><test3>value$1</test3><test4>2</test4><test5>value-_/!\"\\'3!§%&/()=?`*,;.:[]|{}</test5></test2></test1>";

        assertEquals(expected, result);
    }

    @Test
    public void test_replaceEnvironmentVariablePlaceholders_withLegacyAtTheBeginning_variablesReplacedCorrectly()
            throws Exception {
        setTempEnvVars(Map.of("VALUE1", "value$1", "VALUE2", "2", "VALUE3", "value-_/!\"\\'3!§%&/()=?`*,;.:[]|{}"));

        final String testString =
                "<test1><test2 id=\"VALUE1\"><test3>${ENV:VALUE1}</test3><test4>${ENV:VALUE2}</test4><test5>${ENV:VALUE3}</test5></test2></test1>";

        final String result = EnvVarUtil.replaceEnvironmentVariablePlaceholders(testString);

        final String expected =
                "<test1><test2 id=\"VALUE1\"><test3>value$1</test3><test4>2</test4><test5>value-_/!\"\\'3!§%&/()=?`*,;.:[]|{}</test5></test2></test1>";

        assertEquals(expected, result);
    }

    @Test
    public void test_replaceEnvironmentVariablePlaceholders_unknown_varname() throws Exception {

        setTempEnvVars(Map.of("VALUE1", "value"));

        final String testString = "<test1>${ENV:VALUE1}</test1><test2>${ENV:VALUE2}</test2>";

        assertThrows(UnrecoverableException.class, () -> {
            EnvVarUtil.replaceEnvironmentVariablePlaceholders(testString);
        });
    }

    /**
     * Modifies the in-memory map which is returned when System.getenv is called.
     * Does not set Env-Vars at all
     *
     * @param newenv the new Map which should be uses by System.getenv
     * @throws Exception
     */
    private void setTempEnvVars(final @NotNull Map<String, String> newenv) throws Exception {
        final Class[] classes = Collections.class.getDeclaredClasses();
        final Map<String, String> env = System.getenv();
        for (final Class cl : classes) {
            if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                final Field field = cl.getDeclaredField("m");
                field.setAccessible(true);
                final Object obj = field.get(env);
                final Map<String, String> map = (Map<String, String>) obj;
                map.clear();
                map.putAll(newenv);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // EDG-882 review v03, R3-01. The collection unit is the whole span -- an element's entire text or an
    // attribute's entire value -- rather than a placeholder that occupies all of it, which is what makes
    // a concatenation restorable. And the "cannot locate it" branch no longer returns a document that
    // still holds the credential.
    // ---------------------------------------------------------------------------------------------

    @Test
    public void collectPlaceholders_collectsAConcatenatedSpanWhole() {
        System.setProperty("EDG882_UNIT_SITE", "berlin");
        try {
            final var collected = EnvVarUtil.collectPlaceholders("<host>edge-${ENV:EDG882_UNIT_SITE}</host>");

            assertEquals(1, collected.size());
            assertEquals("host", collected.get(0).name());
            assertEquals("edge-${ENV:EDG882_UNIT_SITE}", collected.get(0).literal());
            assertEquals("edge-berlin", collected.get(0).value());
        } finally {
            System.clearProperty("EDG882_UNIT_SITE");
        }
    }

    @Test
    public void collectPlaceholders_collectsAnAttributeSpan() {
        System.setProperty("EDG882_UNIT_DIR", "/etc/edge");
        try {
            final var collected = EnvVarUtil.collectPlaceholders("<keystore path=\"${ENV:EDG882_UNIT_DIR}/k.jks\"/>");

            assertEquals(1, collected.size());
            assertEquals("path", collected.get(0).name());
            assertEquals("/etc/edge/k.jks", collected.get(0).value());
            assertEquals(true, collected.get(0).attribute());
        } finally {
            System.clearProperty("EDG882_UNIT_DIR");
        }
    }

    /** A span whose variable is unset is not collected: the whole-file render throws on it moments later. */
    @Test
    public void collectPlaceholders_skipsASpanWithAnUnsetVariable() {
        assertEquals(
                0,
                EnvVarUtil.collectPlaceholders("<host>edge-${ENV:EDG882_UNIT_NOT_SET}</host>")
                        .size());
    }

    @Test
    public void restorePlaceholders_putsAConcatenatedSpanBack() {
        final var placeholder = new EnvVarUtil.ElementPlaceholder("host", "edge-${ENV:SITE}", "edge-berlin", false);

        final String restored =
                EnvVarUtil.restorePlaceholders("<x><host>edge-berlin</host></x>", java.util.List.of(placeholder));

        assertEquals("<x><host>edge-${ENV:SITE}</host></x>", restored);
    }

    @Test
    public void restorePlaceholders_putsAnAttributeSpanBack() {
        final var placeholder = new EnvVarUtil.ElementPlaceholder("path", "${ENV:DIR}/k.jks", "/etc/edge/k.jks", true);

        final String restored =
                EnvVarUtil.restorePlaceholders("<keystore path=\"/etc/edge/k.jks\"/>", java.util.List.of(placeholder));

        assertEquals("<keystore path=\"${ENV:DIR}/k.jks\"/>", restored);
    }

    /**
     * The branch R3-01 named. The span cannot be located -- the marshaller wrote it in a form the
     * collector did not predict -- but the credential is unmistakably still in the document. Returning it
     * is what the old code did; refusing the write is the only answer that keeps the secret off the disk,
     * and the write has not opened the file yet when this runs.
     */
    @Test
    public void restorePlaceholders_whenACredentialIsPresentButUnlocatable_thenTheWriteIsRefused() {
        final var placeholder =
                new EnvVarUtil.ElementPlaceholder("password", "${ENV:PW}", "s3cr3t-do-not-write-me", false);

        assertThrows(
                UnrecoverableException.class,
                () -> EnvVarUtil.restorePlaceholders(
                        "<x><password xmlns=\"urn:other\">s3cr3t-do-not-write-me</password></x>",
                        java.util.List.of(placeholder)));
    }

    /**
     * And the other side of that judgement: when the value is genuinely gone from the document there is
     * nothing to leak, so the write proceeds. Refusing here would stop a node writing its configuration
     * because an element was removed.
     */
    @Test
    public void restorePlaceholders_whenTheSpanIsGoneAltogether_thenTheWriteProceeds() {
        final var placeholder =
                new EnvVarUtil.ElementPlaceholder("password", "${ENV:PW}", "s3cr3t-do-not-write-me", false);

        final String document = "<x><host>testhost</host></x>";

        assertEquals(document, EnvVarUtil.restorePlaceholders(document, java.util.List.of(placeholder)));
    }

    /** A non-secret that cannot be located keeps its value: losing an indirection is not a disclosure. */
    @Test
    public void restorePlaceholders_whenANonSecretIsPresentButUnlocatable_thenTheWriteProceeds() {
        final var placeholder =
                new EnvVarUtil.ElementPlaceholder("host", "${ENV:HOSTNAME}", "shared.example.com", false);

        final String document = "<x><host xmlns=\"urn:other\">shared.example.com</host></x>";

        assertEquals(document, EnvVarUtil.restorePlaceholders(document, java.util.List.of(placeholder)));
    }
}
