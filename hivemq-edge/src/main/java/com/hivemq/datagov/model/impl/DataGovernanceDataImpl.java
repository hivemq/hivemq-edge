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
package com.hivemq.datagov.model.impl;

import com.hivemq.datagov.model.DataGovernanceData;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.publish.PUBLISH;

import java.util.Objects;

/**
 * @author Simon L Johnson
 */
public class DataGovernanceDataImpl implements DataGovernanceData {

    private @NotNull String clientId;
    private @NotNull PUBLISH publish;

    private DataGovernanceDataImpl(
            final @NotNull String clientId, final @NotNull PUBLISH publish) {
        this.clientId = clientId;
        this.publish = publish;
    }

    @Override
    public @NotNull String getClientId() {
        return clientId;
    }

    @Override
    public @NotNull PUBLISH getPublish() {
        return publish;
    }

    @Override
    public void setPublish(@NotNull final PUBLISH publish) {
        this.publish = publish;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DataGovernanceDataImpl that = (DataGovernanceDataImpl) o;
        return Objects.equals(clientId, that.clientId) && Objects.equals(publish, that.publish);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientId, publish);
    }

    @Override
    public @NotNull String toString() {
        final StringBuilder sb = new StringBuilder("DataGovernanceDataImpl{");
        sb.append("clientId='").append(clientId).append('\'');
        sb.append(", publish='").append(publish).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public static class Builder {

        private String clientId;
        private PUBLISH publish;

        public Builder(final @NotNull DataGovernanceData data) {
            withClientId(data.getClientId());
            withPublish(data.getPublish());
        }

        public Builder() {
        }

        public Builder withClientId(final @NotNull String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder withPublish(final @NotNull PUBLISH publish) {
            this.publish = publish;
            return this;
        }

        public DataGovernanceData build() {
            return new DataGovernanceDataImpl(this.clientId, this.publish);
        }
    }
}
