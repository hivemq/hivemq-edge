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
package com.hivemq.mqtt.handler.publish;

import com.hivemq.api.mqtt.PublishReturnCode;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PublishingResult {

    public static final @NotNull PublishingResult DELIVERED = new PublishingResult(PublishReturnCode.DELIVERED, null);
    public static final @NotNull PublishingResult NO_MATCHING_SUBSCRIBERS =
            new PublishingResult(PublishReturnCode.NO_MATCHING_SUBSCRIBERS, null);

    private final @NotNull PublishReturnCode publishReturnCode;
    private final @Nullable String reasonString;
    private final @NotNull AckReasonCode ackReasonCode;

    private PublishingResult(final @NotNull PublishReturnCode publishReturnCode, final @Nullable String reasonString) {
        this(publishReturnCode, reasonString, null);
    }

    private PublishingResult(
            final @NotNull PublishReturnCode publishReturnCode,
            final @Nullable String reasonString,
            final @Nullable AckReasonCode ackReasonCode) {
        this.publishReturnCode = publishReturnCode;
        this.reasonString = reasonString;
        this.ackReasonCode = ackReasonCode;
    }

    public static @NotNull PublishingResult failed(final @Nullable String reasonString, final @NotNull AckReasonCode ackReasonCode) {
        return new PublishingResult(PublishReturnCode.FAILED, reasonString, ackReasonCode);
    }

    public @NotNull PublishReturnCode getPublishReturnCode() {
        return publishReturnCode;
    }

    public @Nullable String getReasonString() {
        return reasonString;
    }

    public @NotNull AckReasonCode getAckReasonCode() {
        return ackReasonCode;
    }

    @Override
    public @NotNull String toString() {
        return "PublishingResult{" +
                "publishReturnCode=" +
                publishReturnCode +
                ", reasonString='" +
                reasonString +
                '\'' +
                '}';
    }
}
