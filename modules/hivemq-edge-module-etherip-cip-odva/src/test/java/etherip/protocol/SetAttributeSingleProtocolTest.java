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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import etherip.data.CipException;
import etherip.types.CNService;
import etherip.types.LogicalAddressPath;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class SetAttributeSingleProtocolTest {

    /// A body that writes two fixed bytes and expects no reply data.
    private static final class TwoBytes extends ProtocolAdapter {
        @Override
        public int getRequestSize() {
            return 2;
        }

        @Override
        public void encode(final ByteBuffer buf, final StringBuilder log) {
            buf.put((byte) 0x34).put((byte) 0x12);
        }
    }

    private static byte[] encode(final Protocol protocol) throws Exception {
        final ByteBuffer buf = ByteBuffer.allocate(protocol.getRequestSize()).order(ByteOrder.LITTLE_ENDIAN);
        protocol.encode(buf, new StringBuilder());
        return Arrays.copyOf(buf.array(), buf.position());
    }

    @Test
    void request_isTheUpstreamRouterFramingWithServiceCode0x10() throws Exception {
        for (final LogicalAddressPath path : new LogicalAddressPath[] {
            new LogicalAddressPath(1, 2, 3), new LogicalAddressPath(0x1234, 0x5678, 0x0300)
        }) {
            final byte[] upstream =
                    encode(new MessageRouterProtocol(CNService.Get_Attribute_Single, path, new TwoBytes()));
            upstream[0] = 0x10;

            assertThat(encode(new SetAttributeSingleProtocol(path, new TwoBytes())))
                    .isEqualTo(upstream);
        }
    }

    @Test
    void reply_withStatusZero_isAccepted() throws Exception {
        final ByteBuffer reply =
                ByteBuffer.wrap(new byte[] {(byte) 0x90, 0, 0, 0}).order(ByteOrder.LITTLE_ENDIAN);

        new SetAttributeSingleProtocol(new LogicalAddressPath(1, 2, 3), new TwoBytes()).decode(reply, 4, null);

        assertThat(reply.remaining()).isZero();
    }

    @Test
    void reply_withErrorStatus_throwsCipException() {
        // general status 0x0E (attribute not settable), one extended status word 0x0001
        final ByteBuffer reply =
                ByteBuffer.wrap(new byte[] {(byte) 0x90, 0, 0x0E, 1, 1, 0}).order(ByteOrder.LITTLE_ENDIAN);

        assertThatThrownBy(() -> new SetAttributeSingleProtocol(new LogicalAddressPath(1, 2, 3), new TwoBytes())
                        .decode(reply, 6, null))
                .isInstanceOfSatisfying(CipException.class, e -> {
                    assertThat(e.getStatusCode()).isEqualTo(0x0E);
                    assertThat(e.getExtendedStatusCode()).isEqualTo(1);
                });
    }

    @Test
    void reply_withErrorStatus0xFF_throwsCipException() {
        // general status 0xFF (object specific, see extended status), one extended status word 0x2107
        final ByteBuffer reply = ByteBuffer.wrap(new byte[] {(byte) 0x90, 0, (byte) 0xFF, 1, 0x07, 0x21})
                .order(ByteOrder.LITTLE_ENDIAN);

        assertThatThrownBy(() -> new SetAttributeSingleProtocol(new LogicalAddressPath(1, 2, 3), new TwoBytes())
                        .decode(reply, 6, null))
                .isInstanceOfSatisfying(CipException.class, e -> {
                    assertThat(e.getStatusCode()).isEqualTo(0xFF);
                    assertThat(e.getExtendedStatusCode()).isEqualTo(0x2107);
                });
    }

    @Test
    void reply_forAnotherService_isRefused() {
        final ByteBuffer reply =
                ByteBuffer.wrap(new byte[] {(byte) 0x8E, 0, 0, 0}).order(ByteOrder.LITTLE_ENDIAN);

        assertThatThrownBy(() -> new SetAttributeSingleProtocol(new LogicalAddressPath(1, 2, 3), new TwoBytes())
                        .decode(reply, 4, null))
                .isInstanceOf(SetAttributeSingleProtocol.UnexpectedReplyException.class);
    }
}
