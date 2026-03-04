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
package com.hivemq.edge.adapters.etherip_cip_odva.tag;

import com.google.common.base.Objects;
import com.hivemq.edge.adapters.etherip_cip_odva.config.tag.CipTag;
import com.hivemq.edge.adapters.etherip_cip_odva.exception.OdvaException;
import com.hivemq.edge.adapters.etherip_cip_odva.handler.LogicalAddressPathFactory;
import etherip.types.LogicalAddressPath;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Contains tags decoded from this tagAddress
 */
public class TagGroup {
    private final @NotNull String tagAddress;
    private final @NotNull LogicalAddressPath logicalAddressPath;
    private @Nullable CipTag composite;

    private final List<CipTag> tags = new ArrayList<>();

    public TagGroup(final @NotNull String tagAddress) throws OdvaException {
        this.tagAddress = tagAddress;
        this.logicalAddressPath = LogicalAddressPathFactory.create(tagAddress);
    }

    public void add(@NotNull CipTag cipTag) {
        if (cipTag.isComposite()) {
            this.composite = cipTag;
        } else {
            tags.add(cipTag);
        }
    }

    @NotNull
    public String getTagAddress() {
        return tagAddress;
    }

    @Nullable
    public CipTag getComposite() {
        return composite;
    }

    public boolean hasComposite() {
        return composite != null && composite.isComposite();
    }

    @NotNull
    public LogicalAddressPath getLogicalAddressPath() {
        return logicalAddressPath;
    }

    public List<CipTag> getTags() {
        return tags;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof final TagGroup that)) {
            return false;
        }
        return Objects.equal(tagAddress, that.tagAddress) && Objects.equal(tags, that.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(tagAddress, tags);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("tagAddress", tagAddress)
                .append("tags", tags)
                .toString();
    }

    public String toConciseString() {
        return new ToStringBuilder(this)
                .append("tagAddress", tagAddress)
                .append("tagCount", tags.size())
                .toString();
    }
}
