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
package com.hivemq.api.model.status;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Preconditions;
import com.hivemq.api.json.TimestampToDateConverter;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Date;

/**
 * @author Simon L Johnson
 */
public class Status {

    public enum CONNECTION_STATUS {
        CONNECTED,
        DISCONNECTED,
        STATELESS,
        UNKNOWN,
        ERROR
    }

    public enum RUNTIME_STATUS {
        STARTED,
        STOPPED
    }

    @JsonProperty("connection")
    @Schema(description = "A mandatory connection status field.")
    private final @NotNull CONNECTION_STATUS connection;

    @JsonProperty("runtime")
    @Schema(description = "A object status field.")
    private final @NotNull RUNTIME_STATUS runtime;

    @JsonProperty("id")
    @Schema(description = "The identifier of the object")
    private final @NotNull String id;

    @JsonProperty("message")
    @Schema(description = "A message associated with the state of a connection")
    private @Nullable String message;

    @JsonProperty("startedAt")
    @Schema(description = "The datetime the object was 'started' in the system.",
            format = "date-time", type = "string")
    @JsonSerialize(using = TimestampToDateConverter.Serializer.class)
    @JsonDeserialize(using = TimestampToDateConverter.Deserializer.class)
    private @Nullable Long startedAt;

    @JsonProperty("lastActivity")
    @Schema(description = "The datetime of the last activity through this connection",
            format = "date-time", type = "string")
    @JsonSerialize(using = TimestampToDateConverter.Serializer.class)
    @JsonDeserialize(using = TimestampToDateConverter.Deserializer.class)
    private @Nullable Long lastActivity;

    @JsonProperty("type")
    @Schema(description = "The type of the object")
    private final @NotNull String type;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Status(@NotNull @JsonProperty("runtime") final RUNTIME_STATUS runtime,
                  @NotNull @JsonProperty("connection") final CONNECTION_STATUS connection,
                  @NotNull @JsonProperty("id") final String id,
                  @NotNull @JsonProperty("type") final String type,
                  @Nullable @JsonProperty("startedAt") final Long startedAt,
                  @Nullable @JsonProperty("lastActivity") final Long lastActivity,
                  @Nullable @JsonProperty("message") final String message) {
        this.runtime = runtime;
        this.connection = connection;
        this.id = id;
        this.type = type;
        this.startedAt = startedAt;
        this.lastActivity = lastActivity;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public Long getLastActivity() {
        return lastActivity;
    }

    public Long getStartedAt() {
        return startedAt;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public void setMessage(@Nullable final String message) {
        this.message = message;
    }

    public void setLastActivity(@Nullable final Long lastActivity) {
        this.lastActivity = lastActivity;
    }

    public void setStartedAt(@Nullable final Long startedAt) {
        this.startedAt = startedAt;
    }

    public CONNECTION_STATUS getConnection() {
        return connection;
    }

    public RUNTIME_STATUS getRuntime() {
        return runtime;
    }

    public static Status unknown(@NotNull final RUNTIME_STATUS runtimeStatus, @NotNull final String connectionType, @NotNull final String entityId){
        Preconditions.checkNotNull(connectionType);
        Preconditions.checkNotNull(entityId);
        return new Status(runtimeStatus, CONNECTION_STATUS.UNKNOWN, entityId, connectionType, null, null, null);
    }

    public static Status connected(@NotNull final RUNTIME_STATUS runtimeStatus, @NotNull final String connectionType, @NotNull final String entityId){
        Preconditions.checkNotNull(connectionType);
        Preconditions.checkNotNull(entityId);
        return new Status(runtimeStatus, CONNECTION_STATUS.CONNECTED, entityId, connectionType, null, null, null);
    }

    public static Status disconnected(@NotNull final RUNTIME_STATUS runtimeStatus, @NotNull final String connectionType, @NotNull final String entityId){
        Preconditions.checkNotNull(connectionType);
        Preconditions.checkNotNull(entityId);
        return new Status(runtimeStatus, CONNECTION_STATUS.DISCONNECTED, entityId, connectionType, null, null, null);
    }
}
