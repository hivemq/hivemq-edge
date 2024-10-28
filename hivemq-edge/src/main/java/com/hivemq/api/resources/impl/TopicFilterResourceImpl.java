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
package com.hivemq.api.resources.impl;

import com.hivemq.api.model.topicFilters.TopicFilterModel;
import com.hivemq.api.model.topicFilters.TopicFilterModelList;
import com.hivemq.api.resources.TopicFilterApi;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.persistence.topicfilter.TopicFilter;
import com.hivemq.persistence.topicfilter.TopicFilterAddResult;
import com.hivemq.persistence.topicfilter.TopicFilterDeleteResult;
import com.hivemq.persistence.topicfilter.TopicFilterPersistence;
import com.hivemq.persistence.topicfilter.TopicFilterUpdateResult;
import com.hivemq.util.ErrorResponseUtil;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class TopicFilterResourceImpl implements TopicFilterApi {

    private final @NotNull TopicFilterPersistence topicFilterPersistence;

    @Inject
    public TopicFilterResourceImpl(final @NotNull TopicFilterPersistence topicFilterPersistence) {
        this.topicFilterPersistence = topicFilterPersistence;
    }

    @Override
    public @NotNull Response addTopicFilter(@NotNull final TopicFilterModel topicFilterModel) {
        final @NotNull TopicFilterAddResult addResult =
                topicFilterPersistence.addTopicFilter(TopicFilter.fromTopicFilterModel(topicFilterModel));
        switch (addResult.getTopicFilterPutStatus()) {
            case SUCCESS:
                return Response.ok().build();
            case ALREADY_EXISTS:
                final @NotNull String name = topicFilterModel.getName();

                return ErrorResponseUtil.alreadyExists("The topic filter '" +
                        name +
                        "' cannot be created since another item already exists with the same name.");
        }
        return Response.serverError().build();
    }

    @Override
    public @NotNull Response getTopicFilters() {
        final List<TopicFilterModel> topicFilterModelList = topicFilterPersistence.getTopicFilters()
                .stream()
                .map(TopicFilterModel::fromTopicFilter)
                .collect(Collectors.toList());
        return Response.ok().entity(new TopicFilterModelList(topicFilterModelList)).build();
    }

    @Override
    public @NotNull Response deleteTopicFilter(@NotNull final String name) {
        final @NotNull TopicFilterDeleteResult deleteResult = topicFilterPersistence.deleteTopicFilter(name);
        switch (deleteResult.getTopicFilterDeleteStatus()) {
            case SUCCESS:
                return Response.ok().build();
            case NOT_FOUND:
                return ErrorResponseUtil.notFound("topic filter", name);
        }
        return Response.serverError().build();
    }

    @Override
    public @NotNull Response updateTopicFilter(
            @NotNull final String name,
            final @NotNull TopicFilterModel topicFilterModel) {
        final @NotNull TopicFilterUpdateResult updateResult =
                topicFilterPersistence.updateTopicFilter(name, TopicFilter.fromTopicFilterModel( topicFilterModel));
        switch (updateResult.getTopicFilterUpdateStatus()) {
            case SUCCESS:
                return Response.ok().build();
            case INTERNAL_ERROR:
                return Response.serverError().build();
        }
        return Response.serverError().build();
    }
}
