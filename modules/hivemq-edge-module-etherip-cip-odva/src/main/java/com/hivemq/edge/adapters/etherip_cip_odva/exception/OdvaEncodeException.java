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
package com.hivemq.edge.adapters.etherip_cip_odva.exception;

import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTag;
import org.jetbrains.annotations.NotNull;

public class OdvaEncodeException extends OdvaException {
    @NotNull
    private final CipTag cipTag;

    public OdvaEncodeException(@NotNull CipTag cipTag, String msg) {
        super(msg);
        this.cipTag = cipTag;
    }

    public OdvaEncodeException(@NotNull CipTag cipTag, Exception e) {
        super(e);
        this.cipTag = cipTag;
    }

    public @NotNull CipTag getCipTag() {
        return cipTag;
    }

    // FIXME: May be done in exception handler
    @Override
    public String getMessage() {
        return cipTag.toConciseString() + ": " + super.getMessage();
    }
}
