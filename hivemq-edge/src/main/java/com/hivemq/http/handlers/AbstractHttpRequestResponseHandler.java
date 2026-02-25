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
import com.hivemq.http.core.Html;
import com.hivemq.http.core.HttpBadRequestException;
import com.hivemq.http.core.HttpException;
import com.hivemq.http.core.HttpInternalServerError;
import com.hivemq.http.core.HttpUtils;
import com.hivemq.http.core.IHttpRequestResponse;
import com.hivemq.http.core.IHttpRequestResponseHandler;
import com.hivemq.http.core.UsernamePasswordRoles;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractHttpRequestResponseHandler implements IHttpRequestResponseHandler {

    protected final Logger logger = LoggerFactory.getLogger(getClass().getName());

    protected final ObjectMapper mapper;

    public AbstractHttpRequestResponseHandler(final ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void handleRequest(final IHttpRequestResponse httpRequestResponse) throws IOException {

        final var start = System.currentTimeMillis();
        try {
            final var credentials = getRequiredCredentials(httpRequestResponse);
            if (credentials != null) {
                if (!handleBasicHttpAuthentication(credentials, httpRequestResponse)) {
                    return;
                }
            }
            switch (httpRequestResponse.getMethod()) {
                case GET:
                    handleHttpGet(httpRequestResponse);
                    break;
                case POST:
                    handleHttpPost(httpRequestResponse);
                    break;
                case HEAD:
                case PUT:
                case OPTIONS:
                case CONNECT:
                case TRACE:
                case PATCH:
                case DELETE:
                default:
                    sendUnsupportedOperationRequest(httpRequestResponse);
            }
        } catch (HttpException e) {
            logger.warn(
                    "caught strong typed http exception, use code and message [{} -> {}]",
                    e.getResponseCode(),
                    e.getResponseMessage());
            logger.error("handled error", e);
            try {
                writeASCIIResponse(httpRequestResponse, e.getResponseCode(), e.getResponseMessage());
            } catch (IOException ioe) {
                // ignore
            } catch (Exception ex) {
                logger.error("error sending internal server error request!", ex);
            }
        } catch (Exception e) {
            logger.error("unhandled error", e);
            try {
                writeASCIIResponse(httpRequestResponse, HttpConstants.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } catch (IOException ioe) {
                // ignore
            } catch (Exception ex) {
                logger.error("error sending internal server error request!", ex);
            }
        } finally {
            logger.trace(
                    "request {} {} ({}) -> {} done in {}",
                    httpRequestResponse.getHttpRequestUri(),
                    httpRequestResponse.getContextPath(),
                    httpRequestResponse.getContextRelativePath(),
                    httpRequestResponse.getResponseCode(),
                    System.currentTimeMillis() - start);
        }
    }

    protected void handleHttpGet(final IHttpRequestResponse request) throws HttpException, IOException {
        sendNotFoundResponse(request);
    }

    protected void handleHttpPost(final IHttpRequestResponse request) throws HttpException, IOException {
        sendNotFoundResponse(request);
    }

    protected void sendUnsupportedOperationRequest(final IHttpRequestResponse request) throws IOException {
        writeASCIIResponse(
                request,
                HttpConstants.SC_METHOD_NOT_ALLOWED,
                Html.getErrorMessage(HttpConstants.SC_METHOD_NOT_ALLOWED, "Method not allowed"));
    }

    protected void sendNotFoundResponse(final IHttpRequestResponse request) throws IOException {
        writeHTMLResponse(
                request,
                HttpConstants.SC_NOT_FOUND,
                Html.getErrorMessage(HttpConstants.SC_NOT_FOUND, "Resource Not found"));
    }

    protected void sendBadRequestResponse(final IHttpRequestResponse request, final String message) throws IOException {
        logger.info("resource not found {}", request);
        writeHTMLResponse(
                request, HttpConstants.SC_BAD_REQUEST, Html.getErrorMessage(HttpConstants.SC_BAD_REQUEST, message));
    }

    protected void sendRedirect(final IHttpRequestResponse request, final String resourceUri) throws IOException {
        try {
            logger.info("sending client side redirect to {}", resourceUri);
            request.addResponseHeader(HttpConstants.LOCATION_HEADER, resourceUri);
            request.sendResponseHeaders(HttpConstants.SC_MOVED_TEMPORARILY, 0);
        } finally {
            request.commit();
        }
    }

    protected void writeASCIIResponse(final IHttpRequestResponse request, final int responseCode, final String message)
            throws IOException {
        writeResponseInternal(
                request,
                responseCode,
                HttpConstants.PLAIN_MIME_TYPE,
                message != null ? message.getBytes(StandardCharsets.UTF_8) : new byte[0]);
    }

    protected void writeHTMLResponse(final IHttpRequestResponse request, final int responseCode, final String html)
            throws IOException {
        writeResponseInternal(
                request,
                responseCode,
                HttpConstants.HTML_MIME_TYPE,
                html != null ? html.getBytes(StandardCharsets.UTF_8) : new byte[0]);
    }

    protected void writeJSONResponse(final IHttpRequestResponse request, final int responseCode, final byte[] bytes)
            throws IOException {
        writeResponseInternal(request, responseCode, HttpConstants.JSON_MIME_TYPE, bytes);
    }

    protected void writeJSONBeanResponse(final IHttpRequestResponse request, final int responseCode, final Object bean)
            throws IOException {
        writeJSONResponse(request, responseCode, mapper.writeValueAsBytes(bean));
    }

    protected void writeMessageBeanResponse(
            final IHttpRequestResponse request, final int responseCode, final Message message) throws IOException {
        writeJSONResponse(request, responseCode, mapper.writeValueAsBytes(message));
    }

    protected void writeStreamResponse(
            final IHttpRequestResponse request, final int responseCode, final String mimeType, final InputStream is)
            throws IOException {
        final var baos = new ByteArrayOutputStream(1024);
        final var buf = new byte[1024];
        int length;
        while ((length = is.read(buf)) != -1) {
            baos.write(buf, 0, length);
        }
        final var bytes = baos.toByteArray();
        writeResponseInternal(request, responseCode, mimeType, bytes);
    }

    protected void writeResponseInternal(
            final IHttpRequestResponse request, final int responseCode, final String mimeType, final byte[] bytes)
            throws IOException {
        OutputStream os = null;
        try {
            request.setResponseContentType(mimeType, StandardCharsets.UTF_8);
            request.sendResponseHeaders(responseCode, bytes.length);
            os = request.getResponseBody();
            os.write(bytes);
        } finally {
            if (os != null) {
                os.close();
            }
            request.commit();
        }
    }

    protected void writeSimpleOKResponse(final IHttpRequestResponse request) throws IOException {
        try {
            request.setResponseContentType(HttpConstants.PLAIN_MIME_TYPE, StandardCharsets.UTF_8);
            request.sendResponseHeaders(HttpConstants.SC_OK, 0);
        } finally {
            request.commit();
        }
    }

    protected void writeDataFromResource(final IHttpRequestResponse requestResponse, final String resourcePath)
            throws IOException {
        InputStream is = loadClasspathResource(resourcePath);
        logger.trace("loading resource from cp '{}' exists ? {}", resourcePath, is != null);
        if (is == null) {
            sendNotFoundResponse(requestResponse);
        } else {
            final var ext = Files.getFileExtension(resourcePath);
            final var mimeType = HttpUtils.getMimeTypeFromFileExtension(ext);
            writeStreamResponse(requestResponse, HttpConstants.SC_OK, mimeType, is);
        }
    }

    protected <T> T readRequestBody(final IHttpRequestResponse requestResponse, final Class<T> cls)
            throws HttpInternalServerError {
        try {
            return mapper.readValue(requestResponse.getRequestBody(), cls);
        } catch (Exception e) {
            throw new HttpInternalServerError("error reading request body", e);
        }
    }

    protected InputStream loadClasspathResource(final String resource) {
        logger.trace("loading resource from path " + resource);
        InputStream is =
                AbstractHttpRequestResponseHandler.class.getClassLoader().getResourceAsStream(resource);
        if (is == null) {
            is = AbstractHttpRequestResponseHandler.class.getClassLoader().getResourceAsStream("/" + resource);
        }
        return is;
    }

    protected String getMandatoryParameter(final IHttpRequestResponse requestResponse, final String paramKey)
            throws HttpBadRequestException {
        final var value = requestResponse.getParameter(paramKey);
        if (value == null) {
            throw new HttpBadRequestException("mandatory request parameter not available " + paramKey);
        }
        return value;
    }

    protected String getParameter(final IHttpRequestResponse requestResponse, final String paramKey) {
        return requestResponse.getParameter(paramKey);
    }

    protected boolean handleBasicHttpAuthentication(
            final UsernamePasswordRoles usernamePassword, final IHttpRequestResponse httpRequestResponse)
            throws IOException {

        String value = httpRequestResponse.getRequestHeader(HttpConstants.AUTH_HEADER);
        if (value != null) {
            value = value.substring(value.lastIndexOf(" ") + 1);
            value = new String(Base64.getDecoder().decode(value));
            final var userNamePassword = value.split(":");
            if (usernamePassword.getUserName().equals(userNamePassword[0])
                    && Objects.deepEquals(
                            usernamePassword.getPassword(), userNamePassword[1].getBytes(StandardCharsets.UTF_8))) {
                return true;
            }
        }

        httpRequestResponse.addResponseHeader(
                HttpConstants.BASIC_AUTH_CHALLENGE_HEADER,
                String.format(HttpConstants.BASIC_AUTH_REALM, usernamePassword.getRealm()));
        writeResponseInternal(
                httpRequestResponse, HttpConstants.SC_UNAUTHORIZED, HttpConstants.HTML_MIME_TYPE, new byte[0]);
        return false;
    }

    protected UsernamePasswordRoles getRequiredCredentials(final IHttpRequestResponse request) {
        return null;
    }

    public static class Message {

        public String title;
        public String message;
        public boolean success;

        public Message() {}

        public Message(final String message) {
            this.message = message;
            this.success = true;
        }

        public Message(final String title, final String message) {
            this.message = message;
            this.title = title;
            this.success = true;
        }

        public Message(final String message, final boolean success) {
            this.message = message;
            this.success = success;
        }

        public Message(final String title, final String message, final boolean success) {
            this.title = title;
            this.message = message;
            this.success = success;
        }
    }
}
