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
package com.hivemq.http.core;

import java.util.Map;
import org.jetbrains.annotations.Nullable;

public class HttpResponse {

    @Nullable
    Map<String, String> responseHeaders;

    @Nullable
    String requestUrl;

    @Nullable
    String statusMessage;

    @Nullable
    String contentEncoding;

    int statusCode;
    byte @Nullable [] responseBody;
    int contentLength;

    @Nullable
    String contentType;

    public @Nullable String getContentEncoding() {
        return contentEncoding;
    }

    public void setContentEncoding(@Nullable String contentEncoding) {
        this.contentEncoding = contentEncoding;
    }

    public @Nullable String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(@Nullable String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public byte @Nullable [] getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(byte @Nullable [] responseBody) {
        this.responseBody = responseBody;
    }

    public int getContentLength() {
        return contentLength;
    }

    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }

    public @Nullable String getContentType() {
        return contentType;
    }

    public void setContentType(@Nullable String contentType) {
        this.contentType = contentType;
    }

    public boolean isError() {
        return getStatusCode() < 200 || getStatusCode() > 299;
    }

    public @Nullable String getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(@Nullable String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public @Nullable Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(@Nullable Map<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    @Override
    public String toString() {
        return "HttpResponse{" + "requestUrl='"
                + requestUrl + '\'' + ", statusMessage='"
                + statusMessage + '\'' + ", contentEncoding='"
                + contentEncoding + '\'' + ", statusCode="
                + statusCode + ", contentLength="
                + contentLength + ", contentType='"
                + contentType + '\'' + '}';
    }
}
