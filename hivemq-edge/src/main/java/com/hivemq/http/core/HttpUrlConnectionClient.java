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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Simon L Johnson
 *
 * Super quick HTTP client implementation using the in-built HttpUrlConnection in the .net packages.
 * NOTE: do not use for high traffic situations, please for those defer to HttpClient in commons.
 */
public class HttpUrlConnectionClient {

    static final int READ_BUFFER_SIZE = 1024;

    public static final Map<String, String> JSON_HEADERS =
            Map.of("Content-Type", "application/json",
                    "Accept", "application/json");

    public static final Map<String, String> FORM_HEADERS =
            Map.of("Content-Type", "application/x-www-form-urlencoded",
                    "Accept", "application/json");

    public static HttpResponse head(Map<String, String> headers, String url, int readTimeout) throws IOException {
        HttpURLConnection connection = null;
        URL serverAddress = null;
        try {
            serverAddress = new URL(url);
            connection = null;
            connection = (HttpURLConnection) serverAddress.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setDoOutput(false);
            connection.setReadTimeout(readTimeout);
            writeRequestHeaders(headers, connection);
            connection.connect();
            return createResponse(url, connection, false);
        } finally {
            try {connection.disconnect();} catch(Throwable t) {}
        }
    }

    public static HttpResponse get(Map<String, String> headers, String url, int connectTimeoutMillis, int readTimeoutMillis)
            throws IOException {

        HttpURLConnection connection = null;
        InputStream is = null;
        URL serverAddress = null;
        try {
            serverAddress = new URL(url);
            connection = null;
            connection = (HttpURLConnection) serverAddress.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoOutput(true);
            connection.setInstanceFollowRedirects(true);
            connection.setReadTimeout(readTimeoutMillis);
            connection.setConnectTimeout(connectTimeoutMillis);
            writeRequestHeaders(headers, connection);
            connection.connect();
            return createResponse(url, connection, true);
        } finally {
            try {connection.disconnect();} catch(Throwable t) {}
            try {is.close();} catch(Throwable t) {}
        }
    }

    public static HttpResponse _withdata(String httpMethod, Map<String, String> headers, String url, InputStream is, int connectTimeoutMillis, int readTimeoutMillis)
            throws IOException {

        HttpURLConnection connection = null;
        URL serverAddress = null;
        try {
            serverAddress = new URL(url);
            connection = (HttpURLConnection) serverAddress.openConnection();
            connection.setRequestMethod(httpMethod);
            connection.setDoOutput(true);
            connection.setReadTimeout(readTimeoutMillis);
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(connectTimeoutMillis);
            writeRequestHeaders(headers, connection);
            Files.copy(is, connection.getOutputStream(), READ_BUFFER_SIZE);
            connection.connect();
            return createResponse(url, connection, true);
        } finally {
            try {connection.disconnect();} catch(Throwable t) {}
            try {is.close();} catch(Throwable t) {}
        }
    }

    public static HttpResponse put(Map<String, String> headers, String url, InputStream is, int connectTimeoutMillis, int readTimeoutMillis)
            throws IOException {

        return _withdata("PUT", headers, url, is, connectTimeoutMillis, readTimeoutMillis);
    }

    public static HttpResponse post(Map<String, String> headers, String url, InputStream is, int connectTimeoutMillis, int readTimeoutMillis)
            throws IOException {

        return _withdata("POST", headers, url, is, connectTimeoutMillis, readTimeoutMillis);
    }

    public static HttpResponse delete(Map<String, String> headers, String url, InputStream is, int connectTimeoutMillis, int readTimeoutMillis)
            throws IOException {

        return _withdata("DELETE", headers, url, is, connectTimeoutMillis, readTimeoutMillis);
    }

    private static void writeRequestHeaders(Map<String, String> headers, HttpURLConnection connection){
        if(headers != null){
            Iterator<String> keys = headers.keySet().iterator();
            while(keys.hasNext()){
                String k = keys.next();
                connection.setRequestProperty(k, headers.get(k));
            }
        }
    }

    private static HttpResponse createResponse(String url, HttpURLConnection connection, boolean readBody) throws IOException {
        HttpResponse response = new HttpResponse();
        response.setRequestUrl(url);
        response.setContentLength(connection.getContentLength());
        response.setContentType(connection.getContentType());
        response.setStatusCode(connection.getResponseCode());
        response.setStatusMessage(connection.getResponseMessage());
        response.setContentEncoding(connection.getContentEncoding());

        Map<String, String> headers = new HashMap<>();
        for (int i = 0;; i++) {
            String headerName = connection.getHeaderFieldKey(i);
            String headerValue = connection.getHeaderField(i);
            if (headerName == null && headerValue == null) {
                //no more headers
                break;
            }
            headers.put(headerName, headerValue);
        }
        response.setResponseHeaders(Collections.unmodifiableMap(headers));

        if(readBody){
            InputStream is = null;
            try {
                int code = response.getStatusCode();
                if(code > 299) {
                    is = connection.getErrorStream();
                } else {
                    is = connection.getInputStream();
                }
                if(is != null){
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[READ_BUFFER_SIZE];
                    int bytesRead;
                    int total = 0;
                    while ((bytesRead = is.read(buffer)) != -1){
                        baos.write(buffer, 0, bytesRead);
                        total += bytesRead;
                    }
                    response.setResponseBody(baos.toByteArray());
                    response.setContentLength(total);
                }
            }
            finally {
                try {
                    is.close();
                } catch(Exception e){}
            }
        }
        return response;
    }
}
