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
package com.hivemq.util;

import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

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

        //expect System.property to win
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
                "<test1><test2 id=\"VALUE1\"><test3>${ENV:VALUE1}</test3><test4>${ENV:VALUE2}</test4><test5>${ENV:VALUE3}</test5></test2></test1>";

        final String result = EnvVarUtil.replaceEnvironmentVariablePlaceholders(testString);

        final String expected =
                "<test1><test2 id=\"VALUE1\"><test3>value$1</test3><test4>2</test4><test5>value-_/!\"\\'3!§%&/()=?`*,;.:[]|{}</test5></test2></test1>";

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

}
