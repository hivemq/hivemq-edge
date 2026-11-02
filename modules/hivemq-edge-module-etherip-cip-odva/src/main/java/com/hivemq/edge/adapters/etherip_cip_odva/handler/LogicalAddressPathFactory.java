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
package com.hivemq.edge.adapters.etherip_cip_odva.handler;

import com.hivemq.edge.adapters.etherip_cip_odva.exception.OdvaException;
import etherip.types.LogicalAddressPath;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

public class LogicalAddressPathFactory {

    private LogicalAddressPathFactory() {}

    @NotNull
    public static LogicalAddressPath create(String tagAddress) throws OdvaException {
        return parse(tagAddress, LogicalAddressPath::new);
    }

    @VisibleForTesting
    static LogicalAddressPath parse(String tagAddress, LogicalAddressPathProducer producer) throws OdvaException {
        String[] pathElements = StringUtils.split(tagAddress, "@/");
        if (pathElements == null || pathElements.length != 3) {
            throw new OdvaException(
                    "Expected tagAddress to contain '@class/instance/attribute' but is '" + tagAddress + "'");
        }

        final int classCode;
        final int instance;
        final int attribute;
        try {
            classCode = Integer.parseInt(pathElements[0]);
            instance = Integer.parseInt(pathElements[1]);
            attribute = Integer.parseInt(pathElements[2]);
        } catch (Exception e) {
            throw new OdvaException("Error parsing tagAddress=" + tagAddress, e);
        }

        // The etherip library encodes a value above 65535 by casting it to 16 bits, so such an address would
        // silently read or write a different object (EDG-1181). Refuse it, which refuses the adapter's start.
        requireSixteenBits(classCode, "class", tagAddress);
        requireSixteenBits(instance, "instance", tagAddress);
        requireSixteenBits(attribute, "attribute", tagAddress);

        try {
            return producer.produce(classCode, instance, attribute);
        } catch (Exception e) {
            throw new OdvaException("Error parsing tagAddress=" + tagAddress, e);
        }
    }

    private static void requireSixteenBits(final int value, final @NotNull String part, final String tagAddress)
            throws OdvaException {
        if (value < 0 || value > 0xFFFF) {
            throw new OdvaException("The " + part + " in tagAddress '" + tagAddress + "' is " + value
                    + ", outside the range 0 to 65535 that EtherNet/IP Logical Addressing supports");
        }
    }

    interface LogicalAddressPathProducer {
        LogicalAddressPath produce(int classCode, int instance, int attribute);
    }
}
