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
        final byte serviceCode = buf.get();
        if (serviceCode != REPLY_CODE) {
            throw new Exception("Expected Set_Attribute_Single reply (0x"
                    + Integer.toHexString(REPLY_CODE & 0xFF)
                    + "), got service code 0x"
                    + Integer.toHexString(serviceCode & 0xFF));
        }
        buf.get(); // reserved
        final int status = buf.get();
        final int extStatusSize = buf.get();
        final int[] extStatus = new int[extStatusSize];
        for (int i = 0; i < extStatusSize; ++i) {
            extStatus[i] = buf.getShort();
        }
        if (log != null) {
            log.append("MR Response\n");
            log.append("USINT service           : Set_Attribute_Single reply\n");
            log.append("USINT status            : 0x")
                    .append(Integer.toHexString(status))
                    .append("\n");
        }
        if (status != 0 && status != STATUS_PARTIAL_TRANSFER) {
            throw new CipException(status, extStatusSize > 0 ? extStatus[0] : 0);
        }
        body.decode(buf, available - 4 - 2 * extStatusSize, log);
    }
}
