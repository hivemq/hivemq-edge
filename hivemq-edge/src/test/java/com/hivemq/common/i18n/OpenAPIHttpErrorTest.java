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

import static org.assertj.core.api.Assertions.assertThat;

public class OpenAPIHttpErrorTest {
    @BeforeEach
    public void setUp() {
        LocaleContext.setLocale(Locale.US);
    }

    @AfterEach
    public void tearDown() {
        LocaleContext.setLocale(LocaleContext.DEFAULT_LOCALE);
    }

    @Test
    public void whenLocaleIsEnUS_thenErrorCountShouldMatch() throws IOException {
        final List<OpenAPIHttpError> errors = Arrays.asList(OpenAPIHttpError.values());
        assertThat(errors.size()).isGreaterThan(0);
        final Properties properties = new Properties();
        try (final StringReader stringReader = new StringReader(IOUtils.resourceToString(errors.get(0)
                .getResourceName(), StandardCharsets.UTF_8))) {
            properties.load(stringReader);
        }
        assertThat(properties.size()).isEqualTo(errors.size());
        final Set<Object> keySet = properties.keySet();
        errors.forEach(error -> assertThat(keySet.contains(error.getKey())).as(error.getKey() + " is not found.")
                .isTrue());
        properties.values().forEach(template -> {
            assertThat(template).isInstanceOf(String.class);
            assertThat((String) template).isNotBlank();
        });
    }

    @Test
    public void whenLocaleIsEnUS_thenHttpError400ShouldWork() {
        assertThat(OpenAPIHttpError.HTTP_ERROR_400_INVALID_QUERY_PARAMETER_TITLE.get()).isEqualTo(
                "Query Parameter is Invalid");
        assertThat(OpenAPIHttpError.HTTP_ERROR_400_INVALID_QUERY_PARAMETER_DETAIL.get(Map.of("parameter",
                "p1",
                "reason",
                "test."))).isEqualTo("Query parameter 'p1' is invalid: test.");
        assertThat(OpenAPIHttpError.HTTP_ERROR_400_REQUEST_BODY_MISSING_TITLE.get()).isEqualTo(
                "Required Request Body Missing");
        assertThat(OpenAPIHttpError.HTTP_ERROR_400_REQUEST_BODY_MISSING_DETAIL.get()).isEqualTo(
                "Required request body is missing.");
        assertThat(OpenAPIHttpError.HTTP_ERROR_400_REQUEST_BODY_PARAMETER_MISSING_TITLE.get()).isEqualTo(
                "Required Request Body Parameter Missing");
        assertThat(OpenAPIHttpError.HTTP_ERROR_400_REQUEST_BODY_PARAMETER_MISSING_DETAIL.get(Map.of("parameter",
                "p1"))).isEqualTo("Required request body parameter 'p1' is missing.");
        assertThat(OpenAPIHttpError.HTTP_ERROR_400_URL_PARAMETER_MISSING_TITLE.get()).isEqualTo(
                "Required URL Parameter Missing");
        assertThat(OpenAPIHttpError.HTTP_ERROR_400_URL_PARAMETER_MISSING_DETAIL.get(Map.of("parameter",
                "p1"))).isEqualTo("Required URL parameter 'p1' is missing.");
    }

    @Test
    public void whenLocaleIsEnUS_thenHttpError412ShouldWork() {
        assertThat(OpenAPIHttpError.HTTP_ERROR_412_TITLE.get()).isEqualTo("Precondition Failed");
        assertThat(OpenAPIHttpError.HTTP_ERROR_412_DETAIL.get(Map.of("reason", "test."))).isEqualTo(
                "A precondition required for fulfilling the request was not fulfilled: test.");
    }

    @Test
    public void whenLocaleIsEnUS_thenHttpError500ShouldWork() {
        assertThat(OpenAPIHttpError.HTTP_ERROR_500_TITLE.get()).isEqualTo("Internal Server Error");
        assertThat(OpenAPIHttpError.HTTP_ERROR_500_DETAIL_DEFAULT.get()).isEqualTo(
                "An unexpected error occurred, check the logs.");
        assertThat(OpenAPIHttpError.HTTP_ERROR_500_DETAIL_WITH_REASON.get(Map.of("reason", "test."))).isEqualTo(
                "An unexpected error occurred: test.");
    }

    @Test
    public void whenLocaleIsEnUS_thenHttpError503ShouldWork() {
        assertThat(OpenAPIHttpError.HTTP_ERROR_503_TITLE.get()).isEqualTo("Endpoint Temporarily not Available");
        assertThat(OpenAPIHttpError.HTTP_ERROR_503_DETAIL.get()).isEqualTo(
                "The endpoint is temporarily not available, please try again later.");
    }

    @Test
    public void whenLocaleIsEnUS_thenHttpError507ShouldWork() {
        assertThat(OpenAPIHttpError.HTTP_ERROR_507_TITLE.get()).isEqualTo("Insufficient Storage");
        assertThat(OpenAPIHttpError.HTTP_ERROR_507_DETAIL_DEFAULT.get()).isEqualTo("Insufficient Storage.");
        assertThat(OpenAPIHttpError.HTTP_ERROR_507_DETAIL_WITH_REASON.get(Map.of("reason", "test."))).isEqualTo(
                "Insufficient Storage: test.");
    }

    @Test
    public void whenLocaleIsEnGB_thenHttpError500ShouldFail() {
        LocaleContext.setLocale(Locale.UK);
        assertThat(OpenAPIHttpError.HTTP_ERROR_500_TITLE.get()).isEqualTo(
                "Error: Template http.error.500.title for en_GB could not be loaded.");
        assertThat(OpenAPIHttpError.HTTP_ERROR_500_DETAIL_DEFAULT.get()).isEqualTo(
                "Error: Template http.error.500.detail.default for en_GB could not be loaded.");
        assertThat(OpenAPIHttpError.HTTP_ERROR_500_DETAIL_WITH_REASON.get(Map.of("reason", "test."))).isEqualTo(
                "Error: Template http.error.500.detail.with.reason for en_GB could not be loaded.");
    }
}
