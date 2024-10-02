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
package com.hivemq.persistence.domain;

import com.hivemq.api.model.tags.DomainTagModel;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;

@Immutable
public class DomainTag {

    private final @NotNull String tagAddress;
    private final @NotNull String tag;

    public DomainTag(
            final @NotNull String tagAddress, final @NotNull String tag) {
        this.tagAddress = tagAddress;
        this.tag = tag;
    }

    public static DomainTag fromDomainTagEntity(final @NotNull DomainTagModel domainTag) {
        return new DomainTag(domainTag.getTagAddress().getAddress(), domainTag.getTag());
    }

    public @NotNull String getTag() {
        return tag;
    }

    public @NotNull String getTagAddress() {
        return tagAddress;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DomainTag domainTag = (DomainTag) o;
        return tagAddress.equals(domainTag.tagAddress) && tag.equals(domainTag.tag);
    }

    @Override
    public int hashCode() {
        int result = tagAddress.hashCode();
        result = 31 * result + tag.hashCode();
        return result;
    }
}
