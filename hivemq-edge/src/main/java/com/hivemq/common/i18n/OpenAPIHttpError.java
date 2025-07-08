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

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public enum OpenAPIHttpError implements I18nTemplate {
    HTTP_ERROR_500_TITLE("http.error.500.title"),
    HTTP_ERROR_500_DETAIL_DEFAULT("http.error.500.detail.default"),
    HTTP_ERROR_500_DETAIL_WITH_REASON("http.error.500.detail.with.reason"),
    ;

    private static final String RESOURCE_NAME_PREFIX = "/templates/openapi-errors-";
    private static final String RESOURCE_NAME_SUFFIX = ".properties";
    private final @NotNull String key;

    OpenAPIHttpError(final @NotNull String key) {
        this.key = key;
    }

    public @NotNull String get() {
        return get(Map.of());
    }

    public @NotNull String get(final @NotNull Map<String, Object> map) {
        return OpenAPIErrorTemplate.getInstance().get(this, map);
    }

    @Override
    public @NotNull String getKey() {
        return key;
    }

    @Override
    public @NotNull String getName() {
        return name();
    }

    @Override
    public @NotNull String getResourceName() {
        return RESOURCE_NAME_PREFIX + LocaleContext.getCurrentLocale() + RESOURCE_NAME_SUFFIX;
    }
}
