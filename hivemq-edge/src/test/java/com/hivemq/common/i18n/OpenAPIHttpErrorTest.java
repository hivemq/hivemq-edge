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

import org.junit.Test;

import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class OpenAPIHttpErrorTest {
    @Test
    public void whenLocaleIsEnUS_thenHttpError500ShouldWork() {
        LocaleContext.setCurrentLocale(Locale.US);
        assertThat(OpenAPIHttpError.HTTP_ERROR_500_TITLE.get()).isEqualTo("Internal Server Error");
        assertThat(OpenAPIHttpError.HTTP_ERROR_500_DETAIL_DEFAULT.get()).isEqualTo(
                "An unexpected error occurred, check the logs.");
        assertThat(OpenAPIHttpError.HTTP_ERROR_500_DETAIL_WITH_REASON.get(Map.of("reason", "test."))).isEqualTo(
                "An unexpected error occurred: test.");
    }

    @Test
    public void whenLocaleIsEnGB_thenHttpError500ShouldFail() {
        LocaleContext.setCurrentLocale(Locale.UK);
        assertThat(OpenAPIHttpError.HTTP_ERROR_500_TITLE.get()).isEqualTo(
                "Error: Template http.error.500.title for en_GB could not be loaded.");
        assertThat(OpenAPIHttpError.HTTP_ERROR_500_DETAIL_DEFAULT.get()).isEqualTo(
                "Error: Template http.error.500.detail.default for en_GB could not be loaded.");
        assertThat(OpenAPIHttpError.HTTP_ERROR_500_DETAIL_WITH_REASON.get(Map.of("reason", "test."))).isEqualTo(
                "Error: Template http.error.500.detail.with.reason for en_GB could not be loaded.");
    }
}
