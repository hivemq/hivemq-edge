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
package com.hivemq.edge.adapters.opcua.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import util.KeyChain;

import java.io.File;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class KeystoreUtilTest {

    private static final String KEYSTORE_PASSWORD = "password";
    private static final String PRIVATE_KEY_PASSWORD = "password";
    private static final String KEYSTORE_TYPE = "JKS";

    @TempDir
    Path tempDir;

    @Test
    void testGetKeysFromKeystore_withSanUri_extractsApplicationUri() throws Exception {
        // Given
        final String domain = "testclient";
        final String expectedUri = "urn:hivemq:edge:" + domain;

        final KeyChain keyChain = KeyChain.createKeyChain(domain);
        final File keystoreFile = keyChain.wrapInKeyStoreWithPrivateKey(
                tempDir.resolve("test-keystore").toString(),
                domain,
                KEYSTORE_PASSWORD,
                PRIVATE_KEY_PASSWORD
        );

        // When
        final KeystoreUtil.KeyPairWithChain result = KeystoreUtil.getKeysFromKeystore(
                KEYSTORE_TYPE,
                keystoreFile.getAbsolutePath(),
                KEYSTORE_PASSWORD,
                PRIVATE_KEY_PASSWORD
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.applicationUri())
                .as("Application URI should be extracted from certificate SAN")
                .isNotNull()
                .isEqualTo(expectedUri);
    }

    @Test
    void testGetKeysFromKeystore_verifiesAllFieldsPopulated() throws Exception {
        // Given
        final String domain = "client";
        final KeyChain keyChain = KeyChain.createKeyChain(domain);
        final File keystoreFile = keyChain.wrapInKeyStoreWithPrivateKey(
                tempDir.resolve("test-keystore-all-fields").toString(),
                domain,
                KEYSTORE_PASSWORD,
                PRIVATE_KEY_PASSWORD
        );

        // When
        final KeystoreUtil.KeyPairWithChain result = KeystoreUtil.getKeysFromKeystore(
                KEYSTORE_TYPE,
                keystoreFile.getAbsolutePath(),
                KEYSTORE_PASSWORD,
                PRIVATE_KEY_PASSWORD
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.privateKey())
                .as("Private key should be loaded")
                .isNotNull();
        assertThat(result.publicKey())
                .as("Public key (certificate) should be loaded")
                .isNotNull();
        assertThat(result.certificateChain())
                .as("Certificate chain should be loaded")
                .isNotNull()
                .hasSizeGreaterThan(0);
        assertThat(result.applicationUri())
                .as("Application URI should be extracted")
                .isNotNull()
                .startsWith("urn:hivemq:edge:");
    }

    @Test
    void testGetKeysFromKeystore_multipleDomainsInChain_extractsCorrectUri() throws Exception {
        // Given
        final String primaryDomain = "primary";
        final String secondaryDomain = "secondary";
        final String expectedUri = "urn:hivemq:edge:" + primaryDomain;

        // KeyChain creates certificates for both domains, but keystore entry uses primaryDomain
        final KeyChain keyChain = KeyChain.createKeyChain(primaryDomain, secondaryDomain);
        final File keystoreFile = keyChain.wrapInKeyStoreWithPrivateKey(
                tempDir.resolve("test-keystore-multiple").toString(),
                primaryDomain,  // This is the key entry that will be used
                KEYSTORE_PASSWORD,
                PRIVATE_KEY_PASSWORD
        );

        // When
        final KeystoreUtil.KeyPairWithChain result = KeystoreUtil.getKeysFromKeystore(
                KEYSTORE_TYPE,
                keystoreFile.getAbsolutePath(),
                KEYSTORE_PASSWORD,
                PRIVATE_KEY_PASSWORD
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.applicationUri())
                .as("Should extract URI from the primary certificate")
                .isNotNull()
                .isEqualTo(expectedUri);
    }
}
