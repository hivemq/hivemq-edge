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
import com.hivemq.http.HttpConstants;
import com.hivemq.http.core.Files;
import com.hivemq.http.core.HttpUtils;
import com.hivemq.http.core.IHttpRequestResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class WebAppHandler extends AbstractHttpRequestResponseHandler {

    protected final String resourceRoot;

    private final @Nullable String proxyPath;

    public WebAppHandler(final @NotNull ObjectMapper mapper, final @Nullable String proxyPath, final @NotNull String resourceRoot) {
        super(mapper);
        this.resourceRoot = resourceRoot;
        this.proxyPath = proxyPath;
    }

    @Override
    protected void handleHttpGet(final @NotNull IHttpRequestResponse requestResponse) throws IOException {
        String resourcePath = requestResponse.getContextRelativePath();
        resourcePath = HttpUtils.sanitizePath(resourcePath);
        final String filePath = HttpUtils.combinePaths(resourceRoot, resourcePath);
        writeDataFromResource(requestResponse, filePath);
    }

    protected void writeDataFromResource(final @NotNull IHttpRequestResponse requestResponse, final @NotNull String originalResourcePath) throws IOException {
        String resourcePath = originalResourcePath;
        if (resourcePath.endsWith("/")) {
            resourcePath += "index.html";
        }
        InputStream is = loadClasspathResource(resourcePath);
        try {
            logger.trace("loading resource from cp '{}' exists ? {}", resourcePath, is != null);
            if (is == null) {
                final String ext = Files.getFileExtension(resourcePath);
                if (ext == null) {
                    is = loadClasspathResource(HttpUtils.combinePaths(resourceRoot, "/index.html"));
                    if(proxyPath != null) {
                        is = fixIndexHtmL(is, proxyPath);
                    }
                    writeStreamResponse(requestResponse, HttpConstants.SC_OK, "text/html", is);
                } else {
                    sendNotFoundResponse(requestResponse);
                }
            } else {
                final String ext = Files.getFileExtension(resourcePath);
                final String mimeType = HttpUtils.getMimeTypeFromFileExtension(ext);
                if(resourcePath.endsWith("index.html") && proxyPath != null) {
                    is = fixIndexHtmL(is, proxyPath);
                }
                writeStreamResponse(requestResponse, HttpConstants.SC_OK, mimeType, is);
            }
        } catch (final IOException e) {
            //ignore
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    InputStream fixIndexHtmL(final @NotNull InputStream is, final @NotNull String proxyPath) throws IOException {
        final var index = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        final var replaced = index.replace("/app/", proxyPath + "/app/");
        is.close();
        return new ByteArrayInputStream(replaced.getBytes(StandardCharsets.UTF_8));
    }
}
