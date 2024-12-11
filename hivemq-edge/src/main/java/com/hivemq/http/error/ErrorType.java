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
package com.hivemq.http.error;

import org.jetbrains.annotations.NotNull;

public class ErrorType {
    private final @NotNull String type;
    private final @NotNull String title;
    private final @NotNull String detail;

    public ErrorType(@NotNull final String type, @NotNull final String title, @NotNull final String detail) {
        this.type = type;
        this.title = title;
        this.detail = detail;
    }

    public @NotNull String getType() {
        return type;
    }

    public @NotNull String getTitle() {
        return title;
    }

    public @NotNull String getDetail() {
        return detail;
    }
}
