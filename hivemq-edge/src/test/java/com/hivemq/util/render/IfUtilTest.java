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

import com.hivemq.exceptions.UnrecoverableException;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

public class IfUtilTest {

    @Test
    public void test_getValue_existing() throws Exception {

        final HashMap<String, String> map = new HashMap<>();
        map.put("HIVEMQ_MQTTS_ENABLED", "false");
        setTempEnvVars(map);

        final String testString = "${IF:HIVEMQ_MQTTS_ENABLED}hallo${IF:HIVEMQ_MQTTS_ENABLED}welt${IF:!HIVEMQ_HTTPS_ENABLED}!!!${IF:!HIVEMQ_HTTPS_ENABLED}9876543210";

        final String result = IfUtil.replaceIfPlaceHolders(testString);

        assertEquals("welt!!!9876543210", result);
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
