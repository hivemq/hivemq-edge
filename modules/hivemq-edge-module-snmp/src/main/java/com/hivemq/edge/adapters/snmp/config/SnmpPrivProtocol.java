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

import org.snmp4j.security.PrivDES;
import org.snmp4j.security.PrivAES128;
import org.snmp4j.security.PrivAES192;
import org.snmp4j.security.PrivAES256;
import org.snmp4j.smi.OID;
import org.jetbrains.annotations.Nullable;

/**
 * SNMPv3 privacy (encryption) protocols.
 */
public enum SnmpPrivProtocol {

    NONE(null),
    DES(PrivDES.ID),
    AES128(PrivAES128.ID),
    AES192(PrivAES192.ID),
    AES256(PrivAES256.ID);

    private final @Nullable OID oid;

    SnmpPrivProtocol(final @Nullable OID oid) {
        this.oid = oid;
    }

    public @Nullable OID getOid() {
        return oid;
    }
}
