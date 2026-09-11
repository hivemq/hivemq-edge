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
package com.hivemq.configuration.reader;

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.api.ldap.LdapServerEntity;
import com.hivemq.configuration.entity.listener.TCPListenerEntity;
import com.hivemq.configuration.entity.listener.TlsTCPListenerEntity;
import com.hivemq.configuration.entity.listener.tls.ClientAuthenticationModeEntity;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

/**
 * Drives {@code docker/config-k8s.xml} -- the configuration template baked into the container image --
 * through the real render-and-validate pipeline with the environment the Helm chart sets.
 * <p>
 * Every optional element in that template sits behind an {@code ${IF:...}} gate, and a gate the chart
 * forgets to set silently removes the element rather than failing: the truststore, the second and third
 * LDAP server and the client-authentication listener have all been lost that way. These tests pin the
 * env-variable contract so that a gate can no longer go missing unnoticed.
 */
@ExtendWith(SystemStubsExtension.class)
public class ConfigK8sTemplateTest extends AbstractConfigurationTest {

    /** The environment the chart sets on every Edge pod, independent of the enabled features. */
    private static @NotNull Map<String, String> baseEnvironment() {
        final Map<String, String> env = new LinkedHashMap<>();
        env.put("HIVEMQ_PERSISTENCE_MODE", "in-memory");
        env.put("HIVEMQ_MQTT_ENABLED", "true");
        env.put("HIVEMQ_DATAHUB_ENABLED", "false");
        return env;
    }

    /** What the chart adds when no LDAP directory is configured. */
    private static @NotNull Map<String, String> withLocalAdmin(final @NotNull Map<String, String> env) {
        env.put("HIVEMQ_USERS_ENABLED", "true");
        env.put("HIVEMQ_ADMIN_USER", "admin");
        env.put("HIVEMQ_ADMIN_PASSWORD", "hivemq");
        return env;
    }

    private @NotNull HiveMQConfigEntity render(
            final @NotNull EnvironmentVariables environmentVariables, final @NotNull Map<String, String> env)
            throws Exception {
        env.forEach(environmentVariables::set);

        // The chart mounts its adapter/bridge fragment at the absolute path /fragment/config, which a test
        // cannot create. Point the marker at the chart's default fragment content in the temp folder instead.
        final Path fragment = xmlFile.toPath().resolveSibling("fragment");
        Files.writeString(fragment, "<mqtt-bridges></mqtt-bridges>\n<protocol-adapters></protocol-adapters>\n");
        final String template = Files.readString(configK8sTemplate())
                .replace("${FRAGMENT:/fragment/config}", "${FRAGMENT:" + fragment + "}");
        Files.writeString(xmlFile.toPath(), template);

        return reader.applyConfig();
    }

    /**
     * The template lives outside the Gradle source tree, so walk up from the working directory until the
     * repository root is in sight rather than guessing how deep the test happens to run.
     */
    private static @NotNull Path configK8sTemplate() {
        Path candidate = new File("").getAbsoluteFile().toPath();
        for (int depth = 0; depth < 5 && candidate != null; depth++, candidate = candidate.getParent()) {
            final Path template = candidate.resolve("docker").resolve("config-k8s.xml");
            if (java.nio.file.Files.isRegularFile(template)) {
                return template;
            }
        }
        throw new IllegalStateException("docker/config-k8s.xml not found above " + new File("").getAbsolutePath());
    }

    @Test
    public void chartDefaults_yieldAPlainMqttListenerAndTheLocalAdmin(
            final @NotNull EnvironmentVariables environmentVariables) throws Exception {
        final var entity = render(environmentVariables, withLocalAdmin(baseEnvironment()));

        assertThat(entity.getMqttListenerConfig())
                .singleElement()
                .isInstanceOfSatisfying(
                        TCPListenerEntity.class, l -> assertThat(l.getPort()).isEqualTo(1883));
        assertThat(entity.getApiConfig().getUsers()).singleElement().satisfies(user -> assertThat(user.getUserName())
                .isEqualTo("admin"));
    }

