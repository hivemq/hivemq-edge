/*
 * Copyright 2023-present HiveMQ GmbH
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.hivemq.edge.adapters.mtconnect.models;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MtConnectData {
    private final @NotNull String url;
    private final @NotNull String tagName;
    private boolean successful;
    private @Nullable String jsonString;
    private @Nullable String errorMessage;
    private @Nullable Throwable cause;
    public MtConnectData(@NotNull final String url, final boolean successful, final @NotNull String tagName) {
        this.url = url;
        this.successful = successful;
        this.tagName = tagName;
        jsonString = null;
        errorMessage = null;
        cause = null;
    }

    public @NotNull String getTagName() {
        return tagName;
    }

    public @Nullable String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(@NotNull final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public @Nullable Throwable getCause() {
        return cause;
    }

    public void setCause(@NotNull final Throwable cause) {
        this.cause = cause;
    }

    public @NotNull String getUrl() {
        return url;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(final boolean successful) {
        this.successful = successful;
    }

    public @Nullable String getJsonString() {
        return jsonString;
    }

    public void setJsonString(@NotNull final String jsonString) {
        this.jsonString = jsonString;
    }
}
