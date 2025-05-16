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

import org.jetbrains.annotations.NotNull;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

public class KeystoreUtil {


    public static @NotNull List<X509Certificate> getCertificatesFromTruststore(
            final @NotNull String keyStoreType,
            final @NotNull String keyStorePath,
            final @NotNull String keyStorePassword) {
        try (final FileInputStream fileInputStream = new FileInputStream(keyStorePath)) {
            //load keystore from TLS config
            final KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(fileInputStream, keyStorePassword.toCharArray());
            final List<X509Certificate> certificates = new ArrayList<>();
            final Iterator<String> aliasIter = keyStore.aliases().asIterator();
            while (aliasIter.hasNext()) {
                final String alias = aliasIter.next();
                certificates.add((X509Certificate) keyStore.getCertificate(alias));
            }
            return Collections.unmodifiableList(certificates);
        } catch (final FileNotFoundException e) {
            throw new SslException("Cannot find KeyStore at path '" + keyStorePath + "'");
        } catch (final KeyStoreException | IOException e) {
            throw new SslException(String.format("Not able to open or read KeyStore '%s' with type '%s'",
                    keyStorePath,
                    keyStoreType), e);
        } catch (final NoSuchAlgorithmException | CertificateException e) {
            throw new SslException("Not able to read the certificate from KeyStore '" + keyStorePath + "'", e);
        } catch (NoSuchElementException e) {
            throw new SslException("Not able to find key in KeyStore '" + keyStorePath + "'", e);
        }
    }


    public static @NotNull List<X509Certificate> getCertificatesFromDefaultTruststore() {
        //if no truststore is set use java default
        try {
            final List<X509Certificate> certificates = new ArrayList<>();
            // Loads default Root CA certificates (generally, from JAVA_HOME/lib/cacerts)
            final TrustManagerFactory trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore) null);
            return Arrays.stream(trustManagerFactory
                    .getTrustManagers())
                    .flatMap(trustManager -> {
                        if (trustManager instanceof X509TrustManager) {
                            return Arrays.stream(((X509TrustManager) trustManager).getAcceptedIssuers());
                        }
                        return Stream.empty();
                    }).toList();
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            throw new SslException("Not able to load system default truststore", e);
        }
    }

    public static @NotNull KeyPairWithChain getKeysFromKeystore(
            final @NotNull String keyStoreType,
            final @NotNull String keyStorePath,
            final @NotNull String keyStorePassword,
            final @NotNull String privateKeyPassword) {
        try (final FileInputStream fileInputStream = new FileInputStream(keyStorePath)) {
            //load keystore from TLS config
            final KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(fileInputStream, keyStorePassword.toCharArray());
            final String firstAlias = keyStore.aliases().nextElement();
            final PrivateKey privateKey = (PrivateKey) keyStore.getKey(firstAlias, privateKeyPassword.toCharArray());
            final Certificate certificate = keyStore.getCertificate(firstAlias);
            final Certificate[] certificateChain = keyStore.getCertificateChain(firstAlias);

            final X509Certificate certificateX509 = (X509Certificate) certificate;
            final X509Certificate[] certificateChainX509 = new X509Certificate[certificateChain.length];
            for (int i = 0; i < certificateChain.length; i++) {
                certificateChainX509[i] = (X509Certificate) certificateChain[i];
            }

            return new KeyPairWithChain(privateKey, certificateX509, certificateChainX509);
        } catch (final UnrecoverableKeyException e1) {
            throw new SslException(
                    "Not able to recover key from KeyStore, please check your private-key-password and your keyStorePassword",
                    e1);
        } catch (final FileNotFoundException e) {
            throw new SslException("Cannot find KeyStore at path '" + keyStorePath + "'");
        } catch (final KeyStoreException | IOException e) {
            throw new SslException(String.format("Not able to open or read KeyStore '%s' with type '%s'",
                    keyStorePath,
                    keyStoreType), e);
        } catch (final NoSuchAlgorithmException | CertificateException e) {
            throw new SslException("Not able to read the certificate from KeyStore '" + keyStorePath + "'", e);
        } catch (NoSuchElementException e) {
            throw new SslException("Not able to find key in KeyStore '" + keyStorePath + "'", e);
        }
    }

    public static class KeyPairWithChain {

        private final @NotNull PrivateKey privateKey;
        private final @NotNull X509Certificate publicKey;
        private final @NotNull X509Certificate[] certificateChain;

        public KeyPairWithChain(
                final @NotNull PrivateKey privateKey,
                final @NotNull X509Certificate publicKey,
                final @NotNull X509Certificate[] certificateChain) {
            this.privateKey = privateKey;
            this.publicKey = publicKey;
            this.certificateChain = certificateChain;
        }

        public @NotNull PrivateKey getPrivateKey() {
            return privateKey;
        }

        public @NotNull X509Certificate getPublicKey() {
            return publicKey;
        }

        public @NotNull X509Certificate[] getCertificateChain() {
            return certificateChain;
        }
    }
}
