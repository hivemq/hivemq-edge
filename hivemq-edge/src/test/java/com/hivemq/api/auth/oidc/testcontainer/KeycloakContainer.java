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
package com.hivemq.api.auth.oidc.testcontainer;

import java.time.Duration;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Testcontainer for a Keycloak OIDC Identity Provider, used to exercise the OIDC login flow
 * ({@code OidcServiceImpl}) against a real IdP — real discovery, JWKS, and ID-token signatures.
 * <p>
 * Boots {@code keycloak start-dev --import-realm} and imports {@code oidc/acme-realm.json} from the
 * test classpath. The realm {@code acme} models a customer's IdP (its roles are {@code acme-admin} /
 * {@code acme-user}); {@code hivemq-edge} is our client registered in it. Test users:
 * {@code alice} (acme-admin), {@code bob} (acme-user), {@code carol} (no role).
 */
public class KeycloakContainer extends GenericContainer<KeycloakContainer> {

    private static final @NotNull String DEFAULT_IMAGE = "quay.io/keycloak/keycloak:26.0";
    private static final int HTTP_PORT = 8080;

    public static final @NotNull String REALM = "acme";
    public static final @NotNull String CLIENT_ID = "hivemq-edge";
    public static final @NotNull String CLIENT_SECRET = "test-client-secret";

    public KeycloakContainer() {
        this(DEFAULT_IMAGE);
    }

    public KeycloakContainer(final @NotNull String dockerImageName) {
        super(DockerImageName.parse(dockerImageName));
        withExposedPorts(HTTP_PORT);
        withEnv("KEYCLOAK_ADMIN", "admin");
        withEnv("KEYCLOAK_ADMIN_PASSWORD", "admin");
        withEnv("KC_HEALTH_ENABLED", "true");
        withCopyFileToContainer(
                MountableFile.forClasspathResource("oidc/acme-realm.json"),
                "/opt/keycloak/data/import/acme-realm.json");
        withCommand("start-dev", "--import-realm");
        waitingFor(Wait.forLogMessage(".*Running the server in development mode.*", 1)
                .withStartupTimeout(Duration.ofMinutes(3)));
    }

    /**
     * The base URL of the running Keycloak (host + mapped port). Only valid after start.
     */
    public @NotNull String getBaseUrl() {
        return "http://" + getHost() + ":" + getMappedPort(HTTP_PORT);
    }

    /**
     * The OIDC issuer URI for the imported realm — what Edge is configured with. Only valid after start.
     */
    public @NotNull String getIssuerUri() {
        return getBaseUrl() + "/realms/" + REALM;
    }
}
