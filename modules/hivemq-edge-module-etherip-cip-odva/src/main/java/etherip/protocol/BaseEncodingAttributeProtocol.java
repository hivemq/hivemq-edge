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

import com.hivemq.edge.adapters.etherip_cip_odva.exception.OdvaException;
import java.nio.ByteBuffer;

public abstract class BaseEncodingAttributeProtocol<T> extends ProtocolAdapter {

    @Override
    public void encode(final ByteBuffer buf, final StringBuilder log) throws Exception {
        writeToBuffer(buf, log);
    }

    protected abstract void writeToBuffer(ByteBuffer buf, StringBuilder log) throws OdvaException;

    @Override
    public int getRequestSize() {
        return internalGetRequestSize();
    }

    protected abstract int internalGetRequestSize();

    public T getValue() {
        throw new UnsupportedOperationException("Process data in writeToBuffer()");
    }
}
