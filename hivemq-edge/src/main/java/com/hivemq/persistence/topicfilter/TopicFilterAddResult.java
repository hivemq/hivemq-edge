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
package com.hivemq.persistence.topicfilter;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

public class TopicFilterAddResult {

    private final @NotNull TopicFilterPutStatus putStatus;
    private final @Nullable String errorMessage;

    public TopicFilterAddResult(
            final @NotNull TopicFilterPutStatus putStatus, final @Nullable String errorMessage) {
        this.putStatus = putStatus;
        this.errorMessage = errorMessage;
    }

    public static @NotNull TopicFilterAddResult success() {
        return new TopicFilterAddResult(TopicFilterPutStatus.SUCCESS, null);
    }

    public static @NotNull TopicFilterAddResult failed(final @NotNull TopicFilterPutStatus putStatus) {
        return new TopicFilterAddResult(putStatus, null);
    }

    public static @NotNull TopicFilterAddResult failed(
            final @NotNull TopicFilterPutStatus putStatus, final @Nullable String errorMessage) {
        return new TopicFilterAddResult(putStatus, errorMessage);
    }

    public @NotNull TopicFilterPutStatus getTopicFilterPutStatus() {
        return putStatus;
    }

    public @Nullable String getErrorMessage() {
        return errorMessage;
    }

    public enum TopicFilterPutStatus {
        SUCCESS(),
        ALREADY_EXISTS()
    }


}
