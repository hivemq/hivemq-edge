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
package com.hivemq.edge.adapters.http.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hivemq.edge.modules.adapters.data.ProtocolAdapterDataSampleImpl;
import com.hivemq.edge.modules.config.AdapterSubscription;
import com.hivemq.extension.sdk.api.annotations.NotNull;

/**
 * @author HiveMQ Adapter Generator
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HttpData extends ProtocolAdapterDataSampleImpl {

    private final String requestUrl;
    private final String contentType;
    private int httpStatusCode;

    public HttpData(
            AdapterSubscription adapterSubscription, final String requestUrl,
            final int httpStatusCode,
            final @NotNull String contentType) {
        super(adapterSubscription);
        this.requestUrl = requestUrl;
        this.contentType = contentType;
        this.httpStatusCode = httpStatusCode;
    }


    public String getRequestUrl() {
        return requestUrl;
    }

    public String getContentType() {
        return contentType;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }


}
