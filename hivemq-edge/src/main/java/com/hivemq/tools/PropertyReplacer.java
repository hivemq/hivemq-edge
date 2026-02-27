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
package com.hivemq.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PropertyReplacer {

    private static void parsePropertyString(String value, List<String> fragments, List<String> propertyRefs)
            throws IllegalStateException {
        int prev = 0;
        int pos;
        while ((pos = value.indexOf("$", prev)) >= 0) {
            if (pos > 0) {
                fragments.add(value.substring(prev, pos));
            }
            if (pos == (value.length() - 1)) {
                fragments.add("$");
                prev = pos + 1;
            } else if (value.charAt(pos + 1) != '{') {
                if (value.charAt(pos + 1) == '$') {
                    fragments.add("$");
                    prev = pos + 2;
                } else {
                    fragments.add(value.substring(pos, pos + 2));
                    prev = pos + 2;
                }
            } else {
                int endName = value.indexOf('}', pos);
                if (endName < 0) {
                    throw new IllegalStateException("Syntax error in property: " + value);
                }
                String propertyName = value.substring(pos + 2, endName);
                fragments.add(null);
                propertyRefs.add(propertyName);
                prev = endName + 1;
            }
        }
        if (prev < value.length()) {
            fragments.add(value.substring(prev));
        }
    }

    public static String replaceProperties(String value, Map keys) {

        List<String> fragments = new ArrayList<>();
        List<String> propertyRefs = new ArrayList<>();
        parsePropertyString(value, fragments, propertyRefs);

        StringBuilder sb = new StringBuilder();
        int refIndex = 0;

        for (String fragment : fragments) {
            if (fragment == null) {
                String propertyName = propertyRefs.get(refIndex++);
                Object replacement = null;
                if (keys != null) {
                    replacement = keys.get(propertyName);
                }
                fragment = (replacement != null) ? replacement.toString() : "${" + propertyName + "}";
            }
            sb.append(fragment);
        }
        return sb.toString();
    }
}
