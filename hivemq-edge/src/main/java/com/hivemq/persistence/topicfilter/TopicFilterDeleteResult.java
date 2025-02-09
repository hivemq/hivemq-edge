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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TopicFilterDeleteResult {

    private final @NotNull TopicFilterDeleteStatus dataPolicyUpdateStatus;
    private final @Nullable String errorMessage;

    public TopicFilterDeleteResult(
            final @NotNull TopicFilterDeleteStatus deleteStatus, final @Nullable String errorMessage) {
        this.dataPolicyUpdateStatus = deleteStatus;
        this.errorMessage = errorMessage;
    }

    public static @NotNull TopicFilterDeleteResult success() {
        return new TopicFilterDeleteResult(TopicFilterDeleteStatus.SUCCESS, null);
    }

    public static @NotNull TopicFilterDeleteResult failed(final @NotNull TopicFilterDeleteStatus putStatus) {
        return new TopicFilterDeleteResult(putStatus, null);
    }

    public static @NotNull TopicFilterDeleteResult failed(
            final @NotNull TopicFilterDeleteStatus deleteResult, final @Nullable String errorMessage) {
        return new TopicFilterDeleteResult(deleteResult, errorMessage);
    }

    public @NotNull TopicFilterDeleteStatus getTopicFilterDeleteStatus() {
        return dataPolicyUpdateStatus;
    }


    public @Nullable String getErrorMessage() {
        return errorMessage;
    }


    public enum TopicFilterDeleteStatus {
        SUCCESS(),
        NOT_FOUND()
    }
}
