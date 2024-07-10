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
package com.hivemq.protocols.writing;

import com.hivemq.adapter.sdk.api.config.WriteContext;
import com.hivemq.adapter.sdk.api.writing.WriteInput;
import com.hivemq.adapter.sdk.api.writing.WritePayload;
import org.jetbrains.annotations.NotNull;

public class WriteInputImpl<P extends WritePayload, C extends WriteContext> implements WriteInput<P, C> {

    private final @NotNull P payload;
    private final @NotNull C writeContext;

    WriteInputImpl(final @NotNull WritePayload payload, final @NotNull WriteContext writeContext) {
        // we can not cast it in the calling method as it does not know T
        //noinspection unchecked
        this.payload = (P) payload;
        //noinspection unchecked
        this.writeContext = (C) writeContext;
    }

    @Override
    public @NotNull P getWritePayload() {
        return payload;
    }

    @Override
    public @NotNull C getWriteContext() {
        return writeContext;
    }
}
