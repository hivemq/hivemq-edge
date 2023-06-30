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
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.TestKeyStoreGenerator;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.List;

/**
 * @author Simon L Johnson
 */
public class JaxrsSSLTests {

    protected final Logger logger = LoggerFactory.getLogger(JaxrsSSLTests.class);

    static final int TEST_HTTP_PORT = 8088;
    static final int CONNECT_TIMEOUT = 1000;
    static final int READ_TIMEOUT = 1000;
    static final String HTTPS = "https";

    protected JaxrsHttpServer server;
    @Mock
    private Injector injector;
    protected TestKeyStoreGenerator testKeyStoreGenerator;
    protected SSLContext context;

    @Before
    public void setUp() throws Exception {

        testKeyStoreGenerator = new TestKeyStoreGenerator();
        JaxrsHttpServerConfiguration config = new JaxrsHttpServerConfiguration();
        config.setPort(TEST_HTTP_PORT);
        config.setProtocol(HTTPS);
        context = getSslContext("testpassword");
        config.setSslContext(context);
        config.addResourceClasses(TestApiResource.class);
        config.setHttpsConfigurator(new HttpsConfigurator(context) {
            @Override
            public void configure(final HttpsParameters params) {
                SSLParameters parameters = getSSLContext().getDefaultSSLParameters();
                parameters.setProtocols(new String[]{"TLSv1.2"});
                params.setSSLParameters(parameters);
            }
        });
        //-- ensure we supplied our own test mapper as this can effect output
        ObjectMapper mapper = new ObjectMapper();
        config.setObjectMapper(mapper);
        server = new JaxrsHttpServer(List.of(config), null);
        server.startServer();
    }

    @After
    public void tearDown() {
        server.stopServer();
        testKeyStoreGenerator.release();
    }

    private SSLContext getSslContext(String password) throws Exception {
        final KeyStore keyStore = createKeyStore(password);
        final KeyManagerFactory keyManagerFactory =
                KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password.toCharArray());
        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(getKeyManagers(keyManagerFactory),
                defaultTrustManager(keyStore).getTrustManagers(),
                new SecureRandom());
        return sslContext;
    }

    private TrustManagerFactory defaultTrustManager(KeyStore keyStore) throws GeneralSecurityException {
        TrustManagerFactory trustManagerFactory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);
        return trustManagerFactory;
    }

    private KeyStore createKeyStore(String password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        FileInputStream fis = new FileInputStream(generateKeystore(password));
        keyStore.load(fis, password.toCharArray());
        return keyStore;
    }

    private KeyManager[] getKeyManagers(final KeyManagerFactory keyManagerFactory) {
        final KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();
        return keyManagers;
    }

    protected File generateKeystore(String password) throws Exception {
        final File file = testKeyStoreGenerator.generateKeyStore("teststore", "JKS", password, password);
        file.deleteOnExit();
        return file;
    }

    protected static String getTestServerAddress(String protocol, int port, String uri) {
        String url = String.format("%s://%s:%s/%s", protocol, "localhost", port, uri);
        return url;
    }

    @Test
    public void testGetResource() throws IOException {

        HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier((string, ssls) -> true);

        HttpResponse response = HttpUrlConnectionClient.get(null,
                getTestServerAddress(HTTPS, TEST_HTTP_PORT, "test/get"),
                CONNECT_TIMEOUT,
                READ_TIMEOUT);
        Assert.assertEquals("Resource should exist", 200, response.getStatusCode());
    }

    @Test(expected = SocketException.class)
    public void testGetResourceOnWrongProtocol() throws IOException {
        HttpUrlConnectionClient.get(null,
                getTestServerAddress("http", TEST_HTTP_PORT, "test/get"),
                CONNECT_TIMEOUT,
                READ_TIMEOUT);
    }
}
