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
package com.hivemq.edge.adapters.etherip.model.dataypes;

import com.hivemq.edge.adapters.etherip.model.EtherIpValue;

import java.util.Objects;

public class EtherIpLong implements EtherIpValue {
    private final Long value;
    private final String tagAddress;

    public EtherIpLong(final String tagAddress, final Number value) {
        this.value = value.longValue();
        this.tagAddress = tagAddress;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public String getTagAdress() {
        return tagAddress;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EtherIpLong that = (EtherIpLong) o;
        return Objects.equals(value, that.value) && Objects.equals(tagAddress, that.tagAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, tagAddress);
    }
}
