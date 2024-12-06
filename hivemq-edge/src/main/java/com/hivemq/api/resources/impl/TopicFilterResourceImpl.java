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
import com.hivemq.http.error.ErrorType;
import org.jetbrains.annotations.NotNull;
import com.hivemq.persistence.topicfilter.TopicFilter;
import com.hivemq.persistence.topicfilter.TopicFilterAddResult;
import com.hivemq.persistence.topicfilter.TopicFilterDeleteResult;
import com.hivemq.persistence.topicfilter.TopicFilterPersistence;
import com.hivemq.persistence.topicfilter.TopicFilterUpdateResult;
import com.hivemq.util.ErrorResponseUtil;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class TopicFilterResourceImpl implements TopicFilterApi {
    public static final @NotNull ErrorType
            ERROR_TYPE_TOPIC_FILTER_NOT_FOUND = new ErrorType(null, "TopicFilter not found", "No TopicFilter with the given id was found");

    private final @NotNull TopicFilterPersistence topicFilterPersistence;

    @Inject
    public TopicFilterResourceImpl(final @NotNull TopicFilterPersistence topicFilterPersistence) {
        this.topicFilterPersistence = topicFilterPersistence;
    }

    @Override
    public @NotNull Response addTopicFilter(final @NotNull TopicFilterModel topicFilterModel) {
        final @NotNull TopicFilterAddResult addResult =
                topicFilterPersistence.addTopicFilter(TopicFilter.fromTopicFilterModel(topicFilterModel));
        final @NotNull String name = topicFilterModel.getDescription();
        switch (addResult.getTopicFilterPutStatus()) {
            case SUCCESS:
                return Response.ok().build();
            case TOPIC_NAME_ALREADY_USED:
                return ErrorResponseUtil.alreadyExists("The topic filter '" +
                        name +
                        "' cannot be created since another item already exists with the same name.");
            case TOPIC_FILTER_ALREADY_PRESENT:
                return ErrorResponseUtil.alreadyExists("The topic filter '" +
                        name +
                        "' cannot be created since another item already exists with the same filter.");
            default:
                return ErrorResponseUtil.genericError("Internal error");
        }
    }

    @Override
    public @NotNull Response getTopicFilters() {
        final List<TopicFilterModel> topicFilterModelList = topicFilterPersistence.getTopicFilters()
                .stream()
                .map(TopicFilterModel::fromTopicFilter)
                .collect(Collectors.toList());
        return Response.ok(new TopicFilterModelList(topicFilterModelList)).build();
    }

    @Override
    public @NotNull Response deleteTopicFilter(final @NotNull String filterUriEncoded) {
        final String filter = URLDecoder.decode(filterUriEncoded, StandardCharsets.UTF_8);

        final @NotNull TopicFilterDeleteResult deleteResult = topicFilterPersistence.deleteTopicFilter(filter);
        switch (deleteResult.getTopicFilterDeleteStatus()) {
            case SUCCESS:
                return Response.ok().build();
            case NOT_FOUND:
                return ErrorResponseUtil.notFound(ERROR_TYPE_TOPIC_FILTER_NOT_FOUND, filter);
            default:
                return ErrorResponseUtil.genericError("Internal Error");
        }
    }

    @Override
    public @NotNull Response updateTopicFilter(
            final @NotNull String filterUriEncoded, final @NotNull TopicFilterModel topicFilterModel) {
        final String filter = URLDecoder.decode(filterUriEncoded, StandardCharsets.UTF_8);
        if (!filter.equals(topicFilterModel.getTopicFilter())) {
            return ErrorResponseUtil.badRequest(
                    "the filter in the path '" +
                            filter +
                            "' (uriEncoded: '" +
                            filterUriEncoded +
                            "')does not fit to the filter in the body '" +
                            topicFilterModel.getTopicFilter() +
                            "'");
        }

        final @NotNull TopicFilterUpdateResult updateResult =
                topicFilterPersistence.updateTopicFilter(TopicFilter.fromTopicFilterModel(topicFilterModel));
        switch (updateResult.getTopicFilterUpdateStatus()) {
            case SUCCESS:
                return Response.ok().build();
            case INTERNAL_ERROR:
            default:
                return ErrorResponseUtil.genericError("Internal Error");
        }
    }

    @Override
    public @NotNull Response updateTopicFilters(final @NotNull TopicFilterModelList topicFilterModelList) {
        final List<TopicFilter> topicFilters = topicFilterModelList.getItems()
                .stream()
                .map(TopicFilter::fromTopicFilterModel)
                .collect(Collectors.toList());
        final @NotNull TopicFilterUpdateResult updateResult =
                topicFilterPersistence.updateAllTopicFilters(topicFilters);
        switch (updateResult.getTopicFilterUpdateStatus()) {
            case SUCCESS:
                return Response.ok().build();
            case INTERNAL_ERROR:
            default:
                return ErrorResponseUtil.genericError("Internal Error");
        }
    }
}
