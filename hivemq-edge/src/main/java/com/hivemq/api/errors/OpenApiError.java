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

package com.hivemq.api.errors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;

public interface OpenApiError {
    @Nullable String getCode();

    void setCode(@Nullable String code);

    @Nullable String getDetail();

    void setDetail(@Nullable String detail);

    @NotNull String getTitle();

    void setTitle(@NotNull String title);

    @NotNull URI getType();

    void setType(@NotNull URI type);

    @NotNull Integer getStatus();

    void setStatus(@NotNull Integer status);
}
