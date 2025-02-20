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
package com.hivemq.embedded.internal;

import com.hivemq.api.config.ApiListener;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.configuration.service.entity.Listener;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import util.RandomPortGenerator;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This test suite is for testing the embedded HiveMQ server when the Admin API is enabled.
 */
public class EmbeddedHiveMQImplHttpTest {
    @Rule
    public @NotNull TemporaryFolder tmp = new TemporaryFolder();

    private @NotNull File data;
    private @NotNull File license;
    private @NotNull File extensions;
    private @NotNull File conf;
    private int randomPort;
    private int randomApiPort;

    @Before
    public void setUp() throws Exception {
        data = tmp.newFolder("data");
        license = tmp.newFolder("license");
        extensions = tmp.newFolder("extensions");
        conf = tmp.newFolder("conf");
        randomPort = RandomPortGenerator.get();
        randomApiPort = RandomPortGenerator.get();

        final @NotNull String configXmlString = "" +
                "<hivemq>\n" +
                "    <mqtt-listeners>\n" +
                "        <tcp-listener>\n" +
                "            <port>" +
                randomPort +
                "</port>\n" +
                "            <bind-address>0.0.0.0</bind-address>\n" +
                "        </tcp-listener>\n" +
                "    </mqtt-listeners>\n" +
                "    <admin-api>\n" +
                "        <enabled>true</enabled>\n" +
                "        <listeners>\n" +
                "            <http-listener>\n" +
                "                <port>" +
                randomApiPort +
                "</port>\n" +
                "                <bind-address>0.0.0.0</bind-address>\n" +
                "            </http-listener>\n" +
                "        </listeners>\n" +
                "    </admin-api>\n" +
                "</hivemq>";
        FileUtils.write(new File(conf, "config.xml"), configXmlString, StandardCharsets.UTF_8);
    }

    @Test(timeout = 20000L)
    public void embeddedHiveMQ_whenAdminApiEnabled_readsConfig() throws Exception {
        try (final EmbeddedHiveMQImpl embeddedHiveMQ = new EmbeddedHiveMQImpl(conf, data, extensions, license)) {
            embeddedHiveMQ.start().join();

            final ConfigurationService configurationService = embeddedHiveMQ.bootstrapConfig();

            final List<Listener> mqttListeners = configurationService.listenerConfiguration().getListeners();
            assertThat(mqttListeners.size()).as("The listener count should be 1").isOne();
            assertThat(mqttListeners.get(0).getPort()).as("The MQTT port should match").isEqualTo(randomPort);

            final List<ApiListener> apiListeners = configurationService.apiConfiguration().getListeners();
            assertThat(apiListeners.size()).as("The API listener count should be 1").isOne();
            assertThat(apiListeners.get(0).getPort()).as("The Admin API port should match").isEqualTo(randomApiPort);

            final HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
            final HttpRequest httpRequest =
                    HttpRequest.newBuilder().uri(URI.create("http://localhost:" + randomApiPort + "/")).GET().build();

            final HttpResponse<String> httpResponse =
                    httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(httpResponse.statusCode()).as("The status code should be 307").isEqualTo(307);
            HttpHeaders httpHeaders = httpResponse.headers();
            List<String> locations = httpHeaders.allValues("Location");
            assertThat(locations.size()).as("Location should exist in the response headers").isOne();
            assertThat(locations.get(0)).as("Location should be relative").isEqualTo("/app/");
            assertThat(httpResponse.body()).as("Body should be empty").isEqualTo("");

            embeddedHiveMQ.stop().join();
        }
    }
}
