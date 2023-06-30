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
package com.hivemq.http.sun;

import com.google.common.base.Preconditions;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.http.core.HttpConstants;
import com.hivemq.http.core.HttpRequestResponse;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class SunHttpRequestResponse extends HttpRequestResponse {

    public HttpExchange exchange;

    public SunHttpRequestResponse(final @NotNull HttpConstants.METHOD method, final @NotNull URI httpRequestUri, final @NotNull String contextPath, final @NotNull HttpExchange exchange) {
        super(method, httpRequestUri, contextPath);
        this.exchange = exchange;
    }

    public OutputStream getResponseBody(){
        return exchange.getResponseBody();
    }

    @Override
    public InputStream getRequestBody() {
        return exchange.getRequestBody();
    }

    public void addResponseHeader(final @NotNull String headerKey, final @NotNull String headerValue){
        Preconditions.checkNotNull(headerKey);
        Preconditions.checkNotNull(headerValue);
        exchange.getResponseHeaders().add(headerKey,
                headerValue);
    }

    protected void sendResponseHeadersInternal(int httpCode, int size) throws IOException {
        exchange.sendResponseHeaders(httpCode, size);
    }

    @Override
    public String getRequestHeader(final @NotNull String key) {
        return exchange.getRequestHeaders().getFirst(key);
    }

    @Override
    protected Map<String, String> getRequestHeaders() {
        Headers headers = exchange.getRequestHeaders();
        Set<String> s = headers.keySet();
        Iterator<String> itr = s.iterator();
        HashMap<String, String> map = new HashMap<>();
        while(itr.hasNext()){
            String key = itr.next();
            String value = headers.getFirst(key);
            map.put(key, value);
        }
        return Collections.unmodifiableMap(map);
    }

    @Override
    public void commit() {
        try {
            exchange.close();
        } catch(Exception e){
//            e.printStackTrace();
        }
    }
}
