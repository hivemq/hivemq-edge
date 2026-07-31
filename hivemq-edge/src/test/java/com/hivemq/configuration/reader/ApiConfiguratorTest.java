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

    private static @NotNull String users(final @NotNull String @NotNull ... usernames) {
        final StringBuilder sb = new StringBuilder("<users>");
        for (final String username : usernames) {
            sb.append("<user><username>")
                    .append(username)
                    .append("</username><password>pw</password><roles><role>admin</role></roles></user>");
        }
        return sb.append("</users>").toString();
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
        writeConfig(usernameAuth(true) + users("alice"));
        reader.applyConfig();

        assertThat(apiConfigurationService.getAuthModes()).containsExactly(AuthMode.USERNAME_PASSWORD);
    }

    @Test
    public void noAuthConfig_yieldsTheBuiltInAdminAccount() throws Exception {
        // The fully-implicit case (no <username-authentication>, no <users>, no <ldap>, no <oidc>) is the
        // first-boot convenience: the built-in admin account is the only login. This is the ONLY branch in
        // which it survives (see EDG-849).
        writeConfig("");
        reader.applyConfig();

        assertThat(apiConfigurationService.getUserList())
                .singleElement()
                .satisfies(user -> assertThat(user.getUserName()).isEqualTo("admin"));
    }

    @Test
    public void usernameAuthEnabledExplicitlyWithNoSource_isRejected() throws Exception {
        // Local login turned on explicitly but no <users> and no <ldap>: rather than silently injecting the
        // built-in admin account (which would expose default credentials, EDG-849), reject the config.
        writeConfig(usernameAuth(true));

        assertThrows(UnrecoverableException.class, () -> reader.applyConfig());
    }

    @Test
    public void emptyUsersElement_isRejectedByTheSchema() throws Exception {
        // A present <users> must contain at least one <user>; the empty form is no longer accepted, so the
        // "present-but-empty means nobody can log in" ambiguity cannot arise.
        writeConfig(usernameAuth(true) + "<users></users>");

        assertThrows(Exception.class, () -> reader.applyConfig());
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
        writeConfig(usernameAuth(true) + users("alice") + oidc(true));
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
        writeConfig(usernameAuth(true) + users("alice") + oidc(false));
        reader.applyConfig();

        assertThat(apiConfigurationService.getAuthModes()).containsExactly(AuthMode.USERNAME_PASSWORD);
        assertThat(apiConfigurationService.getOidcConfiguration()).isNull();
    }

    @Test
    public void disabledOidcStanzaWithoutRoleMappings_isAccepted() throws Exception {
        // <role-mappings> is only required for an *enabled* OIDC (enforced in code, gated on <enabled>). A
        // disabled stanza is never read into an OidcConfiguration, so it must validate without it -- otherwise
        // pre-staging or temporarily disabling OIDC would be impossible. The XSD makes <role-mappings> optional
        // (minOccurs=0); the code re-check applies it only when enabled.
        writeConfig(usernameAuth(true)
                + users("alice")
                + "<oidc-authentication><enabled>false</enabled></oidc-authentication>");
        reader.applyConfig();

        assertThat(apiConfigurationService.getAuthModes()).containsExactly(AuthMode.USERNAME_PASSWORD);
        assertThat(apiConfigurationService.getOidcConfiguration()).isNull();
    }

    @Test
    public void enabledOidcWithoutRoleMappings_isRejected() throws Exception {
        // An *enabled* OIDC must still declare role mappings; the requirement now lives in the code path
        // (OidcConfiguration.fromEntity), not the schema.
        writeConfig(usernameAuth(false)
                + "<oidc-authentication><enabled>true</enabled>"
                + "<issuer-uri>https://idp.example.com</issuer-uri>"
                + "<client-id>edge</client-id>"
                + "<redirect-uri>https://edge.example.com/api/v1/auth/oidc/callback</redirect-uri>"
                + "</oidc-authentication>");

        assertThrows(Exception.class, () -> reader.applyConfig());
    }

    @Test
    public void oidcWithEmptyRoleMappingsElement_isRejectedByTheSchema() throws Exception {
        // <role-mappings> is optional, but a *present* one must still hold at least one <role-mapping>.
        writeConfig(usernameAuth(false)
                + "<oidc-authentication><enabled>true</enabled>"
                + "<issuer-uri>https://idp.example.com</issuer-uri>"
                + "<client-id>edge</client-id>"
                + "<redirect-uri>https://edge.example.com/api/v1/auth/oidc/callback</redirect-uri>"
                + "<role-mappings></role-mappings>"
                + "</oidc-authentication>");

        assertThrows(Exception.class, () -> reader.applyConfig());
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
        writeConfig(
                usernameAuth(true) + users("alice") + "<oidc-authentication>" + OIDC_FIELDS + "</oidc-authentication>");

        assertThrows(Exception.class, () -> reader.applyConfig());
    }
}