    @Test
    public void mqttsWithClientAuth_yieldsExactlyOneTlsListenerCarryingTheTruststore(
            final @NotNull EnvironmentVariables environmentVariables) throws Exception {
        // HIVEMQ_MQTTS_ENABLED must stay unset here: it selects a second listener on the same port 8883,
        // and the one it selects is the variant without a truststore.
        final Map<String, String> env = withLocalAdmin(baseEnvironment());
        env.put("HIVEMQ_MQTTS_CLIENTAUTH_ENABLED", "true");
        env.put("HIVEMQ_MQTTS_CLIENT_AUTH_MODE", "REQUIRED");
        env.put("HIVEMQ_MQTTS_PREFER_SERVER_CIPHER_SUITE", "false");
        env.put("HIVEMQ_MQTTS_KEYSTORE_PATH", "/mqtts/keystore.jks");
        env.put("HIVEMQ_MQTTS_KEYSTORE_PASSWORD", "changeit");
        env.put("HIVEMQ_MQTTS_SECRET_PRIVATE_KEY_PASSWORD", "changeit");
        env.put("HIVEMQ_MQTTS_TRUSTSTORE_PATH", "/mqtts-trust/truststore.jks");
        env.put("HIVEMQ_MQTTS_SECRET_TRUSTSTORE_PASSWORD", "changeit");

        final var entity = render(environmentVariables, env);

        assertThat(entity.getMqttListenerConfig()).hasSize(2);
        assertThat(entity.getMqttListenerConfig())
                .filteredOn(TlsTCPListenerEntity.class::isInstance)
                .singleElement()
                .isInstanceOfSatisfying(TlsTCPListenerEntity.class, listener -> {
                    assertThat(listener.getPort()).isEqualTo(8883);
                    assertThat(listener.getTls()).isNotNull();
                    assertThat(listener.getTls().getClientAuthMode())
                            .isEqualTo(ClientAuthenticationModeEntity.REQUIRED);
                    assertThat(listener.getTls().getTruststoreEntity()).isNotNull();
                });
    }

    @Test
    public void ldapWithThreeServersAndATruststore_survivesTheTemplate(
            final @NotNull EnvironmentVariables environmentVariables) throws Exception {
        final var entity = render(environmentVariables, ldapEnvironment(true));

        final var ldap = entity.getApiConfig().getLdap();
        assertThat(ldap).isNotNull();
        assertThat(ldap.getServers())
                .extracting(LdapServerEntity::getHost)
                .containsExactly("ldap1.example.com", "ldap2.example.com", "ldap3.example.com");
        assertThat(ldap.getTrustStore()).isNotNull();
        assertThat(ldap.getTrustStore().getTrustStorePath()).isEqualTo("/ldap-trust/truststore.jks");
        assertThat(ldap.getBaseDn()).isEqualTo("dc=example,dc=com");
    }

    @Test
    public void ldapWithoutABaseDn_isStillAValidConfiguration(final @NotNull EnvironmentVariables environmentVariables)
            throws Exception {
        // base-dn is optional (legacy mode treats rdns as an absolute DN), so leaving it out must not
        // leave an unresolvable ${ENV:HIVEMQ_LDAP_BASE_DN} behind and abort the boot.
        final var entity = render(environmentVariables, ldapEnvironment(false));

        assertThat(entity.getApiConfig().getLdap()).isNotNull();
        assertThat(entity.getApiConfig().getLdap().getBaseDn()).isNull();
    }

    private static @NotNull Map<String, String> ldapEnvironment(final boolean withBaseDn) {
        final Map<String, String> env = baseEnvironment();
        env.put("HIVEMQ_LDAP_ENABLED", "true");
        env.put("HIVEMQ_LDAP_TLS_MODE", "LDAPS");
        env.put("HIVEMQ_LDAP_TLS_TRUSTSTORE_ENABLED", "true");
        env.put("HIVEMQ_LDAP_TRUSTSTORE_PATH", "/ldap-trust/truststore.jks");
        env.put("HIVEMQ_LDAP_TRUSTSTORE_PASSWORD", "changeit");
        env.put("HIVEMQ_LDAP_SERVER1_HOST", "ldap1.example.com");
        env.put("HIVEMQ_LDAP_SERVER1_PORT", "636");
        env.put("HIVEMQ_LDAP_SERVER2_ENABLED", "true");
        env.put("HIVEMQ_LDAP_SERVER2_HOST", "ldap2.example.com");
        env.put("HIVEMQ_LDAP_SERVER2_PORT", "636");
        env.put("HIVEMQ_LDAP_SERVER3_ENABLED", "true");
        env.put("HIVEMQ_LDAP_SERVER3_HOST", "ldap3.example.com");
        env.put("HIVEMQ_LDAP_SERVER3_PORT", "636");
        env.put("HIVEMQ_LDAP_SIMPLEBIND_RDNS", "cn=admin,dc=example,dc=com");
        env.put("HIVEMQ_LDAP_SIMPLEBIND_PASSWORD", "adminpassword");
        env.put("HIVEMQ_LDAP_UID", "uid");
        env.put("HIVEMQ_LDAP_RDNS", "ou=people");
        env.put("HIVEMQ_LDAP_DIRECTORY_DESCENT", "false");
        env.put("HIVEMQ_LDAP_MAX_CONNECTION", "5");
        env.put("HIVEMQ_LDAP_CONNECT_TIMEOUT_MS", "5000");
        env.put("HIVEMQ_LDAP_RESPONSE_TIMEOUT_MS", "5000");
        env.put("HIVEMQ_LDAP_SEARCH_TIMEOUT_S", "10");
        if (withBaseDn) {
            env.put("HIVEMQ_LDAP_BASE_DN_ENABLED", "true");
            env.put("HIVEMQ_LDAP_BASE_DN", "dc=example,dc=com");
        }
        return env;
    }
}
