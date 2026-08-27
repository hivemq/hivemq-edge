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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hivemq.exceptions.UnrecoverableException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
            final var collected = EnvVarUtil.collectPlaceholders("<host>edge-${ENV:EDG882_UNIT_SITE}</host>")
                    .placeholders();

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
            final var collected = EnvVarUtil.collectPlaceholders("<keystore path=\"${ENV:EDG882_UNIT_DIR}/k.jks\"/>")
                    .placeholders();

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
                        .placeholders()
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
     * The branch R3-01 named. The span cannot be located -- the marshaller wrote the value somewhere the
     * restore is not anchored to -- but the credential is unmistakably still in the document. Returning it
     * is what the old code did; refusing the write is the only answer that keeps the secret off the disk,
     * and the write has not opened the file yet when this runs.
     * <p>
     * The value sits in an element of another name, which is what "unlocatable" means now that the span
     * tolerates attributes: an element written as {@code <password attr="x">} is located and restored, and
     * no longer reaches this branch.
     */
    @Test
    public void restorePlaceholders_whenACredentialIsPresentButUnlocatable_thenTheWriteIsRefused() {
        final var placeholder =
                new EnvVarUtil.ElementPlaceholder("password", "${ENV:PW}", "s3cr3t-do-not-write-me", false);

        assertThrows(
                UnrecoverableException.class,
                () -> EnvVarUtil.restorePlaceholders(
                        "<x><renamed-by-the-marshaller>s3cr3t-do-not-write-me</renamed-by-the-marshaller></x>",
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

        final String document = "<x><renamed-by-the-marshaller>shared.example.com</renamed-by-the-marshaller></x>";

        assertEquals(document, EnvVarUtil.restorePlaceholders(document, java.util.List.of(placeholder)));
    }

    // ---------------------------------------------------------------------------------------------
    // EDG-882 review v03, R3-08. The collector used to store the raw XML source span and compare it
    // with post-marshal output. XML parsing normalises the representation without changing the value,
    // so three ordinary ways of writing a placeholder rendered to something the restore could not find:
    // it took the "not in the document" branch and let the credential onto disk. Each of the three is
    // pinned here on the collector and again on the document the marshaller would have produced.
    // ---------------------------------------------------------------------------------------------

    private static final @NotNull String PW = "EDG882_UNIT_PW";
    private static final @NotNull String OTHER = "EDG882_UNIT_OTHER";
    private static final @NotNull String SECRET = "s3cr3t-do-not-write-me";

    private @NotNull EnvVarUtil.CollectedPlaceholders collectWithSecret(final @NotNull String xml) {
        System.setProperty(PW, SECRET);
        try {
            return EnvVarUtil.collectPlaceholders(xml);
        } finally {
            System.clearProperty(PW);
        }
    }

    /**
     * A numeric character reference. The parser turns it into the character it denotes long before JAXB
     * marshals anything, so a collector storing the source span searched the document for a string
     * containing the reference and never found it.
     */
    @Test
    public void collectPlaceholders_normalisesANumericCharacterReference() {
        final var collected = collectWithSecret("<password>prefix&#45;${ENV:" + PW + "}</password>");

        assertEquals(1, collected.placeholders().size());
        assertEquals(0, collected.unaccountedTokens());
        assertEquals("prefix-${ENV:" + PW + "}", collected.placeholders().get(0).literal());
        assertEquals("prefix-" + SECRET, collected.placeholders().get(0).value());
    }

    @Test
    public void restorePlaceholders_putsBackASpanWrittenWithACharacterReference() {
        final var collected = collectWithSecret("<password>prefix&#45;${ENV:" + PW + "}</password>");

        // What JAXB writes: the parsed value, with the reference already resolved.
        final String restored =
                EnvVarUtil.restorePlaceholders("<x><password>prefix-" + SECRET + "</password></x>", collected);

        assertFalse(restored.contains(SECRET), "the credential was written to config.xml");
        assertEquals("<x><password>prefix-${ENV:" + PW + "}</password></x>", restored);
    }

    /** CRLF padding. The parser normalises the line endings; the raw span kept the carriage returns. */
    @Test
    public void collectPlaceholders_normalisesCrlfPadding() {
        final var collected = collectWithSecret("<password>\r\n  ${ENV:" + PW + "}\r\n</password>");

        assertEquals(1, collected.placeholders().size());
        assertEquals(0, collected.unaccountedTokens());
        assertFalse(collected.placeholders().get(0).value().contains("\r"), "the carriage returns survived the parse");
        assertEquals("\n  " + SECRET + "\n", collected.placeholders().get(0).value());
    }

    @Test
    public void restorePlaceholders_putsBackASpanWrittenWithCrlfPadding() {
        final var collected = collectWithSecret("<password>\r\n  ${ENV:" + PW + "}\r\n</password>");

        final String restored =
                EnvVarUtil.restorePlaceholders("<x><password>\n  " + SECRET + "\n</password></x>", collected);

        assertFalse(restored.contains(SECRET), "the credential was written to config.xml");
        assertEquals("<x><password>\n  ${ENV:" + PW + "}\n</password></x>", restored);
    }

    /**
     * CDATA, which the old pattern could not match at all — so nothing was collected, the restore never
     * ran, and the secret went out with no message of any kind.
     */
    @Test
    public void collectPlaceholders_seesInsideACdataSection() {
        final var collected = collectWithSecret("<password><![CDATA[prefix-${ENV:" + PW + "}]]></password>");

        assertEquals(1, collected.placeholders().size(), "a placeholder inside CDATA was invisible to the collector");
        assertEquals(0, collected.unaccountedTokens());
        assertEquals("prefix-" + SECRET, collected.placeholders().get(0).value());
    }

    @Test
    public void restorePlaceholders_putsBackASpanWrittenInsideCdata() {
        final var collected = collectWithSecret("<password><![CDATA[prefix-${ENV:" + PW + "}]]></password>");

        final String restored =
                EnvVarUtil.restorePlaceholders("<x><password>prefix-" + SECRET + "</password></x>", collected);

        assertFalse(restored.contains(SECRET), "the credential was written to config.xml");
        assertEquals("<x><password>prefix-${ENV:" + PW + "}</password></x>", restored);
    }

    /** Single-quoted attributes are as valid as double-quoted ones; the old pattern matched only one. */
    @Test
    public void collectPlaceholders_seesASingleQuotedAttribute() {
        System.setProperty("EDG882_UNIT_DIR", "/etc/edge");
        try {
            final var collected = EnvVarUtil.collectPlaceholders("<keystore path='${ENV:EDG882_UNIT_DIR}/k.jks'/>");

            assertEquals(1, collected.placeholders().size());
            assertEquals(0, collected.unaccountedTokens());
            assertEquals("path", collected.placeholders().get(0).name());
            assertEquals("/etc/edge/k.jks", collected.placeholders().get(0).value());
            assertTrue(collected.placeholders().get(0).attribute());
        } finally {
            System.clearProperty("EDG882_UNIT_DIR");
        }
    }

    /**
     * A literal that needs escaping on the way back out. The collected literal is now parsed text, so an
     * operator's escaped ampersand is a bare one by the time it is restored; writing it raw would produce
     * a config.xml that does not parse on the next start.
     */
    @Test
    public void restorePlaceholders_escapesTheLiteralItWritesBack() {
        System.setProperty("EDG882_UNIT_SITE", "berlin");
        try {
            final var collected = EnvVarUtil.collectPlaceholders("<host>a&amp;b-${ENV:EDG882_UNIT_SITE}</host>");

            final String restored = EnvVarUtil.restorePlaceholders("<x><host>a&amp;b-berlin</host></x>", collected);

            assertEquals("<x><host>a&amp;b-${ENV:EDG882_UNIT_SITE}</host></x>", restored);
        } finally {
            System.clearProperty("EDG882_UNIT_SITE");
        }
    }

    /** A commented-out block is counted as seen so that it does not read as an unaccounted placeholder. */
    @Test
    public void collectPlaceholders_countsACommentedPlaceholderAsSeenButDoesNotRestoreIt() {
        final var collected = collectWithSecret("<x><!-- <password>${ENV:" + PW + "}</password> --></x>");

        assertEquals(0, collected.placeholders().size(), "a commented-out block is not part of the configuration");
        assertEquals(0, collected.unaccountedTokens(), "and it must not read as a placeholder that was missed");
    }

    /**
     * The safety net. The branch-by-branch reasoning can only speak for the spans the collector handed
     * over; a token it never saw has no branch at all, which is exactly how the three cases above got a
     * credential onto disk. So an unaccounted token refuses the write rather than assuming.
     */
    @Test
    public void restorePlaceholders_whenATokenIsUnaccountedFor_thenTheWriteIsRefused() {
        final var collected = new EnvVarUtil.CollectedPlaceholders(List.of(), 1);

        assertThrows(
                UnrecoverableException.class,
                () -> EnvVarUtil.restorePlaceholders("<x><host>testhost</host></x>", collected),
                "a placeholder the collector could not locate must refuse the write");
    }

    /** And nothing unaccounted for is the ordinary case, which must not refuse anything. */
    @Test
    public void restorePlaceholders_whenEveryTokenIsAccountedFor_thenTheWriteProceeds() {
        final var collected = new EnvVarUtil.CollectedPlaceholders(List.of(), 0);
        final String document = "<x><host>testhost</host></x>";

        assertEquals(document, EnvVarUtil.restorePlaceholders(document, collected));
    }

    /**
     * The last word, asked of the finished document rather than of the branch that produced it. Here the
     * restore succeeds on the {@code <password>} element and the same value is also written literally in a
     * second one, so the document still carries the credential when the restore believes it is done.
     */
    @Test
    public void restorePlaceholders_whenACredentialSurvivesTheRestore_thenTheWriteIsRefused() {
        final var placeholder = new EnvVarUtil.ElementPlaceholder("password", "${ENV:PW}", SECRET, false);

        assertThrows(
                UnrecoverableException.class,
                () -> EnvVarUtil.restorePlaceholders(
                        "<x><password>" + SECRET + "</password><note>" + SECRET + "</note></x>", List.of(placeholder)),
                "a credential still in the document when the restore finishes must refuse the write");
    }

    // ---------------------------------------------------------------------------------------------
    // EDG-882 review v04, finding 1.3. The accounting subtracted a count taken from the parsed document
    // from one taken from the file's bytes, and the two measure different populations: a placeholder
    // spelled with a character reference exists only in the second. The difference went negative, and the
    // guard refuses on "not zero" -- so a configuration with nothing wrong with it could never be written
    // again. It is compared per variable now, and only a shortfall counts.
    // ---------------------------------------------------------------------------------------------

    /** The reported case: one ordinary placeholder, one the raw file cannot see. Nothing is missing. */
    @Test
    public void collectPlaceholders_whenAPlaceholderIsSpelledWithACharacterReference_thenNothingIsUnaccountedFor() {
        System.setProperty(OTHER, "probe-host");
        try {
            final var collected = collectWithSecret(
                    "<x><host>${ENV:" + OTHER + "}</host><password>$&#123;ENV:" + PW + "}</password></x>");

            assertEquals(
                    0,
                    collected.unaccountedTokens(),
                    "a placeholder only the parser can see is not a placeholder the walk missed");
            assertEquals(2, collected.placeholders().size(), "both spans are still restorable");
        } finally {
            System.clearProperty(OTHER);
        }
    }

    /** And such a configuration can still be written, which is what the negative count took away. */
    @Test
    public void restorePlaceholders_whenAPlaceholderIsSpelledWithACharacterReference_thenTheWriteProceeds() {
        System.setProperty(OTHER, "probe-host");
        try {
            final var collected = collectWithSecret(
                    "<x><host>${ENV:" + OTHER + "}</host><password>$&#123;ENV:" + PW + "}</password></x>");

            // What the marshaller holds: the resolved host, and the literal text of the escaped token,
            // which the render never resolved because it works on the file's bytes.
            final String written = EnvVarUtil.restorePlaceholders(
                    "<x><host>probe-host</host><password>${ENV:" + PW + "}</password></x>", collected);

            assertEquals("<x><host>${ENV:" + OTHER + "}</host><password>${ENV:" + PW + "}</password></x>", written);
            assertFalse(written.contains(SECRET), "the escaped token's value was never rendered, so it cannot leak");
        } finally {
            System.clearProperty(OTHER);
        }
    }

    /**
     * The reason the totals were not simply clamped. A placeholder the walk genuinely could not account
     * for and one only the parser can see used to cancel each other out to zero, which reads as "every
     * token accounted for" — the one answer this guard must never give by accident.
     */
    @Test
    public void collectPlaceholders_whenAnUnaccountedTokenMeetsAnEscapedOne_thenTheyDoNotCancelOut() {
        final var collected =
                collectWithSecret("<x><note>${ENV:</note><password>$&#123;ENV:" + PW + "}</password></x>");

        assertEquals(1, collected.unaccountedTokens(), "a token this cannot name must stay unaccounted for");
        assertThrows(
                UnrecoverableException.class,
                () -> EnvVarUtil.restorePlaceholders("<x><note>${ENV:</note></x>", collected),
                "an unaccounted token must refuse the write whatever else the file holds");
    }

    /** A placeholder named twice is accounted for twice, not once. */
    @Test
    public void collectPlaceholders_countsEachOccurrenceOfTheSameVariable() {
        final var collected = collectWithSecret("<x><password>${ENV:" + PW + "}</password><keystore-password>${ENV:"
                + PW + "}</keystore-password></x>");

        assertEquals(0, collected.unaccountedTokens());
        assertEquals(2, collected.placeholders().size());
    }

    // ---------------------------------------------------------------------------------------------
    // EDG-882 review v04, finding 1.2. The span was built as <name>value</name> and compared literally,
    // so an element the marshaller wrote with an attribute was never found: a credential in one refused
    // every configuration write, and a non-secret in one had its placeholder replaced by its value. No
    // element in today's schema both carries an attribute and holds text -- the arbitrary XML in the file
    // is unmarshalled into maps, which drop attributes before this ever sees them -- so what is pinned
    // here is the trap, not a live disclosure.
    // ---------------------------------------------------------------------------------------------

    /** The element is located through its attributes, and keeps them. */
    @Test
    public void restorePlaceholders_putsBackASpanOnAnElementWithAnAttribute() {
        final var placeholder = new EnvVarUtil.ElementPlaceholder("password", "${ENV:PW}", SECRET, false);

        final String restored = EnvVarUtil.restorePlaceholders(
                "<x><password enc=\"aes\">" + SECRET + "</password></x>", List.of(placeholder));

        assertEquals("<x><password enc=\"aes\">${ENV:PW}</password></x>", restored);
    }

    /** Several attributes, in the order the marshaller wrote them, including one holding a right angle bracket. */
    @Test
    public void restorePlaceholders_keepsEveryAttributeOfTheElementItRestores() {
        final var placeholder = new EnvVarUtil.ElementPlaceholder("password", "${ENV:PW}", SECRET, false);

        final String restored = EnvVarUtil.restorePlaceholders(
                "<x><password enc=\"aes\" note=\"a>b\" id=\"7\">" + SECRET + "</password></x>", List.of(placeholder));

        assertEquals("<x><password enc=\"aes\" note=\"a>b\" id=\"7\">${ENV:PW}</password></x>", restored);
    }

    /** An attributed element and a bare one holding the same value are one group, and both are restored. */
    @Test
    public void restorePlaceholders_putsBackBothAnAttributedAndABareSpan() {
        final var placeholder = new EnvVarUtil.ElementPlaceholder("password", "${ENV:PW}", SECRET, false);
        final var second = new EnvVarUtil.ElementPlaceholder("password", "${ENV:PW}", SECRET, false);

        final String restored = EnvVarUtil.restorePlaceholders(
                "<x><password enc=\"aes\">" + SECRET + "</password><password>" + SECRET + "</password></x>",
                List.of(placeholder, second));

        assertEquals(
                "<x><password enc=\"aes\">${ENV:PW}</password><password>${ENV:PW}</password></x>",
                restored,
                "the count check must see both spans, or it refuses a document it can restore");
    }

    /** What is written in place of an unrestorable credential lands in the element, not on top of it. */
    @Test
    public void restorePlaceholders_poisonsAnAttributedElementWithoutLosingItsAttributes() {
        final var one = new EnvVarUtil.ElementPlaceholder("password", "${ENV:PW}", SECRET, false);
        final var other = new EnvVarUtil.ElementPlaceholder("password", "${ENV:OTHER_PW}", SECRET, false);

        final String restored = EnvVarUtil.restorePlaceholders(
                "<x><password enc=\"aes\">" + SECRET + "</password></x>", List.of(one, other));

        assertEquals("<x><password enc=\"aes\">${ENV:EDGE_UNRESTORED_SECRET}</password></x>", restored);
    }

    /** A start tag is not a place to look for another element: the pattern stays inside one of them. */
    @Test
    public void restorePlaceholders_doesNotMatchAcrossElements() {
        final var placeholder = new EnvVarUtil.ElementPlaceholder("password", "${ENV:PW}", SECRET, false);

        final String document = "<x><password>other</password><note>" + SECRET + "</note></x>";

        assertThrows(
                UnrecoverableException.class,
                () -> EnvVarUtil.restorePlaceholders(document, List.of(placeholder)),
                "the span must not be found by matching one element's tag against another's text");
    }

    // ---------------------------------------------------------------------------------------------
    // EDG-882 review v04, finding 2. That last word used to be asked of the serialised XML with
    // String.contains, which reads markup as content: a password of 'admin' was "still present" in any
    // configuration holding an <admin-api> element, so every write was refused from then on -- the node
    // stayed up and silently stopped being able to persist anything. It is asked of the parsed
    // document's element text and attribute values now.
    // ---------------------------------------------------------------------------------------------

    /** Sam's reproduction: the credential is an element name in the file, and nothing else. */
    @Test
    public void restorePlaceholders_whenAnElementNameContainsTheCredential_thenTheWriteProceeds() {
        final var placeholder = new EnvVarUtil.ElementPlaceholder("password", "${ENV:PW}", "admin", false);

        final String restored = EnvVarUtil.restorePlaceholders(
                "<x><admin-api><enabled>true</enabled></admin-api><password>admin</password></x>",
                List.of(placeholder));

        assertEquals(
                "<x><admin-api><enabled>true</enabled></admin-api><password>${ENV:PW}</password></x>",
                restored,
                "an element name that contains the credential is markup, not a disclosure");
    }

    /** And an ordinary value that has the credential inside it is the operator's own text, not the secret. */
    @Test
    public void restorePlaceholders_whenAnUnrelatedValueContainsTheCredential_thenTheWriteProceeds() {
        final var placeholder = new EnvVarUtil.ElementPlaceholder("password", "${ENV:PW}", "1", false);

        final String restored =
                EnvVarUtil.restorePlaceholders("<x><port>1883</port><password>1</password></x>", List.of(placeholder));

        assertEquals(
                "<x><port>1883</port><password>${ENV:PW}</password></x>",
                restored,
                "a port of 1883 is not a disclosure of a password of 1");
    }

    /**
     * The narrowing stops there. Inside a credential-bearing span, a value that merely contains the
     * secret is the concatenation case and is exactly the disclosure this exists to catch.
     */
    @Test
    public void restorePlaceholders_whenACredentialSurvivesInsideAnotherCredentialElement_thenTheWriteIsRefused() {
        final var placeholder = new EnvVarUtil.ElementPlaceholder("password", "${ENV:PW}", SECRET, false);

        assertThrows(
                UnrecoverableException.class,
                () -> EnvVarUtil.restorePlaceholders(
                        "<x><password>" + SECRET + "</password><keystore-password>prefix-" + SECRET
                                + "</keystore-password></x>",
                        List.of(placeholder)),
                "a credential left inside another credential element must refuse the write");
    }

    /** Attribute values are values too, and an attribute can be the credential-bearing span. */
    @Test
    public void restorePlaceholders_whenACredentialSurvivesInAnAttribute_thenTheWriteIsRefused() {
        final var placeholder = new EnvVarUtil.ElementPlaceholder("password", "${ENV:PW}", SECRET, false);

        assertThrows(
                UnrecoverableException.class,
                () -> EnvVarUtil.restorePlaceholders(
                        "<x><password>" + SECRET + "</password><keystore password=\"prefix-" + SECRET + "\"/></x>",
                        List.of(placeholder)),
                "a credential left in an attribute must refuse the write");
    }

    /**
     * A document that cannot be read back cannot be cleared of credentials either, and "cannot tell" is
     * not "there is none". The previous config.xml is intact and parses.
     */
    @Test
    public void restorePlaceholders_whenTheFinishedDocumentCannotBeParsed_thenTheWriteIsRefused() {
        final var placeholder = new EnvVarUtil.ElementPlaceholder("password", "${ENV:PW}", SECRET, false);

        assertThrows(
                UnrecoverableException.class,
                () -> EnvVarUtil.restorePlaceholders("<x><password>${ENV:PW}</password>", List.of(placeholder)),
                "a document this cannot parse must refuse the write rather than assume it is clean");
    }
}
