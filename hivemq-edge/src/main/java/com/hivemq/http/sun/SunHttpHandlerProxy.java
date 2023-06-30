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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.http.core.HttpConstants;
import com.hivemq.http.core.IHttpRequestResponseHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.net.URI;

public class SunHttpHandlerProxy implements HttpHandler {

    private IHttpRequestResponseHandler handler;

    public SunHttpHandlerProxy(final @NotNull IHttpRequestResponseHandler handler) {
        this.handler = handler;
    }

    public void handle(final @NotNull HttpExchange exchange) throws IOException {
        URI requestURI = exchange.getRequestURI();
        String contextPath = exchange.getHttpContext().getPath();
        String requestMethod = exchange.getRequestMethod();
        HttpConstants.METHOD method = HttpConstants.METHOD.valueOf(requestMethod);
        SunHttpRequestResponse sunHttpRequestResponse =
                new SunHttpRequestResponse(method, requestURI, contextPath, exchange);
//        printRequestInfo(exchange);
        handler.handleRequest(sunHttpRequestResponse);
    }
}
