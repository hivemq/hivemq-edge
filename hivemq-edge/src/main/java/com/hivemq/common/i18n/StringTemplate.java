/*
 *  Copyright 2019-present HiveMQ GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.hivemq.common.i18n;

import freemarker.template.Configuration;
import freemarker.template.Template;
import org.jetbrains.annotations.NotNull;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

public final class StringTemplate {
    private final static @NotNull Configuration CONFIGURATION = new Configuration(Configuration.VERSION_2_3_22);

    static {
        CONFIGURATION.setDefaultEncoding(StandardCharsets.UTF_8.name());
        CONFIGURATION.setLocale(Locale.US);
    }

    private StringTemplate() {
    }

    public static @NotNull String format(
            final @NotNull String stringTemplate,
            final @NotNull Map<String, Object> arguments) {
        try (final StringWriter stringWriter = new StringWriter();) {
            final Template template = new Template(stringTemplate, stringTemplate, CONFIGURATION);
            template.process(arguments, stringWriter);
            return stringWriter.toString();
        } catch (final Exception e) {
            return e.getMessage();
        }
    }
}
