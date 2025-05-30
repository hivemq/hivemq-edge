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

import java.util.List;

public interface TopicFilterPersistence {


    void sync();

    @NotNull
    TopicFilterAddResult addTopicFilter(@NotNull TopicFilterPojo domainTag);

    @NotNull TopicFilterUpdateResult updateTopicFilter(@NotNull TopicFilterPojo topicFilter);

    @NotNull TopicFilterUpdateResult updateAllTopicFilters(@NotNull List<TopicFilterPojo> topicFilter);


    @NotNull
    TopicFilterDeleteResult deleteTopicFilter(@NotNull String filter);

    @NotNull
    List<TopicFilterPojo> getTopicFilters();

    @Nullable
    TopicFilterPojo getTopicFilter(@NotNull String filter);
}
