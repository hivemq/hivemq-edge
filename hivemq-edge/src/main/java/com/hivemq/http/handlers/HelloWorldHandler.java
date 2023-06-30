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
package com.hivemq.http.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.http.core.Html;
import com.hivemq.http.core.HttpConstants;
import com.hivemq.http.core.IHttpRequestResponse;

import java.io.IOException;

public class HelloWorldHandler extends AbstractHttpRequestResponseHandler {

    public HelloWorldHandler(ObjectMapper mapper) {
        super(mapper);
    }

    @Override
    protected void handleHttpGet(IHttpRequestResponse request) throws IOException {
        writeHTMLResponse(request, HttpConstants.SC_OK, Html.span("Hello World From HiveMQ Edge Console!", Html.RED, true));
    }
}
