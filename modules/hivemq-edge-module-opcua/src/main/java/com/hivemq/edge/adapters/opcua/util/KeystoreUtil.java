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

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.security.exception.SslException;

import javax.net.ssl.TrustManager;
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
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class KeystoreUtil {


    public static @NotNull List<X509Certificate> getCertificatesFromTruststore(
            final @NotNull String keyStoreType,
            final @NotNull String keyStorePath,
            final @NotNull String keyStorePassword) {
        try (final FileInputStream fileInputStream = new FileInputStream(keyStorePath)) {
            //load keystore from TLS config
            final KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(fileInputStream, keyStorePassword.toCharArray());
            final ImmutableList.Builder<X509Certificate> certificates = ImmutableList.<X509Certificate>builder();
            final Iterator<String> aliasIter = keyStore.aliases().asIterator();
            while (aliasIter.hasNext()) {
                final String alias = aliasIter.next();
                certificates.add((X509Certificate) keyStore.getCertificate(alias));
            }
            return certificates.build();
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
            final ImmutableList.Builder<X509Certificate> certificates = ImmutableList.<X509Certificate>builder();
            // Loads default Root CA certificates (generally, from JAVA_HOME/lib/cacerts)
            final TrustManagerFactory trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore) null);
            for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
                if (trustManager instanceof X509TrustManager) {
                    for (X509Certificate acceptedIssuer : ((X509TrustManager) trustManager).getAcceptedIssuers()) {
                        certificates.add(acceptedIssuer);
                    }
                }
            }
            return certificates.build();
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
            return new KeyPairWithChain(privateKey,
                    (X509Certificate) certificate,
                    (X509Certificate[]) certificateChain);
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
