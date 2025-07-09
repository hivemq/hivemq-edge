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

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class I18nHttpErrorTest {
    @BeforeEach
    public void setUp() {
        I18nLocaleContext.setLocale(Locale.US);
    }

    @AfterEach
    public void tearDown() {
        I18nLocaleContext.setLocale(I18nLocaleContext.DEFAULT_LOCALE);
    }

    @Test
    public void whenLocaleIsEnUS_thenErrorCountShouldMatch() throws IOException {
        final List<I18nHttpError> errors = Arrays.asList(I18nHttpError.values());
        assertThat(errors.size()).isGreaterThan(0);
        final Properties properties = new Properties();
        try (final StringReader stringReader = new StringReader(IOUtils.resourceToString(
                "/templates/http-errors-en_US.properties",
                StandardCharsets.UTF_8))) {
            properties.load(stringReader);
        }
        final Set<Object> propertyKeySet = properties.keySet();
        final Set<String> errorKeySet = errors.stream().map(I18nHttpError::getKey).collect(Collectors.toSet());
        propertyKeySet.forEach(key -> {
            assertThat(key).isInstanceOf(String.class);
            assertThat(errorKeySet.contains((String) key)).as(key + " is not found in the enum.").isTrue();
        });
        errorKeySet.forEach(key -> assertThat(propertyKeySet.contains(key)).as(key + " is not found in the properties.")
                .isTrue());
    }

    @Test
    public void whenLocaleIsEnUS_thenHttpError400ShouldWork() {
        assertThat(I18nHttpError.HTTP_ERROR_400_INVALID_QUERY_PARAMETER_TITLE.get()).isEqualTo(
                "Query Parameter is Invalid");
        assertThat(I18nHttpError.HTTP_ERROR_400_INVALID_QUERY_PARAMETER_DETAIL.get(Map.of("parameter",
                "p1",
                "reason",
                "test."))).isEqualTo("Query parameter 'p1' is invalid: test.");
        assertThat(I18nHttpError.HTTP_ERROR_400_REQUEST_BODY_MISSING_TITLE.get()).isEqualTo(
                "Required Request Body Missing");
        assertThat(I18nHttpError.HTTP_ERROR_400_REQUEST_BODY_MISSING_DETAIL.get()).isEqualTo(
                "Required request body is missing.");
        assertThat(I18nHttpError.HTTP_ERROR_400_REQUEST_BODY_PARAMETER_MISSING_TITLE.get()).isEqualTo(
                "Required Request Body Parameter Missing");
        assertThat(I18nHttpError.HTTP_ERROR_400_REQUEST_BODY_PARAMETER_MISSING_DETAIL.get(Map.of("parameter",
                "p1"))).isEqualTo("Required request body parameter 'p1' is missing.");
        assertThat(I18nHttpError.HTTP_ERROR_400_URL_PARAMETER_MISSING_TITLE.get()).isEqualTo(
                "Required URL Parameter Missing");
        assertThat(I18nHttpError.HTTP_ERROR_400_URL_PARAMETER_MISSING_DETAIL.get(Map.of("parameter", "p1"))).isEqualTo(
                "Required URL parameter 'p1' is missing.");
    }

    @Test
    public void whenLocaleIsEnUS_thenHttpError412ShouldWork() {
        assertThat(I18nHttpError.HTTP_ERROR_412_TITLE.get()).isEqualTo("Precondition Failed");
        assertThat(I18nHttpError.HTTP_ERROR_412_DETAIL.get(Map.of("reason", "test."))).isEqualTo(
                "A precondition required for fulfilling the request was not fulfilled: test.");
    }

    @Test
    public void whenLocaleIsEnUS_thenHttpError500ShouldWork() {
        assertThat(I18nHttpError.HTTP_ERROR_500_TITLE.get()).isEqualTo("Internal Server Error");
        assertThat(I18nHttpError.HTTP_ERROR_500_DETAIL_DEFAULT.get()).isEqualTo(
                "An unexpected error occurred, check the logs.");
        assertThat(I18nHttpError.HTTP_ERROR_500_DETAIL_WITH_REASON.get(Map.of("reason", "test."))).isEqualTo(
                "An unexpected error occurred: test.");
    }

    @Test
    public void whenLocaleIsEnUS_thenHttpError503ShouldWork() {
        assertThat(I18nHttpError.HTTP_ERROR_503_TITLE.get()).isEqualTo("Endpoint Temporarily not Available");
        assertThat(I18nHttpError.HTTP_ERROR_503_DETAIL.get()).isEqualTo(
                "The endpoint is temporarily not available, please try again later.");
    }

    @Test
    public void whenLocaleIsEnUS_thenHttpError507ShouldWork() {
        assertThat(I18nHttpError.HTTP_ERROR_507_TITLE.get()).isEqualTo("Insufficient Storage");
        assertThat(I18nHttpError.HTTP_ERROR_507_DETAIL_DEFAULT.get()).isEqualTo("Insufficient Storage.");
        assertThat(I18nHttpError.HTTP_ERROR_507_DETAIL_WITH_REASON.get(Map.of("reason", "test."))).isEqualTo(
                "Insufficient Storage: test.");
    }

    @Test
    public void whenLocaleIsEnGB_thenHttpError500ShouldFail() {
        I18nLocaleContext.setLocale(Locale.UK);
        assertThat(I18nHttpError.HTTP_ERROR_500_TITLE.get()).isEqualTo(
                "Error: Template http.error.500.title for en_GB could not be loaded.");
        assertThat(I18nHttpError.HTTP_ERROR_500_DETAIL_DEFAULT.get()).isEqualTo(
                "Error: Template http.error.500.detail.default for en_GB could not be loaded.");
        assertThat(I18nHttpError.HTTP_ERROR_500_DETAIL_WITH_REASON.get(Map.of("reason", "test."))).isEqualTo(
                "Error: Template http.error.500.detail.with.reason for en_GB could not be loaded.");
    }
}
