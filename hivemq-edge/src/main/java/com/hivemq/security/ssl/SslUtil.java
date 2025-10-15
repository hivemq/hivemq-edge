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
package com.hivemq.security.ssl;

import com.google.common.collect.ImmutableSet;
import com.hivemq.configuration.service.entity.Tls;
import com.hivemq.security.exception.SslException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Enumeration;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public final class SslUtil {

    private static final Logger log = LoggerFactory.getLogger(SslUtil.class);

    public static @NotNull KeyManagerFactory getKeyManagerFactory(final @NotNull Tls tls) throws SslException {
        return createKeyManagerFactory(tls.getKeystoreType(),
                tls.getKeystorePath(),
                tls.getKeystorePassword(),
                tls.getPrivateKeyPassword());
    }

    public static @NotNull KeyManagerFactory createKeyManagerFactory(
            final @NotNull String keyStoreType,
            final @NotNull String keyStorePath,
            final @NotNull String keyStorePassword,
            final @NotNull String privateKeyPassword) {

        try  {
            //load keystore from TLS config
            final KeyStore keyStore = getKeyStore(keyStoreType, keyStorePassword, keyStorePath);

            //set up KeyManagerFactory with private-key-password from TLS config
            final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, privateKeyPassword.toCharArray());
            return kmf;

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
        }
    }

    private static @NotNull KeyStore getKeyStore(
            final @NotNull String keyStoreType,
            final @NotNull String keyStorePassword,
            final String keyStorePath)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        //First try to load keystore as is
        try (final InputStream fileInputStream = new FileInputStream(keyStorePath)) {
            final KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(fileInputStream, keyStorePassword.toCharArray());
            return keyStore;
        } catch (final IOException ioe) {
            //IOException generally means the keystore can't be read in the given format
            log.debug("Keystore can't be loaded, probably encoded as base64", ioe);
        }

        //We might run in k8s, so let's try if the file is base64 encoded
        try (final InputStream fileInputStream = new ByteArrayInputStream(loadFileContentAndConvertIfBase64Encoded(keyStoreType, keyStorePath))) {
            final KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(fileInputStream, keyStorePassword.toCharArray());
            return keyStore;
        } catch (final IOException ioe) {
            log.debug("Keystore can't be loaded, probably encoded as base64", ioe);
            //If we fail now the file is broken
            throw new SslException(String.format("Not able to open or read KeyStore '%s' with type '%s'",
                    keyStorePath,
                    keyStoreType), ioe);
        }

    }

    private static byte[] loadFileContentAndConvertIfBase64Encoded(
            final @NotNull String keyStoreType,
            final @NotNull String keyStorePath) {
        final byte[] keystoreContent;
        try {
            byte[] loaded = Files.readAllBytes(Path.of(keyStorePath));
            //in containers the keystore might arrive base64 encoded
            try {
                loaded = Base64.getDecoder().decode(loaded);
            } catch (IllegalArgumentException e) {
                //ignored, just means the content isn't base64 encoded
            }
            keystoreContent = loaded;
        } catch (final IOException e) {
            throw new SslException(String.format("Not able to open or read KeyStore '%s' with type '%s'",
                    keyStorePath,
                    keyStoreType), e);
        }
        return keystoreContent;
    }

    public static @Nullable TrustManagerFactory getTrustManagerFactory(final @NotNull Tls tls) throws SslException {
        return isNotBlank(tls.getTruststorePath()) &&
                tls.getTruststoreType() != null &&
                tls.getTruststorePassword() != null ?
                createTrustManagerFactory(tls.getTruststoreType(),
                        tls.getTruststorePath(),
                        tls.getTruststorePassword()) :
                null;
    }

    public static @NotNull TrustManagerFactory createTrustManagerFactory(
            final @NotNull String trustStoreType,
            final @NotNull String trustStorePath,
            final @NotNull String trustStorePassword) {
        try  {
            //load keystore from TLS config
            final KeyStore keyStoreTrust = getKeyStore(trustStoreType, trustStorePassword, trustStorePath);

            //set up TrustManagerFactory
            final TrustManagerFactory tmFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmFactory.init(keyStoreTrust);
            return tmFactory;
        } catch (final FileNotFoundException e) {
            throw new SslException("Cannot find TrustStore at path '" + trustStorePath + "'");
        } catch (final KeyStoreException | IOException e) {
            throw new SslException(String.format("Not able to open or read TrustStore '%s' with type '%s'",
                    trustStorePath,
                    trustStoreType), e);
        } catch (final NoSuchAlgorithmException | CertificateException e) {
            throw new SslException("Not able to read the certificate from TrustStore '" + trustStorePath + "'", e);
        }
    }

    public static @NotNull ImmutableSet<X509Certificate> loadCertificatesFromTruststore(final @Nullable String password,
                                                                                       final @Nullable String type,
                                                                                       final @Nullable Path path)
            throws SslException {
        try (final FileInputStream fileInputStream = new FileInputStream(path.toFile())) {
            //load keystore from TLS config
            final KeyStore keyStore = KeyStore.getInstance(type);
            keyStore.load(fileInputStream,
                    password != null ? password.toCharArray() : new char[]{});
            final Enumeration<String> aliases = keyStore.aliases();

            final ImmutableSet.Builder<X509Certificate> builder = ImmutableSet.builder();

            while (aliases.hasMoreElements()) {
                final String alias = aliases.nextElement();
                final Certificate certificate = keyStore.getCertificate(alias);
                if (certificate instanceof X509Certificate) {
                    builder.add((X509Certificate) certificate);
                }

                final Certificate[] certificateChain = keyStore.getCertificateChain(alias);

                if (certificateChain != null) {
                    for (final Certificate certificateInChain : certificateChain) {
                        if (certificateInChain instanceof X509Certificate) {
                            builder.add((X509Certificate) certificateInChain);
                        }
                    }
                }
            }

            return builder.build();
        } catch (final FileNotFoundException e1) {
            throw new SslException("Cannot find TrustStore at path " + path, e1);
        } catch (final KeyStoreException | IOException e2) {
            throw new SslException("Not able to open or read TrustStore '" +
                    path +
                    "' with type '" +
                    type +
                    "'", e2);
        } catch (final NoSuchAlgorithmException | CertificateException e3) {
            throw new SslException("Not able to read certificate from TrustStore '" +
                    path, e3);
        }
    }

    public static @NotNull SSLContext loadSSLContextFromTruststore(final @Nullable String password,
                                                                   final @Nullable String type,
                                                                   final @Nullable Path path)
            throws SslException {

        //TODO kinda weird, make nicer
        if (password == null && type == null && path == null) {
            try {
                return SSLContext.getDefault();
            } catch (final NoSuchAlgorithmException e) {
                throw new SslException("Not able to initialize default ssl context.", e);
            }
        }
        return loadSSLContextFromTruststore(path, password, type);
    }

    private static @NotNull SSLContext loadSSLContextFromTruststore(
            final @NotNull Path path, final @NotNull String type, final @Nullable String password)
            throws SslException {

        try (final FileInputStream fileInputStream = new FileInputStream(path.toFile())) {
            //We're expecting PKIX format
            final TrustManagerFactory trustMgrFactory = TrustManagerFactory.getInstance("PKIX");
            final KeyStore keyStore = KeyStore.getInstance(type);
            keyStore.load(fileInputStream, password != null ? password.toCharArray() : new char[]{});

            trustMgrFactory.init(keyStore);
            final TrustManager[] customTrustManagers = trustMgrFactory.getTrustManagers();
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, customTrustManagers, null);
            return sslContext;

        } catch (final FileNotFoundException e1) {
            throw new SslException("Cannot find TrustStore at path " + path, e1);
        } catch (final KeyStoreException | IOException e2) {
            throw new SslException("Not able to open or read TrustStore '" + path + "' with type '" + type + "'",
                    e2);
        } catch (final NoSuchAlgorithmException | CertificateException e3) {
            throw new SslException("Not able to read certificate from TrustStore '" + path + "'", e3);
        } catch (final KeyManagementException e4) {
            throw new SslException("Not able to initialize KeyManagement from TrustStore '" + path + "'", e4);
        }
    }


    private SslUtil() {
    }
}
