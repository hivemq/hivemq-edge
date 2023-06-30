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
import com.hivemq.http.core.Files;
import com.hivemq.http.core.HttpConstants;
import com.hivemq.http.core.HttpUtils;
import com.hivemq.http.core.IHttpRequestResponse;

import java.io.IOException;
import java.io.InputStream;

public class WebAppHandler extends AbstractHttpRequestResponseHandler {

    protected final String resourceRoot;

    public WebAppHandler(ObjectMapper mapper, String resourceRoot) {
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

    protected void writeDataFromResource(IHttpRequestResponse requestResponse, String resourcePath) throws IOException {
        if (resourcePath.endsWith("/")) {
            resourcePath += "index.html";
        }
        InputStream is = loadClasspathResource(resourcePath);
        try {
            logger.trace("loading resource from cp '{}' exists ? {}", resourcePath, is != null);
            if (is == null) {
                String ext = Files.getFileExtension(resourcePath);
                if (ext == null) {
                    is = loadClasspathResource(HttpUtils.combinePaths(resourceRoot, "/index.html"));
                    writeStreamResponse(requestResponse, HttpConstants.SC_OK, "text/html", is);
                } else {
                    sendNotFoundResponse(requestResponse);
                }
            } else {
                String fileName = Files.getFileName(resourcePath);
                String ext = Files.getFileExtension(resourcePath);
                String mimeType = HttpUtils.getMimeTypeFromFileExtension(ext);
                writeStreamResponse(requestResponse, HttpConstants.SC_OK, mimeType, is);
            }
        } catch (IOException e) {
            //ignore
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }
}
