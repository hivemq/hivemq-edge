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
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class WebAppHandler extends AbstractHttpRequestResponseHandler {
    protected static final @NotNull String INDEX_HTML = "index.html";
    protected static final @NotNull String ROOT_INDEX_HTML = HttpConstants.SLASH + INDEX_HTML;
    protected final @NotNull String resourceRoot;

    public WebAppHandler(final @NotNull ObjectMapper mapper, final @NotNull String resourceRoot) {
        super(mapper);
        this.resourceRoot = resourceRoot;
    }

    @Override
    protected void handleHttpGet(final @NotNull IHttpRequestResponse requestResponse) throws IOException {

        final String relativePath = requestResponse.getContextRelativePath();
        if (StringUtils.isEmpty(relativePath)) {
            sendRedirect(requestResponse, "app/");
        } else {
            final String resourcePath = HttpUtils.sanitizePath(relativePath);
            final String filePath = HttpUtils.combinePaths(resourceRoot, resourcePath);
            writeDataFromResource(requestResponse, filePath);
        }
    }

    protected void writeDataFromResource(
            final @NotNull IHttpRequestResponse requestResponse,
            @NotNull String resourcePath) throws IOException {
        if (resourcePath.endsWith(HttpConstants.SLASH)) {
            resourcePath += INDEX_HTML;
        }
        InputStream inputStream = loadClasspathResource(resourcePath);
        try {
            logger.trace("loading resource from cp '{}' exists ? {}", resourcePath, inputStream != null);
            String ext = Files.getFileExtension(resourcePath);
            if (inputStream == null && ext != null) {
                sendNotFoundResponse(requestResponse);
            } else {
                if (inputStream == null) {
                    resourcePath = HttpUtils.combinePaths(resourceRoot, ROOT_INDEX_HTML);
                    ext = Files.getFileExtension(resourcePath);
                    inputStream = loadClasspathResource(resourcePath);
                }
                if (resourcePath.endsWith(ROOT_INDEX_HTML)) {
                    String indexContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                    final int depth = StringUtils.countMatches(requestResponse.getContextRelativePath(), '/');
                    if (depth > 1) {
                        final String relativePathPrefix = StringUtils.repeat("../", depth - 1);
                        /*
                         * Adjust relative paths inside the index.html to support Edge behind a reverse proxy.
                         * As Edge doesn't get the absolute path from the request, we can only calculate the
                         * depth from the context relative path and patch the index.html dynamically.
                         */
                        indexContent = indexContent.replaceAll("(href|src)=\"\\./", "$1=\"" + relativePathPrefix);
                    }
                    writeResponseInternal(requestResponse,
                            HttpConstants.SC_OK,
                            HttpConstants.HTML_MIME_TYPE,
                            indexContent.getBytes(StandardCharsets.UTF_8));
                } else {
                    final String mimeType = HttpUtils.getMimeTypeFromFileExtension(ext);
                    writeStreamResponse(requestResponse, HttpConstants.SC_OK, mimeType, inputStream);
                }
            }
        } catch (final IOException ignored) {
            //ignore
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }
}
