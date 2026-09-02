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
package com.hivemq.common.i18n;

import java.util.Locale;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * The i18n catalog for the v2 protocol-adapter REST API errors. Each error object served by the
 * v2 surface assembles its {@code title} and {@code detail} from this catalog so the strings are localizable; the
 * {@code .detail.with.*} variants carry FreeMarker parameters ({@code ${adapterId}}, {@code ${tagName}},
 * {@code ${reason}}). The backing resource bundle is {@code templates/protocol-adapter-errors-<locale>.properties}.
 */
public enum I18nProtocolAdapterError implements I18nError {
    ADAPTER_NOT_FOUND_TITLE,
    ADAPTER_NOT_FOUND_DETAIL_WITH_ID,
    ADAPTER_TYPE_NOT_FOUND_TITLE,
    ADAPTER_TYPE_NOT_FOUND_DETAIL_WITH_ID,
    TAG_NOT_FOUND_TITLE,
    TAG_NOT_FOUND_DETAIL_WITH_ADAPTER_AND_TAG,
    BROWSE_NOT_SUPPORTED_TITLE,
    BROWSE_NOT_SUPPORTED_DETAIL_WITH_ID,
    BROWSE_FILTER_INVALID_TITLE,
    BROWSE_FILTER_INVALID_DETAIL_WITH_ID_AND_REASON,
    ADAPTER_NOT_CONNECTED_TITLE,
    ADAPTER_NOT_CONNECTED_DETAIL_WITH_ID,
    BROWSE_IN_PROGRESS_TITLE,
    BROWSE_IN_PROGRESS_DETAIL_WITH_ID,
    BROWSE_TIMEOUT_TITLE,
    BROWSE_TIMEOUT_DETAIL_WITH_ID,
    ACTIVATION_INVALID_TITLE,
    ACTIVATION_INVALID_DETAIL,
    BROWSE_INTERRUPTED_TITLE,
    BROWSE_INTERRUPTED_DETAIL_WITH_ID,
    BROWSE_FAILED_TITLE,
    BROWSE_FAILED_DETAIL_WITH_ID,
    ;

    private static final @NotNull String RESOURCE_NAME_PREFIX = "templates/protocol-adapter-errors-";
    private static final @NotNull String RESOURCE_NAME_SUFFIX = ".properties";
    private static final @NotNull I18nErrorTemplate TEMPLATE = new I18nErrorTemplate(
            locale -> RESOURCE_NAME_PREFIX + locale + RESOURCE_NAME_SUFFIX,
            I18nProtocolAdapterError.class.getClassLoader());

    private final @NotNull String key;

    I18nProtocolAdapterError() {
        key = name().toLowerCase(Locale.ROOT).replace("_", ".");
    }

    @Override
    public @NotNull String get(final @NotNull Map<String, Object> map) {
        return TEMPLATE.get(this, map);
    }

    @Override
    public @NotNull String getKey() {
        return key;
    }

    @Override
    public @NotNull String getName() {
        return name();
    }
}
