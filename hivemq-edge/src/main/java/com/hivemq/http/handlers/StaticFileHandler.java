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
import com.hivemq.http.core.HttpUtils;
import com.hivemq.http.core.IHttpRequestResponse;

import java.io.IOException;

public class StaticFileHandler extends AbstractHttpRequestResponseHandler {

    protected final String resourceRoot;

    public StaticFileHandler(ObjectMapper mapper, String resourceRoot) {
        super(mapper);
        this.resourceRoot = resourceRoot;
    }

    @Override
    protected void handleHttpGet(IHttpRequestResponse requestResponse) throws IOException {

        String resourcePath = requestResponse.getContextRelativePath();
        resourcePath = HttpUtils.sanitizePath(resourcePath);
        String filePath = HttpUtils.combinePaths(resourceRoot, resourcePath);
        writeDataFromResource(requestResponse, filePath);
    }
}
