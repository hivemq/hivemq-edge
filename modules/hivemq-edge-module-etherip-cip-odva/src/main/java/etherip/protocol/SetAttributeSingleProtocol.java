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
package etherip.protocol;

import etherip.data.CipException;
import etherip.types.CNPath;
import java.nio.ByteBuffer;

/// A Message Router request for the CIP service Set_Attribute_Single (0x10), and its reply.
///
/// The upstream `MessageRouterProtocol` takes the service from the `CNService` enum, which has no
/// constant for this service and cannot be extended; it also rejects any reply code the enum does
/// not know. This class frames the request and reads the reply the same way, with the service code
/// held as a plain byte.
public class SetAttributeSingleProtocol extends ProtocolAdapter {

    public static final byte SERVICE_CODE = 0x10;
    public static final byte REPLY_CODE = (byte) (SERVICE_CODE | 0x80);

    /// CIP general status "Reply Data Too Large": the upstream router treats it as a partial
    /// transfer rather than an error, and so does this class.
    private static final int STATUS_PARTIAL_TRANSFER = 0x06;

    private final CNPath path;
    private final Protocol body;

    public SetAttributeSingleProtocol(final CNPath path, final Protocol body) {
        this.path = path;
        this.body = body;
    }

    @Override
    public int getRequestSize() {
        return 2 + path.getRequestSize() + body.getRequestSize();
    }

    @Override
    public void encode(final ByteBuffer buf, final StringBuilder log) throws Exception {
        buf.put(SERVICE_CODE);
        path.encode(buf, log);
        if (log != null) {
            log.append("MR Request\n");
            log.append("USINT service           : Set_Attribute_Single\n");
            log.append("USINT path              : ").append(path).append("\n");
        }
        body.encode(buf, log);
    }

    @Override
    public int getResponseSize(final ByteBuffer buf) {
        throw new IllegalStateException("Unknown response size");
    }

    @Override
    public void decode(final ByteBuffer buf, final int available, final StringBuilder log) throws Exception {
        // The whole header is read before the service code is judged, so that a device's error status is
        // reported even in a reply for the wrong service. Upstream MessageRouterProtocol, too, reads the status
        // before it compares the reply's service with the request's.
        if (buf.remaining() < 4) {
            throw new Exception(
                    "Set_Attribute_Single reply cut short: " + buf.remaining() + " bytes, at least 4 expected");
        }
        final int serviceCode = buf.get() & 0xFF;
        buf.get(); // reserved
        final int status = buf.get() & 0xFF;
        final int extStatusSize = buf.get() & 0xFF;
        if (buf.remaining() < 2 * extStatusSize) {
            throw new Exception("Set_Attribute_Single reply cut short: " + extStatusSize
                    + " extended status words announced, " + buf.remaining() + " bytes left");
        }
        final int[] extStatus = new int[extStatusSize];
        for (int i = 0; i < extStatusSize; ++i) {
            extStatus[i] = buf.getShort() & 0xFFFF;
        }
        if (log != null) {
            log.append("MR Response\n");
            log.append("USINT service           : 0x")
                    .append(Integer.toHexString(serviceCode))
                    .append("\n");
            log.append("USINT status            : 0x")
                    .append(Integer.toHexString(status))
                    .append("\n");
        }
        final int extendedStatus = extStatusSize > 0 ? extStatus[0] : 0;
        if (serviceCode != (REPLY_CODE & 0xFF)) {
            throw new UnexpectedReplyException(serviceCode, status, extendedStatus);
        }
        if (isError(status)) {
            // As a signed byte, as upstream MessageRouterProtocol passes it: CipException's status table has no
            // description for 0xFF, and the constructor fails on that entry when it is given 255.
            throw new CipException((byte) status, extendedStatus);
        }
        body.decode(buf, available - 4 - 2 * extStatusSize, log);
    }

    private static boolean isError(final int status) {
        return status != 0 && status != STATUS_PARTIAL_TRANSFER;
    }

    /// The device answered the write with a reply for another service. Its status is kept: an error status is
    /// still the device's reason for refusing the write, while a success status leaves open whether the value
    /// was written.
    public static final class UnexpectedReplyException extends Exception {

        private final int serviceCode;
        private final int status;
        private final int extendedStatus;

        UnexpectedReplyException(final int serviceCode, final int status, final int extendedStatus) {
            super(String.format(
                    "Expected Set_Attribute_Single reply (0x%02X), got service code 0x%02X with status 0x%02X",
                    REPLY_CODE & 0xFF, serviceCode, status));
            this.serviceCode = serviceCode;
            this.status = status;
            this.extendedStatus = extendedStatus;
        }

        public int getServiceCode() {
            return serviceCode;
        }

        public int getStatus() {
            return status;
        }

        public int getExtendedStatus() {
            return extendedStatus;
        }

        public boolean isDeviceError() {
            return isError(status);
        }
    }
}
