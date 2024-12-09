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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.api.format.DataUrl;
import com.hivemq.api.model.samples.PayloadSample;
import com.hivemq.api.model.samples.PayloadSampleList;
import com.hivemq.api.resources.SamplingApi;
import org.jetbrains.annotations.NotNull;
import com.hivemq.sampling.SamplingService;
import com.hivemq.util.ErrorResponseUtil;
import com.saasquatch.jsonschemainferrer.AdditionalPropertiesPolicies;
import com.saasquatch.jsonschemainferrer.JsonSchemaInferrer;
import com.saasquatch.jsonschemainferrer.RequiredPolicies;
import com.saasquatch.jsonschemainferrer.SpecVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

@Singleton
public class SamplingResourceImpl implements SamplingApi {


    private static final @NotNull Logger log = LoggerFactory.getLogger(SamplingResourceImpl.class);
    private static final @NotNull JsonSchemaInferrer INFERRER = JsonSchemaInferrer.newBuilder()
            .setSpecVersion(SpecVersion.DRAFT_07)
            .setAdditionalPropertiesPolicy(AdditionalPropertiesPolicies.notAllowed())
            .setRequiredPolicy(RequiredPolicies.nonNullCommonFields())
            .build();

    private final @NotNull SamplingService samplingService;
    private final @NotNull ObjectMapper objectMapper;


    @Inject
    public SamplingResourceImpl(
            final @NotNull SamplingService samplingService, final @NotNull ObjectMapper objectMapper) {
        this.samplingService = samplingService;
        this.objectMapper = objectMapper;
    }

    @Override
    public @NotNull Response getSamplesForTopic(final @NotNull String topicUrlEncoded) {
        final String topic = URLDecoder.decode(topicUrlEncoded, StandardCharsets.UTF_8);

        final List<byte[]> samples = samplingService.getSamples(topic);
        final ArrayList<PayloadSample> sampleArrayList = new ArrayList<>();

        // we want LIFO, but the queue return FIFO
        Collections.reverse(samples);

        samples.forEach(sample -> sampleArrayList.add(new PayloadSample(Base64.getEncoder().encodeToString(sample))));
        return Response.ok().entity(new PayloadSampleList(sampleArrayList)).build();
    }

    @Override
    public @NotNull Response getSchemaForTopic(final @NotNull String topicUrlEncoded) {
        final String topic = URLDecoder.decode(topicUrlEncoded, StandardCharsets.UTF_8);
        final List<byte[]> samples = samplingService.getSamples(topic);
        if (samples.isEmpty()) {
            log.info("No samples were found for the requested topic '{}'.", topic);
            return ErrorResponseUtil.notFound("samples", topic);
        }

        final ArrayList<JsonNode> jsonSamples = new ArrayList<>();
        for (final byte[] sample : samples) {
            try {
                jsonSamples.add(objectMapper.readTree(sample));
            } catch (final IOException e) {
                log.warn("Parsing error while trying to create json samples from payload.");
                log.debug("Original exception: ", e);
                return Response.serverError().build();
            }
        }

        final JsonNode inferredSchema = INFERRER.inferForSamples(jsonSamples);

        final DataUrl dataUrl = DataUrl.createBase64JsonDataUrl(inferredSchema.asText());
        return Response.ok().entity(dataUrl).build();
    }

    @Override
    public @NotNull Response startSamplingForTopic(final @NotNull String topicUrlEncoded) {
        final String topic = URLDecoder.decode(topicUrlEncoded, StandardCharsets.UTF_8);
        samplingService.startSampling(topic);
        return Response.ok().build();
    }
}
