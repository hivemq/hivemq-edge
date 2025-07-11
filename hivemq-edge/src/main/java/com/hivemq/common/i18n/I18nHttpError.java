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

public enum I18nHttpError implements I18nError {
    HTTP_ERROR_400_INVALID_QUERY_PARAMETER_DETAIL_WITH_PARAMETER_AND_REASON,
    HTTP_ERROR_400_INVALID_QUERY_PARAMETER_TITLE,
    HTTP_ERROR_400_REQUEST_BODY_MISSING_DETAIL,
    HTTP_ERROR_400_REQUEST_BODY_MISSING_TITLE,
    HTTP_ERROR_400_REQUEST_BODY_PARAMETER_MISSING_DETAIL_WITH_PARAMETER,
    HTTP_ERROR_400_REQUEST_BODY_PARAMETER_MISSING_TITLE,
    HTTP_ERROR_400_URL_PARAMETER_MISSING_DETAIL_WITH_PARAMETER,
    HTTP_ERROR_400_URL_PARAMETER_MISSING_TITLE,
    HTTP_ERROR_412_DETAIL_WITH_REASON,
    HTTP_ERROR_412_TITLE,
    HTTP_ERROR_500_DETAIL,
    HTTP_ERROR_500_DETAIL_WITH_REASON,
    HTTP_ERROR_500_TITLE,
    HTTP_ERROR_503_DETAIL,
    HTTP_ERROR_503_TITLE,
    HTTP_ERROR_507_DETAIL,
    HTTP_ERROR_507_DETAIL_WITH_REASON,
    HTTP_ERROR_507_TITLE,
    ;

    private static final @NotNull String RESOURCE_NAME_PREFIX = "/templates/http-errors-";
    private static final @NotNull String RESOURCE_NAME_SUFFIX = ".properties";
    private static final I18nErrorTemplate TEMPLATE =
            new I18nErrorTemplate(locale -> RESOURCE_NAME_PREFIX + locale + RESOURCE_NAME_SUFFIX);

    private final @NotNull String key;

    I18nHttpError() {
        key = name().toLowerCase().replace("_", ".");
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
