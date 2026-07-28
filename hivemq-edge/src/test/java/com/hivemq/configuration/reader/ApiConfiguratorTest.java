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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.io.Files;
import com.hivemq.api.config.AuthMode;
import com.hivemq.exceptions.UnrecoverableException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * Drives the real XML parser and {@link ApiConfigurator} over the authentication-mechanism matrix.
 * <p>
 * Local username/password is enabled by an absent {@code <username-authentication>} stanza or an
 * explicit {@code <enabled>true</enabled>}. OIDC is enabled only by a present, enabled stanza. When
 * an {@code <oidc-authentication>} stanza is present, {@code <username-authentication>} must be
 * present too.
 */
public class ApiConfiguratorTest extends AbstractConfigurationTest {

    private static final @NotNull String OIDC_FIELDS = "<issuer-uri>https://idp.example.com</issuer-uri>"
            + "<client-id>edge</client-id>"
            + "<redirect-uri>https://edge.example.com/api/v1/auth/oidc/callback</redirect-uri>"
            + "<role-mappings><role-mapping><idp-role>admins</idp-role><edge-role>admin</edge-role></role-mapping></role-mappings>";

    private void writeConfig(final @NotNull String adminApiBody) throws Exception {
        Files.write(("<hivemq><admin-api>" + adminApiBody + "</admin-api></hivemq>").getBytes(UTF_8), xmlFile);
    }

    private static @NotNull String usernameAuth(final boolean enabled) {
        return "<username-authentication><enabled>" + enabled + "</enabled></username-authentication>";
    }

    private static @NotNull String oidc(final boolean enabled) {
        return "<oidc-authentication><enabled>" + enabled + "</enabled>" + OIDC_FIELDS + "</oidc-authentication>";
    }

    // -- Local username/password.

    @Test
    public void noAuthConfig_defaultsToUsernamePasswordEnabled() throws Exception {
        writeConfig("");
        reader.applyConfig();

        assertThat(apiConfigurationService.getAuthModes()).containsExactly(AuthMode.USERNAME_PASSWORD);
        assertThat(apiConfigurationService.getOidcConfiguration()).isNull();
    }

    @Test
    public void usernameAuthEnabledExplicitly_activatesLocal() throws Exception {
        writeConfig(usernameAuth(true));
        reader.applyConfig();

        assertThat(apiConfigurationService.getAuthModes()).containsExactly(AuthMode.USERNAME_PASSWORD);
    }

    @Test
    public void usernameAuthDisabledWithNoOtherMechanism_isAcceptedAndLeavesNoModes() throws Exception {
        // No lockout floor: disabling every mechanism is the operator's decision.
        writeConfig(usernameAuth(false));
        reader.applyConfig();

        assertThat(apiConfigurationService.getAuthModes()).isEmpty();
        assertThat(apiConfigurationService.getUserList()).isEmpty();
    }

    // -- OIDC.

    @Test
    public void oidcEnabledWithUsernameAuthPresent_activatesBoth() throws Exception {
        writeConfig(usernameAuth(true) + oidc(true));
        reader.applyConfig();

        assertThat(apiConfigurationService.getAuthModes())
                .containsExactlyInAnyOrder(AuthMode.USERNAME_PASSWORD, AuthMode.OPEN_ID);
        assertThat(apiConfigurationService.getOidcConfiguration()).isNotNull();
    }

    @Test
    public void oidcEnabledWithLocalDisabled_activatesOnlyOidc() throws Exception {
        writeConfig(usernameAuth(false) + oidc(true));
        reader.applyConfig();

        assertThat(apiConfigurationService.getAuthModes()).containsExactly(AuthMode.OPEN_ID);
        assertThat(apiConfigurationService.getOidcConfiguration()).isNotNull();
        assertThat(apiConfigurationService.getUserList()).isEmpty();
    }

    @Test
    public void oidcStanzaDisabled_leavesOidcInactive() throws Exception {
        // A present-but-disabled stanza can be pre-staged; it does not activate OIDC.
        writeConfig(usernameAuth(true) + oidc(false));
        reader.applyConfig();

        assertThat(apiConfigurationService.getAuthModes()).containsExactly(AuthMode.USERNAME_PASSWORD);
        assertThat(apiConfigurationService.getOidcConfiguration()).isNull();
    }

    // -- Presence rule: an OIDC stanza requires an explicit <username-authentication>.

    @Test
    public void enabledOidcWithoutUsernameAuthStanza_isRejected() throws Exception {
        writeConfig(oidc(true));

        assertThrows(UnrecoverableException.class, () -> reader.applyConfig());
    }

    @Test
    public void disabledOidcWithoutUsernameAuthStanza_isAlsoRejected() throws Exception {
        // The rule keys on the stanza's presence, not its enabled value.
        writeConfig(oidc(false));

        assertThrows(UnrecoverableException.class, () -> reader.applyConfig());
    }

    // -- Schema: <enabled> is required inside each stanza.

    @Test
    public void usernameAuthWithoutEnabled_isRejectedByTheSchema() throws Exception {
        writeConfig("<username-authentication></username-authentication>");

        assertThrows(Exception.class, () -> reader.applyConfig());
    }

    @Test
    public void oidcWithoutEnabled_isRejectedByTheSchema() throws Exception {
        writeConfig(usernameAuth(true) + "<oidc-authentication>" + OIDC_FIELDS + "</oidc-authentication>");

        assertThrows(Exception.class, () -> reader.applyConfig());
    }
}
