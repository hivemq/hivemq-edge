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
package com.hivemq.datagov.impl;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.hivemq.api.mqtt.PublishReturnCode;
import com.hivemq.datagov.DataGovernanceContext;
import com.hivemq.datagov.DataGovernanceService;
import com.hivemq.datagov.model.DataGovernancePolicy;
import com.hivemq.datagov.model.DataGovernanceResult;
import com.hivemq.datagov.model.impl.DataGoveranceResultImpl;
import com.hivemq.datagov.model.impl.DataGovernanceDataImpl;
import com.hivemq.mqtt.handler.publish.PublishingResult;
import com.hivemq.mqtt.services.PrePublishProcessorService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 *
 */
@Singleton
public class DataGovernanceServiceImpl implements DataGovernanceService {

    private static final Logger log = LoggerFactory.getLogger(DataGovernanceServiceImpl.class);

    private final @NotNull PrePublishProcessorService prePublishProcessorService;
    private final @NotNull ListeningExecutorService executorService;
    private final @NotNull UnifiedNamespaceDataGovernancePolicy namespaceDataGovernancePolicy;

    @Inject
    public DataGovernanceServiceImpl(
            final @NotNull PrePublishProcessorService prePublishProcessorService,
            final @NotNull ExecutorService executorService,
            final @NotNull UnifiedNamespaceDataGovernancePolicy namespaceDataGovernancePolicy) {
        this.prePublishProcessorService = prePublishProcessorService;
        this.executorService = MoreExecutors.listeningDecorator(executorService);
        this.namespaceDataGovernancePolicy = namespaceDataGovernancePolicy;
    }

    @Override
    public @NotNull ListenableFuture<DataGovernanceResult> apply(final @NotNull DataGovernanceContext context) {

        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(context.getInput(), "Data Governance Input Cannot Be <null>");

        //-- Create the initial result object initd to the value of the input
        final DataGovernanceResult result = new DataGoveranceResultImpl(
                new DataGovernanceDataImpl.Builder(context.getInput()).build());
        result.setStatus(DataGovernanceResult.STATUS.SUCCESS);
        context.setResult(result);

        //-- If More Than 1 Policy Is Matched, Ensure All Are Run Serially On The Same Thread
        final PolicyExecution policyExecution = new PolicyExecution(context, List.of(namespaceDataGovernancePolicy));
        final ExecutorService executorForContext = getExecutorForContext(context);
        final Future<DataGovernanceResult> resultFuture = executorForContext.submit(policyExecution);
        return JdkFutureAdapters.listenInPoolThread(resultFuture, executorForContext);
    }

    @Override
    public @NotNull ListenableFuture<PublishingResult> applyAndPublish(final @NotNull DataGovernanceContext context) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(context.getInput(), "Data Governance Input Cannot Be <null>");
        final ListenableFuture<DataGovernanceResult> policyFuture = apply(context);
        final AsyncFunction<DataGovernanceResult, PublishingResult> async = result -> publish(context);
        return Futures.transformAsync(policyFuture, async, getExecutorForContext(context));
    }

    protected @NotNull ListenableFuture<PublishingResult> publish(final @NotNull DataGovernanceContext context) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(context.getResult(), "Data Governance Result Cannot Be <null>");
        Preconditions.checkArgument(context.getResult().getStatus() == DataGovernanceResult.STATUS.SUCCESS,
                "Can Only Apply Publish On Successful Execution");
        try {
            log.trace("Data Governance Publishing {} Bytes To {} at QoS {}",
                    context.getResult().getOutput().getPublish().getPayload().length,
                    context.getResult().getOutput().getPublish().getTopic(),
                    context.getResult().getOutput().getPublish().getQoS().getQosNumber());
            return prePublishProcessorService.publish(
                    context.getResult().getOutput().getPublish(), getExecutorForContext(context),
                    context.getResult().getOutput().getClientId());
        } catch(final Exception e){
            return Futures.immediateFailedFuture(e);
        }
    }

    protected ExecutorService getExecutorForContext(final @NotNull DataGovernanceContext context){
        return context.getExecutorService() == null ? executorService :
                context.getExecutorService();
    }

    class PolicyExecution implements Callable<DataGovernanceResult> {

        private final @NotNull List<DataGovernancePolicy> policies;
        private final @NotNull DataGovernanceContext context;

        public PolicyExecution(final @NotNull DataGovernanceContext context, final @NotNull List<DataGovernancePolicy> policies) {
            Preconditions.checkNotNull(context);
            Preconditions.checkNotNull(context.getResult(), "Result should be available on context");
            this.policies = policies;
            this.context = context;
        }

        @Override
        public DataGovernanceResult call() {
            final DataGovernanceResult result = context.getResult();
            for(final DataGovernancePolicy policy : policies){
//                log.trace("Data-Gov Executing '{}' with Id '{}'", policy.getName(), policy.getId());
                policy.execute(context, context.getInput());
            }
            return result;
        }
    }
}
