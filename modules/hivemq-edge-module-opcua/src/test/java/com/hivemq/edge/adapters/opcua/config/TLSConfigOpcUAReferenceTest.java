/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.opcua.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hivemq.security.tls.FileSet;
import com.hivemq.security.tls.MaterialRef;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/// How a reference into shared TLS material is read, and what the two ways of writing one refuse.
///
/// References live only on the superseding `tls-*` elements; the older `keystore`, `truststore`,
/// `allowList` and `revocationList` keep exactly the reach they always had.
class TLSConfigOpcUAReferenceTest {

    @Test
    void whenAUuidIsReferenced_thenItIsNotParsedAsAName() throws Exception {
        // A uuid carries no '::'. Through one combined element it would be refused as naming no
        // tls-material, which points the operator at the wrong mistake - which is why there are two.
        final TLSConfigOpcUA config = new TLSConfigOpcUA(Tls.superseding(
                true,
                TlsChecks.STANDARD,
                null,
                null,
                new TlsTruststore(null, null, null, null, null, null, null, null, null, "no-separator-anywhere"),
                null,
                null));

        assertThat(config.truststore()).contains(new MaterialRef.ByUuid("no-separator-anywhere"));
    }

    @Test
    void whenAnElementReferencesBothByNameAndByUuid_thenItIsRefused() {
        assertThatThrownBy(() -> new TLSConfigOpcUA(Tls.superseding(
                        true,
                        TlsChecks.STANDARD,
                        null,
                        null,
                        new TlsTruststore(
                                null, null, null, null, null, null, null, null, "plant::authorities", "some-uuid"),
                        null,
                        null)))
                .isInstanceOf(TLSConfigOpcUA.InvalidTlsConfigException.class)
                .hasMessageContaining("tls-truststore")
                .hasMessageContaining("plant::authorities")
                .hasMessageContaining("some-uuid");
    }

    @Test
    void whenAnElementNamesBothFilesAndAReference_thenItIsRefused() {
        // They can name different material, so honouring one would leave the other line doing
        // nothing - on the setting that decides which authorities Edge trusts.
        assertThatThrownBy(() -> new TLSConfigOpcUA(Tls.superseding(
                        true,
                        TlsChecks.STANDARD,
                        null,
                        null,
                        new TlsTruststore(
                                null,
                                List.of("/etc/edge/ca.pem"),
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                "plant::authorities",
                                null),
                        null,
                        null)))
                .isInstanceOf(TLSConfigOpcUA.InvalidTlsConfigException.class)
                .hasMessageContaining("tls-truststore")
                .hasMessageContaining("plant::authorities");
    }

    @Test
    void whenAReferenceNameHasNoMaterialHalf_thenItIsRefusedAtConfigurationTime() {
        // Caught here rather than when the material is looked up, so the message names the element.
        assertThatThrownBy(() -> new TLSConfigOpcUA(Tls.superseding(
                        true,
                        TlsChecks.STANDARD,
                        null,
                        null,
                        new TlsTruststore(null, null, null, null, null, null, null, null, "authorities", null),
                        null,
                        null)))
                .isInstanceOf(TLSConfigOpcUA.InvalidTlsConfigException.class)
                .hasMessageContaining("tls-truststore")
                .hasMessageContaining("names no tls-material");
    }

    @Test
    void whenFingerprintTrustNamesNoAllowListAtAll_thenItIsStillRefused() {
        assertThatThrownBy(() -> new TLSConfigOpcUA(new Tls(true, TlsChecks.SELF_SIGNED, null, null, null, null, null)))
                .isInstanceOf(TLSConfigOpcUA.InvalidTlsConfigException.class)
                .hasMessageContaining("ALLOW_LIST");
    }

    @Test
    void whenASupersededElementIsEmpty_thenNoMaterialIsNamed() throws Exception {
        // A blank value is absent, not material named with an empty name. The truststore then takes
        // the same path an omitted one does, which under chain trust is the runtime's own
        // authorities; the two lists have no fallback and stay absent.
        final TLSConfigOpcUA config = new TLSConfigOpcUA(new Tls(
                true,
                TlsChecks.STANDARD,
                null,
                null,
                new Truststore("", ""),
                new AllowList(""),
                new RevocationList("")));

        final MaterialRef.Inline truststore =
                (MaterialRef.Inline) config.truststore().orElseThrow();
        assertThat(truststore.files().builtins()).containsExactly(FileSet.JVM_CACERTS);
        assertThat(config.allowList()).isEqualTo(Optional.empty());
        assertThat(config.revocationList()).isEqualTo(Optional.empty());
    }
}
