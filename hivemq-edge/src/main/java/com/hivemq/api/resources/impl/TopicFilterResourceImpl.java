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
import com.hivemq.api.errors.NotFoundError;
import com.hivemq.api.errors.topicfilters.TopicFilterNotFoundError;
import com.hivemq.api.format.DataUrl;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.edge.api.TopicFiltersApi;
import com.hivemq.edge.api.model.TopicFilterList;
import com.hivemq.persistence.topicfilter.TopicFilterDeleteResult;
import com.hivemq.persistence.topicfilter.TopicFilterPersistence;
import com.hivemq.persistence.topicfilter.TopicFilterPojo;
import com.hivemq.persistence.topicfilter.TopicFilterUpdateResult;
import com.hivemq.util.ErrorResponseUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;
import org.jetbrains.annotations.NotNull;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

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

        final var result = topicFilterPersistence.addTopicFilter(TopicFilterPojo.fromModel(topicFilterModel));
        return switch (result.getTopicFilterPutStatus()) {
            case SUCCESS -> Response.ok().build();
            case TOPIC_NAME_ALREADY_USED ->
                    ErrorResponseUtil.errorResponse(new AlreadyExistsError("The topic filter '" +
                            topicFilterModel.getDescription() +
                            "' cannot be created since another item already exists with the same name."));
            case TOPIC_FILTER_ALREADY_PRESENT ->
                    ErrorResponseUtil.errorResponse(new AlreadyExistsError("The topic filter '" +
                            topicFilterModel.getDescription() +
                            "' cannot be created since another item already exists with the same filter."));
        };
    }

    @Override
    public @NotNull Response getTopicFilters() {
        final List<com.hivemq.edge.api.model.TopicFilter> topicFilterModelList =
                topicFilterPersistence.getTopicFilters().stream().map(TopicFilterPojo::toModel).toList();
        return Response.ok(new TopicFilterList().items(topicFilterModelList)).build();
    }

    @Override
    public @NotNull Response getTopicFilter(final @NotNull String filter) {
        final TopicFilterPojo topicFilter = topicFilterPersistence.getTopicFilter(filter);
        return topicFilter != null ?
                Response.ok(topicFilter.toModel()).build() :
                ErrorResponseUtil.errorResponse(new NotFoundError());
    }

    @Override
    public @NotNull Response getTopicFilterSchema(final @NotNull String filter) {
        final TopicFilterPojo topicFilter = topicFilterPersistence.getTopicFilter(filter);
        if (topicFilter == null) {
            return ErrorResponseUtil.errorResponse(new NotFoundError());
        }

        final DataUrl schemaAsDataUrl = topicFilter.getSchema();
        if (schemaAsDataUrl == null) {
            return ErrorResponseUtil.errorResponse(new NotFoundError());
        }

        final String schema = new String(Base64.getDecoder().decode(schemaAsDataUrl.getData()), StandardCharsets.UTF_8);
        return Response.ok(schema).header("Content-Type", "text/plain; charset=UTF-8").build();
    }

    @Override
    public @NotNull Response deleteTopicFilter(final @NotNull String filterUriEncoded) {
        if (!systemInformation.isConfigWriteable()) {
            return ErrorResponseUtil.errorResponse(new ConfigWritingDisabled());
        }

        final String filter = URLDecoder.decode(filterUriEncoded, StandardCharsets.UTF_8);
        final TopicFilterDeleteResult result = topicFilterPersistence.deleteTopicFilter(filter);
        return switch (result.getTopicFilterDeleteStatus()) {
            case SUCCESS -> Response.ok().build();
            case NOT_FOUND -> ErrorResponseUtil.errorResponse(new TopicFilterNotFoundError(filter));
        };
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
            return ErrorResponseUtil.errorResponse(new BadRequestError("the filter in the path '" +
                    filter +
                    "' (uriEncoded: '" +
                    filterUriEncoded +
                    "')does not fit to the filter in the body '" +
                    topicFilterModel.getTopicFilter() +
                    "'"));
        }

        final TopicFilterUpdateResult result =
                topicFilterPersistence.updateTopicFilter(TopicFilterPojo.fromModel(topicFilterModel));
        return switch (result.getTopicFilterUpdateStatus()) {
            case SUCCESS -> Response.ok().build();
            case NOT_FOUND -> ErrorResponseUtil.errorResponse(new TopicFilterNotFoundError(filter));
            case INTERNAL_ERROR -> ErrorResponseUtil.errorResponse(new InternalServerError(result.getErrorMessage()));
        };
    }

    @Override
    public @NotNull Response updateTopicFilters(final @NotNull TopicFilterList model) {
        if (!systemInformation.isConfigWriteable()) {
            return ErrorResponseUtil.errorResponse(new ConfigWritingDisabled());
        }

        final TopicFilterUpdateResult result = topicFilterPersistence.updateAllTopicFilters(model.getItems()
                .stream()
                .map(TopicFilterPojo::fromModel)
                .toList());
        return switch (result.getTopicFilterUpdateStatus()) {
            case SUCCESS -> Response.ok().build();
            case NOT_FOUND -> ErrorResponseUtil.errorResponse(new NotFoundError());
            case INTERNAL_ERROR -> ErrorResponseUtil.errorResponse(new InternalServerError(result.getErrorMessage()));
        };
    }
}
