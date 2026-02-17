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
package com.hivemq.security.auth;

import java.security.cert.Certificate;

/**
 * Represents information of a X509 client certificate.
 *
 * @author Christoph Schäbel
 * @since 3.0
 */
public interface SslClientCertificate {

    /**
     * Returns the last certificate in the chain.
     *
     * @return the last certificate in the chain
     */
    Certificate certificate();

    /**
     * Returns the whole certificate chain.
     *
     * @return the whole certificate chain
     */
    Certificate[] certificateChain();

    /**
     * Returns the commonName from the last certificate in the chain.
     *
     * @return the commonName from the last certificate in the chain
     */
    String commonName();

    /**
     * Returns the organization from the last certificate in the chain.
     *
     * @return the organization from the last certificate in the chain
     */
    String organization();

    /**
     * Returns the organizational unit from the last certificate in the chain.
     *
     * @return the organizational unit from the last certificate in the chain
     */
    String organizationalUnit();

    /**
     * Returns the title from the last certificate in the chain.
     *
     * @return the title from the last certificate in the chain
     */
    String title();

    /**
     * Returns the serial number from the last certificate in the chain.
     *
     * @return the serial number from the last certificate in the chain
     */
    String serial();

    /**
     * Returns the country code from the last certificate in the chain.
     *
     * @return the country code from the last certificate in the chain
     */
    String country();

    /**
     * Returns the locality from the last certificate in the chain.
     *
     * @return the locality from the last certificate in the chain
     */
    String locality();

    /**
     * Returns the state from the last certificate in the chain.
     *
     * @return the state from the last certificate in the chain
     */
    String state();
}
