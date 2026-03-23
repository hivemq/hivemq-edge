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
import com.hivemq.api.errors.ConfigWritingDisabled;
import com.hivemq.api.errors.NotFoundError;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.reader.TopicBufferExtractor;
import com.hivemq.edge.api.TopicBuffersApi;
import com.hivemq.edge.api.model.TopicBufferSubscriptionList;
import com.hivemq.util.ErrorResponseUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.jetbrains.annotations.NotNull;

@Singleton
public class TopicBufferResourceImpl extends AbstractApi implements TopicBuffersApi {

    private final @NotNull SystemInformation systemInformation;
    private final @NotNull TopicBufferExtractor topicBufferExtractor;

    @Inject
    public TopicBufferResourceImpl(
            final @NotNull SystemInformation systemInformation,
            final @NotNull TopicBufferExtractor topicBufferExtractor) {
        this.systemInformation = systemInformation;
        this.topicBufferExtractor = topicBufferExtractor;
    }

    @Override
    public @NotNull Response getTopicBufferSubscriptions() {
        final List<com.hivemq.edge.api.model.TopicBufferSubscription> items =
                topicBufferExtractor.getAllSubscriptions().stream()
                        .map(TopicBufferResourceImpl::toModel)
                        .toList();
        return Response.ok(new TopicBufferSubscriptionList().items(items)).build();
    }

    @Override
    public @NotNull Response addTopicBufferSubscription(
            final @NotNull com.hivemq.edge.api.model.TopicBufferSubscription body) {
        if (!systemInformation.isConfigWriteable()) {
            return ErrorResponseUtil.errorResponse(new ConfigWritingDisabled());
        }
        final boolean added = topicBufferExtractor.addSubscription(fromModel(body));
        if (!added) {
            return ErrorResponseUtil.errorResponse(new AlreadyExistsError(
                    "Topic buffer subscription already exists for filter: " + body.getTopicFilter()));
        }
        return Response.ok().build();
    }

    @Override
    public @NotNull Response updateTopicBufferSubscription(
            final @NotNull String topicFilter, final @NotNull com.hivemq.edge.api.model.TopicBufferSubscription body) {
        if (!systemInformation.isConfigWriteable()) {
            return ErrorResponseUtil.errorResponse(new ConfigWritingDisabled());
        }
        final boolean updated = topicBufferExtractor.updateSubscription(fromModel(body));
        if (!updated) {
            return ErrorResponseUtil.errorResponse(new NotFoundError());
        }
        return Response.ok().build();
    }

    @Override
    public @NotNull Response deleteTopicBufferSubscription(final @NotNull String topicFilter) {
        if (!systemInformation.isConfigWriteable()) {
            return ErrorResponseUtil.errorResponse(new ConfigWritingDisabled());
        }
        final boolean deleted = topicBufferExtractor.deleteSubscription(topicFilter);
        if (!deleted) {
            return ErrorResponseUtil.errorResponse(new NotFoundError());
        }
        return Response.ok().build();
    }

    private static @NotNull com.hivemq.edge.api.model.TopicBufferSubscription toModel(
            final @NotNull com.hivemq.topicbuffer.model.TopicBufferSubscription sub) {
        return new com.hivemq.edge.api.model.TopicBufferSubscription()
                .topicFilter(sub.topicFilter())
                .maxMessages(sub.maxMessages());
    }

    private static @NotNull com.hivemq.topicbuffer.model.TopicBufferSubscription fromModel(
            final @NotNull com.hivemq.edge.api.model.TopicBufferSubscription model) {
        return new com.hivemq.topicbuffer.model.TopicBufferSubscription(model.getTopicFilter(), model.getMaxMessages());
    }
}
