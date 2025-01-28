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

import com.hivemq.api.AbstractApi;
import com.hivemq.api.errors.AlreadyExistsError;
import com.hivemq.api.errors.BadRequestError;
import com.hivemq.api.errors.ConfigWritingDisabled;
import com.hivemq.api.errors.InternalServerError;
import com.hivemq.api.errors.topicfilters.TopicFilterNotFoundError;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.edge.api.TopicFiltersApi;
import com.hivemq.edge.api.model.TopicFilterList;
import com.hivemq.persistence.topicfilter.TopicFilterPojo;
import org.jetbrains.annotations.NotNull;
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
public class TopicFilterResourceImpl extends AbstractApi implements TopicFiltersApi {

    private final @NotNull TopicFilterPersistence topicFilterPersistence;
    private final @NotNull SystemInformation systemInformation;

    @Inject
    public TopicFilterResourceImpl(
            final @NotNull TopicFilterPersistence topicFilterPersistence,
            final @NotNull SystemInformation systemInformation) {
        this.topicFilterPersistence = topicFilterPersistence;
        this.systemInformation = systemInformation;
    }

    @Override
    public @NotNull Response addTopicFilters(final @NotNull com.hivemq.edge.api.model.TopicFilter topicFilterModel) {
        if (!systemInformation.isConfigWriteable()) {
            return ErrorResponseUtil.errorResponse(new ConfigWritingDisabled());
        }
        final @NotNull TopicFilterAddResult addResult =
                topicFilterPersistence.addTopicFilter(TopicFilterPojo.fromModel(topicFilterModel));
        final @NotNull String name = topicFilterModel.getDescription();
        switch (addResult.getTopicFilterPutStatus()) {
            case SUCCESS:
                return Response.ok().build();
            case TOPIC_NAME_ALREADY_USED:
                return ErrorResponseUtil.errorResponse(new AlreadyExistsError("The topic filter '" +
                        name +
                        "' cannot be created since another item already exists with the same name."));
            case TOPIC_FILTER_ALREADY_PRESENT:
                return ErrorResponseUtil.errorResponse(new AlreadyExistsError("The topic filter '" +
                        name +
                        "' cannot be created since another item already exists with the same filter."));
            default:
                return ErrorResponseUtil.errorResponse(new InternalServerError("Internal error"));
        }
    }

    @Override
    public @NotNull Response getTopicFilters() {
        final List<com.hivemq.edge.api.model.TopicFilter> topicFilterModelList = topicFilterPersistence.getTopicFilters()
                .stream()
                .map(TopicFilterPojo::toModel)
                .collect(Collectors.toList());
        return Response.ok(new TopicFilterList().items(topicFilterModelList)).build();
    }

    @Override
    public @NotNull Response deleteTopicFilter(final @NotNull String filterUriEncoded) {
        if (!systemInformation.isConfigWriteable()) {
            return ErrorResponseUtil.errorResponse(new ConfigWritingDisabled());
        }
        final String filter = URLDecoder.decode(filterUriEncoded, StandardCharsets.UTF_8);

        final @NotNull TopicFilterDeleteResult deleteResult = topicFilterPersistence.deleteTopicFilter(filter);
        switch (deleteResult.getTopicFilterDeleteStatus()) {
            case SUCCESS:
                return Response.ok().build();
            case NOT_FOUND:
                return ErrorResponseUtil.errorResponse(new TopicFilterNotFoundError(filter));
            default:
                return ErrorResponseUtil.errorResponse(new InternalServerError("Internal Error"));
        }
    }

    @Override
    public @NotNull Response updateTopicFilter(
            final @NotNull String filterUriEncoded,
            final @NotNull com.hivemq.edge.api.model.TopicFilter topicFilterModel) {

        if (!systemInformation.isConfigWriteable()) {
            return ErrorResponseUtil.errorResponse(new ConfigWritingDisabled());
        }
        final String filter = URLDecoder.decode(filterUriEncoded, StandardCharsets.UTF_8);
        if (!filter.equals(topicFilterModel.getTopicFilter())) {
            return ErrorResponseUtil.errorResponse(new BadRequestError(
                    "the filter in the path '" +
                            filter +
                            "' (uriEncoded: '" +
                            filterUriEncoded +
                            "')does not fit to the filter in the body '" +
                            topicFilterModel.getTopicFilter() +
                            "'"));
        }

        final @NotNull TopicFilterUpdateResult updateResult =
                topicFilterPersistence.updateTopicFilter(TopicFilterPojo.fromModel(topicFilterModel));
        switch (updateResult.getTopicFilterUpdateStatus()) {
            case SUCCESS:
                return Response.ok().build();
            case INTERNAL_ERROR:
            default:
                return ErrorResponseUtil.errorResponse(new InternalServerError("Internal Error"));
        }
    }

    @Override
    public @NotNull Response updateTopicFilters(final @NotNull TopicFilterList topicFilterModelList) {
        if (!systemInformation.isConfigWriteable()) {
            return ErrorResponseUtil.errorResponse(new ConfigWritingDisabled());
        }
        final List<TopicFilterPojo> topicFilters = topicFilterModelList.getItems()
                .stream()
                .map(TopicFilterPojo::fromModel)
                .collect(Collectors.toList());
        final @NotNull TopicFilterUpdateResult updateResult =
                topicFilterPersistence.updateAllTopicFilters(topicFilters);
        switch (updateResult.getTopicFilterUpdateStatus()) {
            case SUCCESS:
                return Response.ok().build();
            case INTERNAL_ERROR:
            default:
                return ErrorResponseUtil.errorResponse(new InternalServerError("Internal Error"));
        }
    }
}
