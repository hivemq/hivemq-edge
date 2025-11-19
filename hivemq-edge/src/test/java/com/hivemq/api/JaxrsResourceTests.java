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
package com.hivemq.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.http.JaxrsHttpServer;
import com.hivemq.http.config.JaxrsHttpServerConfiguration;
import com.hivemq.http.core.HttpResponse;
import com.hivemq.http.core.HttpUrlConnectionClient;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.RandomPortGenerator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * @author Simon L Johnson
 */
public class JaxrsResourceTests {

    protected final Logger logger = LoggerFactory.getLogger(JaxrsResourceTests.class);

    static final int TEST_HTTP_PORT = RandomPortGenerator.get();
    static final int CONNECT_TIMEOUT = 5000;
    static final int READ_TIMEOUT = 5000;
    static final String HTTP = "http";
    static final String JSON_ENTITY = "{\"key\":\"value\"}";

    protected static @NotNull JaxrsHttpServer server;
    @BeforeAll
    public static void setUp() throws Exception {
        final JaxrsHttpServerConfiguration config = new JaxrsHttpServerConfiguration();
        config.setPort(TEST_HTTP_PORT);
        config.addResourceClasses(TestApiResource.class);
        //-- ensure we supplied our own test mapper as this can effect output
        final ObjectMapper mapper = new ObjectMapper();
        config.setObjectMapper(mapper);
        server = new JaxrsHttpServer(mock(), List.of(config), null);
        server.startServer();
    }
    @AfterAll
    public static void tearDown() {
        server.stopServer();
    }

    protected static String getTestServerAddress(final @NotNull String uri) {
        return String.format("%s://%s:%s/%s",
                JaxrsResourceTests.HTTP,
                "localhost",
                JaxrsResourceTests.TEST_HTTP_PORT,
                uri);
    }

    @Test
    public void testGetNotFoundResource() throws IOException {
        final HttpResponse response =
                HttpUrlConnectionClient.get(null, getTestServerAddress("doesnt/exist"), CONNECT_TIMEOUT, READ_TIMEOUT);
        assertEquals(404,response.getStatusCode(),"Resource should not exist");
    }

    @Test
    public void testGetResource() throws IOException {
        final HttpResponse response =
                HttpUrlConnectionClient.get(null, getTestServerAddress("test/get"), CONNECT_TIMEOUT, READ_TIMEOUT);
        assertEquals(200,response.getStatusCode(),"Resource should exist");
    }

    @Test
    public void testHeadResourceOK() throws IOException {
        final HttpResponse response =
                HttpUrlConnectionClient.head(null, getTestServerAddress("test/head"), READ_TIMEOUT);
        assertEquals(200,response.getStatusCode(),"Resource should exist");
    }

    @Test
    public void testHeadResourceNotFound() throws IOException {
        final HttpResponse response =
                HttpUrlConnectionClient.head(null, getTestServerAddress("test/head/not/found"), READ_TIMEOUT);
        assertEquals(404,response.getStatusCode(),"Resource should not exist");
    }

    @Test
    public void testPostJsonResource() throws IOException {
        final HttpResponse response = HttpUrlConnectionClient.post(HttpUrlConnectionClient.JSON_HEADERS,
                getTestServerAddress("test/post/entity"),
                new ByteArrayInputStream(JSON_ENTITY.getBytes(StandardCharsets.UTF_8)),
                CONNECT_TIMEOUT,
                READ_TIMEOUT);
        assertEquals(200,response.getStatusCode(),"Resource should exist");
        assertEquals(JSON_ENTITY,
                new String(response.getResponseBody()),
                "Resource should have been echod back and match");
    }

    @Test
    public void testPostFormDataResource() throws IOException {
        final String formParams = "param1=data1&param2=data2&param3=data3";
        final HttpResponse response = HttpUrlConnectionClient.post(HttpUrlConnectionClient.FORM_HEADERS,
                getTestServerAddress("test/post/formData"),
                new ByteArrayInputStream(formParams.getBytes(StandardCharsets.UTF_8)),
                CONNECT_TIMEOUT,
                READ_TIMEOUT);
        System.err.println(new String(response.getResponseBody()));
        assertEquals(200,response.getStatusCode(),"Resource should exist");
        assertEquals(58,
                response.getContentLength(),
                "Form data should be marshalled back matching expected format");
    }

    @Test
    public void testPutEntity() throws IOException {
        final HttpResponse response = HttpUrlConnectionClient.put(HttpUrlConnectionClient.JSON_HEADERS,
                getTestServerAddress("test/put"),
                new ByteArrayInputStream(JSON_ENTITY.getBytes(StandardCharsets.UTF_8)),
                CONNECT_TIMEOUT,
                READ_TIMEOUT);
        assertEquals(200,response.getStatusCode(),"Resource should exist");
        assertEquals(JSON_ENTITY,
                new String(response.getResponseBody()),
                "Resource should have been echod back and match");
    }

    @Test
    public void testDeleteEntity() throws IOException {
        final HttpResponse response = HttpUrlConnectionClient.delete(HttpUrlConnectionClient.JSON_HEADERS,
                getTestServerAddress("test/delete"),
                new ByteArrayInputStream(JSON_ENTITY.getBytes(StandardCharsets.UTF_8)),
                CONNECT_TIMEOUT,
                READ_TIMEOUT);
        assertEquals(200,response.getStatusCode(),"Resource should exist");
        assertEquals(JSON_ENTITY,
                new String(response.getResponseBody()),
                "Resource should have been echod back and match");
    }

    @Test
    public void testPathParam() throws IOException {
        final HttpResponse response = HttpUrlConnectionClient.get(null,
                getTestServerAddress("test/get/myparam"),
                CONNECT_TIMEOUT,
                READ_TIMEOUT);
        assertEquals(200,response.getStatusCode(),"Resource should exist");
        assertEquals("myparam",
                new String(response.getResponseBody()),
                "Resource should have been echod back and match");
    }

    @Test
    public void testQueryParam() throws IOException {
        final HttpResponse response = HttpUrlConnectionClient.get(null,
                getTestServerAddress("test/get/query?param=foo"),
                CONNECT_TIMEOUT,
                READ_TIMEOUT);
        assertEquals(200,response.getStatusCode(),"Resource should exist");
        assertEquals("foo",
                new String(response.getResponseBody()),
                "Resource should have been echod back and match");
    }
}
