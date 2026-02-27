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
package com.hivemq.edge.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hivemq.edge.utils.HiveMQEdgeEnvironmentUtils;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Events to help track usage patterns to help define UX & platform usability
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HiveMQEdgeRemoteEvent {

    public final @NotNull Map<String, String> userData;
    public final @NotNull Date created;
    public final @NotNull EVENT_TYPE eventType;
    public final @NotNull String installationToken;
    public final @NotNull String sessionToken;
    public @Nullable String edgeVersion;

    @SuppressWarnings("JavaUtilDate")
    public HiveMQEdgeRemoteEvent(final @NotNull EVENT_TYPE eventType) {
        this.eventType = eventType;
        this.userData = new HashMap<>();
        this.created = new Date();
        this.installationToken = HiveMQEdgeEnvironmentUtils.generateInstallationToken();
        this.sessionToken = HiveMQEdgeEnvironmentUtils.getSessionToken();
    }

    public @NotNull Date getCreated() {
        return created;
    }

    public @NotNull EVENT_TYPE getEventType() {
        return eventType;
    }

    public @Nullable String getEdgeVersion() {
        return edgeVersion;
    }

    public void setEdgeVersion(final @NotNull String edgeVersion) {
        this.edgeVersion = edgeVersion;
    }

    public @NotNull String getSessionToken() {
        return sessionToken;
    }

    public @NotNull Map<String, String> getUserData() {
        return userData;
    }

    public void addUserData(final @NotNull String propertyName, final @Nullable String propertyValue) {
        this.userData.put(propertyName, propertyValue);
    }

    public void addAll(final @NotNull Map<String, String> map) {
        this.userData.putAll(map);
    }

    @Override
    public @NotNull String toString() {
        return "HiveMQEdgeEvent{" + "userData="
                + userData
                + ", created="
                + created
                + ", eventType="
                + eventType
                + ", installationToken='"
                + installationToken
                + '\''
                + '}';
    }

    public enum EVENT_TYPE {
        EDGE_STARTED,
        EDGE_PING,
        ADAPTER_STARTED,
        ADAPTER_ERROR,
        BRIDGE_STARTED,
        BRIDGE_ERROR
    }
}
