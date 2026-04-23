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
package com.hivemq.edge.adapters.snmp.config;

import org.snmp4j.security.AuthMD5;
import org.snmp4j.security.AuthSHA;
import org.snmp4j.security.AuthHMAC128SHA224;
import org.snmp4j.security.AuthHMAC192SHA256;
import org.snmp4j.security.AuthHMAC256SHA384;
import org.snmp4j.security.AuthHMAC384SHA512;
import org.snmp4j.smi.OID;
import org.jetbrains.annotations.Nullable;

/**
 * SNMPv3 authentication protocols.
 */
public enum SnmpAuthProtocol {

    NONE(null),
    MD5(AuthMD5.ID),
    SHA(AuthSHA.ID),
    SHA224(AuthHMAC128SHA224.ID),
    SHA256(AuthHMAC192SHA256.ID),
    SHA384(AuthHMAC256SHA384.ID),
    SHA512(AuthHMAC384SHA512.ID);

    private final @Nullable OID oid;

    SnmpAuthProtocol(final @Nullable OID oid) {
        this.oid = oid;
    }

    public @Nullable OID getOid() {
        return oid;
    }
}
