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

import com.hivemq.api.model.samples.PayloadSample;
import com.hivemq.api.model.samples.PayloadSampleList;
import com.hivemq.api.resources.SamplingApi;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.sampling.SamplingService;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

@Singleton
public class SamplingResourceImpl implements SamplingApi {

    private final @NotNull SamplingService samplingService;

    @Inject
    public SamplingResourceImpl(final @NotNull SamplingService samplingService) {
        this.samplingService = samplingService;
    }

    @Override
    public @NotNull Response getSamplesForTopic(@NotNull final String topicBase64) {

        final String topic = new String(Base64.getDecoder().decode(topicBase64), StandardCharsets.UTF_8);

        final List<byte[]> samples = samplingService.getSamples(topic);
        final ArrayList<PayloadSample> sampleArrayList = new ArrayList<>();

        // we want LIFO, but the queue return FIFO
        Collections.reverse(samples);

        samples.forEach(sample -> sampleArrayList.add(new PayloadSample(Base64.getEncoder().encodeToString(sample))));
        return Response.ok().entity(new PayloadSampleList(sampleArrayList)).build();
    }

    @Override
    public @NotNull Response startSamplingForTopic(@NotNull final String topicBase64) {
        final String topic = new String(Base64.getDecoder().decode(topicBase64), StandardCharsets.UTF_8);
        samplingService.startSampling(topic);
        return Response.ok().build();
    }
}
