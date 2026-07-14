/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.http.v2;

import org.jetbrains.annotations.NotNull;

/**
 * The content type of a {@link HttpNode}'s request body, and its {@code Content-Type} MIME value — carried over
 * verbatim from the v1 HTTP adapter. Used to set the {@code Content-Type} header on {@code POST}/{@code PUT}
 * requests.
 */
public enum HttpContentType {
    JSON("application/json"),
    PLAIN("text/plain"),
    HTML("text/html"),
    XML("application/xml"),
    YAML("application/yaml");

    private final @NotNull String mimeType;

    HttpContentType(final @NotNull String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * @return the {@code Content-Type} MIME value for this content type.
     */
    public @NotNull String getMimeType() {
        return mimeType;
    }
}
