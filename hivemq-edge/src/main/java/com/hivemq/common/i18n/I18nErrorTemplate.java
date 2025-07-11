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

import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Singleton class to manage OpenAPI i18n error templates using FreeMarker.
 * It provides methods to retrieve error messages based on the current locale and template keys.
 * <p>
 * This class is thread-safe and uses a cache to store configurations for different locales.
 */
public final class I18nErrorTemplate {
    private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(I18nErrorTemplate.class);
    private final @NotNull Map<String, Configuration> configurationMap;
    private final @NotNull Function<Locale, String> resourceNameFunction;

    public I18nErrorTemplate(final @NotNull Function<Locale, String> resourceNameFunction) {
        configurationMap = new ConcurrentHashMap<>();
        this.resourceNameFunction = resourceNameFunction;
    }

    private Configuration createConfiguration(final @NotNull Locale locale) throws IOException {
        final Configuration configuration = new Configuration(Configuration.VERSION_2_3_34);
        final StringTemplateLoader stringTemplateLoader = new StringTemplateLoader();
        configuration.setDefaultEncoding(StandardCharsets.UTF_8.name());
        configuration.setTemplateLoader(stringTemplateLoader);
        configuration.setLocale(locale);
        final Properties properties = new Properties();
        final String resourceName = resourceNameFunction.apply(locale);
        try (final StringReader stringReader = new StringReader(IOUtils.resourceToString(resourceName,
                StandardCharsets.UTF_8))) {
            properties.load(stringReader);
        }
        for (final Map.Entry<Object, Object> entry : properties.entrySet()) {
            stringTemplateLoader.putTemplate(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }
        return configuration;
    }

    public @NotNull String get(final @NotNull I18nError i18NError) {
        return get(i18NError, Map.of());
    }

    public @NotNull String get(final @NotNull I18nError i18NError, final @NotNull Map<String, Object> map) {
        final Locale locale = I18nLocaleContext.getLocale();
        try {
            Configuration configuration = configurationMap.get(locale.toString());
            if (configuration == null) {
                // The whole configuration creation process is not thread-safe, but creating a new configuration
                // multiple times among concurrent threads brings no harm.
                // So, we don't use a lock here to improve the overall performance.
                configuration = createConfiguration(locale);
                configurationMap.put(locale.toString(), configuration);
            }
            final Template template = configuration.getTemplate(i18NError.getKey());
            try (final StringWriter stringWriter = new StringWriter()) {
                template.process(map, stringWriter);
                return stringWriter.toString();
            }
        } catch (final TemplateException e) {
            final String errorMessage =
                    "Error: Template " + i18NError.getKey() + " for " + locale + " could not be processed.";
            LOGGER.error(errorMessage, e);
            return errorMessage;
        } catch (final IOException e) {
            final String errorMessage =
                    "Error: Template " + i18NError.getKey() + " for " + locale + " could not be loaded.";
            LOGGER.error(errorMessage, e);
            return errorMessage;
        }
    }
}
