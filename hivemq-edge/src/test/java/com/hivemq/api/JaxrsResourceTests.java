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
import com.hivemq.bootstrap.ioc.Injector;
import com.hivemq.http.JaxrsHttpServer;
import com.hivemq.http.config.JaxrsHttpServerConfiguration;
import com.hivemq.http.core.HttpResponse;
import com.hivemq.http.core.HttpUrlConnectionClient;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author Simon L Johnson
 */
public class JaxrsResourceTests {

    protected final Logger logger = LoggerFactory.getLogger(JaxrsResourceTests.class);

    static final int TEST_HTTP_PORT = 8088;
    static final int CONNECT_TIMEOUT = 1000;
    static final int READ_TIMEOUT = 1000;
    static final String HTTP = "http";
    static final String HTTPS = "https";
    static final String JSON_ENTITY = "{\"key\":\"value\"}";

    protected static JaxrsHttpServer server;

    @Mock
    private static Injector injector;

    @BeforeClass
    public static void setUp() throws Exception {
        JaxrsHttpServerConfiguration config = new JaxrsHttpServerConfiguration();
        config.setPort(TEST_HTTP_PORT);
        config.addResourceClasses(TestApiResource.class);
        //-- ensure we supplied our own test mapper as this can effect output
        ObjectMapper mapper = new ObjectMapper();
        config.setObjectMapper(mapper);
        server = new JaxrsHttpServer(List.of(config), null);
        server.startServer();
    }

    @AfterClass
    public static void tearDown(){
        server.stopServer();
    }

    protected static String getTestServerAddress(String protocol, int port, String uri){
        String url = String.format("%s://%s:%s/%s", protocol, "localhost", port, uri);
        return url;
    }

    @Test
    public void testGetNotFoundResource() throws IOException {
        HttpResponse response =
                HttpUrlConnectionClient.get(null,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "doesnt/exist"), CONNECT_TIMEOUT, READ_TIMEOUT);
        Assert.assertEquals("Resource should not exist", 404, response.getStatusCode());
    }

    @Test
    public void testGetResource() throws IOException {
        HttpResponse response =
                HttpUrlConnectionClient.get(null,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/get"), CONNECT_TIMEOUT, READ_TIMEOUT);
        Assert.assertEquals("Resource should exist", 200, response.getStatusCode());
    }

    @Test
    public void testHeadResourceOK() throws IOException {
        HttpResponse response =
                HttpUrlConnectionClient.head(null,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/head"), READ_TIMEOUT);
        Assert.assertEquals("Resource should exist", 200, response.getStatusCode());
    }

    @Test
    public void testHeadResourceNotFound() throws IOException {
        HttpResponse response =
                HttpUrlConnectionClient.head(null,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/head/not/found"), READ_TIMEOUT);
        Assert.assertEquals("Resource should not exist", 404, response.getStatusCode());
    }

    @Test
    public void testPostJsonResource() throws IOException {
        HttpResponse response =
                HttpUrlConnectionClient.post(HttpUrlConnectionClient.JSON_HEADERS,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/post/entity"),
                        new ByteArrayInputStream(JSON_ENTITY.getBytes(StandardCharsets.UTF_8)),
                        CONNECT_TIMEOUT, READ_TIMEOUT);
        Assert.assertEquals("Resource should exist", 200, response.getStatusCode());
        Assert.assertEquals("Resource should have been echod back and match", JSON_ENTITY, new String(response.getResponseBody()));
    }

    @Test
    public void testPostFormDataResource() throws IOException {
        String formParams  = "param1=data1&param2=data2&param3=data3";
        HttpResponse response =
                HttpUrlConnectionClient.post(HttpUrlConnectionClient.FORM_HEADERS,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/post/formData"),
                        new ByteArrayInputStream(formParams.getBytes(StandardCharsets.UTF_8)),
                        CONNECT_TIMEOUT, READ_TIMEOUT);
        System.err.println(new String(response.getResponseBody()));
        Assert.assertEquals("Resource should exist", 200, response.getStatusCode());
        Assert.assertEquals("Form data should be marshalled back matching expected format", 58, response.getContentLength());
    }

    @Test
    public void testPutEntity() throws IOException {
        HttpResponse response =
                HttpUrlConnectionClient.put(HttpUrlConnectionClient.JSON_HEADERS,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/put"),
                        new ByteArrayInputStream(JSON_ENTITY.getBytes(StandardCharsets.UTF_8)),
                        CONNECT_TIMEOUT, READ_TIMEOUT);
        Assert.assertEquals("Resource should exist", 200, response.getStatusCode());
        Assert.assertEquals("Resource should have been echod back and match", JSON_ENTITY, new String(response.getResponseBody()));
    }

    @Test
    public void testDeleteEntity() throws IOException {
        HttpResponse response =
                HttpUrlConnectionClient.delete(HttpUrlConnectionClient.JSON_HEADERS,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/delete"),
                        new ByteArrayInputStream(JSON_ENTITY.getBytes(StandardCharsets.UTF_8)),
                        CONNECT_TIMEOUT, READ_TIMEOUT);
        Assert.assertEquals("Resource should exist", 200, response.getStatusCode());
        Assert.assertEquals("Resource should have been echod back and match", JSON_ENTITY, new String(response.getResponseBody()));
    }

    @Test
    public void testPathParam() throws IOException {
        HttpResponse response =
                HttpUrlConnectionClient.get(null,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/get/myparam"),
                        CONNECT_TIMEOUT, READ_TIMEOUT);
        Assert.assertEquals("Resource should exist", 200, response.getStatusCode());
        Assert.assertEquals("Resource should have been echod back and match", "myparam", new String(response.getResponseBody()));
    }

    @Test
    public void testQueryParam() throws IOException {
        HttpResponse response =
                HttpUrlConnectionClient.get(null,
                        getTestServerAddress(HTTP, TEST_HTTP_PORT, "test/get/query?param=foo"),
                        CONNECT_TIMEOUT, READ_TIMEOUT);
        Assert.assertEquals("Resource should exist", 200, response.getStatusCode());
        Assert.assertEquals("Resource should have been echod back and match", "foo", new String(response.getResponseBody()));
    }
}
