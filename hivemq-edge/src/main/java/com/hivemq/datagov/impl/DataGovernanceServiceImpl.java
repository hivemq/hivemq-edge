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

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.hivemq.datagov.DataGovernanceContext;
import com.hivemq.datagov.DataGovernanceService;
import com.hivemq.datagov.model.DataGovernancePolicy;
import com.hivemq.datagov.model.DataGovernanceResult;
import com.hivemq.datagov.model.impl.DataGoveranceResultImpl;
import com.hivemq.datagov.model.impl.DataGovernanceDataImpl;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.handler.publish.PublishReturnCode;
import com.hivemq.mqtt.services.InternalPublishService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
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

    private final @NotNull Counter governatedMessagesCounter;
    private final @NotNull InternalPublishService internalPublishService;
    private final @NotNull ListeningExecutorService executorService;
    private final @NotNull UnifiedNamespaceDataGovernancePolicy namespaceDataGovernancePolicy;

    @Inject
    public DataGovernanceServiceImpl(final @NotNull MetricRegistry metricRegistry,
                                     final @NotNull InternalPublishService internalPublishService,
                                     final @NotNull ExecutorService executorService,
                                     final @NotNull UnifiedNamespaceDataGovernancePolicy namespaceDataGovernancePolicy) {
        this.internalPublishService = internalPublishService;
        this.executorService = MoreExecutors.listeningDecorator(executorService);
        this.governatedMessagesCounter = metricRegistry.counter("com.hivemq.messages.governance.count");
        this.namespaceDataGovernancePolicy = namespaceDataGovernancePolicy;
    }

    @Override
    public @NotNull ListenableFuture<DataGovernanceResult> apply(@NotNull final DataGovernanceContext context) {

        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(context.getInput(), "Data Governance Input Cannot Be <null>");

        //-- Create the initial result object initd to the value of the input
        DataGovernanceResult result = new DataGoveranceResultImpl(
                new DataGovernanceDataImpl.Builder(context.getInput()).build());
        result.setStatus(DataGovernanceResult.STATUS.SUCCESS);
        context.setResult(result);

        //-- If More Than 1 Policy Is Matched, Ensure All Are Run Serially On The Same Thread
        PolicyExecution policyExecution = new PolicyExecution(context, List.of(namespaceDataGovernancePolicy));
        final ExecutorService executorForContext = getExecutorForContext(context);
        final Future<DataGovernanceResult> resultFuture = executorForContext.submit(policyExecution);
        return JdkFutureAdapters.listenInPoolThread(resultFuture, executorForContext);
    }

    @Override
    public @NotNull ListenableFuture<PublishReturnCode> applyAndPublish(@NotNull final DataGovernanceContext context) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(context.getInput(), "Data Governance Input Cannot Be <null>");
        ListenableFuture<DataGovernanceResult> policyFuture = apply(context);
        AsyncFunction<DataGovernanceResult, PublishReturnCode> async = result -> publish(context);
        return Futures.transformAsync(policyFuture, async, getExecutorForContext(context));
    }

    protected @NotNull ListenableFuture<PublishReturnCode> publish(@NotNull final DataGovernanceContext context) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(context.getResult(), "Data Governance Result Cannot Be <null>");
        Preconditions.checkArgument(context.getResult().getStatus() == DataGovernanceResult.STATUS.SUCCESS,
                "Can Only Apply Publish On Successful Execution");
        try {
            log.trace("Data Governance Publishing {} Bytes To {} at QoS {}",
                    context.getResult().getOutput().getPublish().getPayload().length,
                    context.getResult().getOutput().getPublish().getTopic(),
                    context.getResult().getOutput().getPublish().getQoS().getQosNumber());
            return internalPublishService.publish(
                    context.getResult().getOutput().getPublish(), getExecutorForContext(context),
                    context.getResult().getOutput().getClientId());
        } catch(Exception e){
            return Futures.immediateFailedFuture(e);
        }
    }

    protected ExecutorService getExecutorForContext(@NotNull final DataGovernanceContext context){
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
            governatedMessagesCounter.inc();
            DataGovernanceResult result = context.getResult();
            for(DataGovernancePolicy policy : policies){
//                log.trace("Data-Gov Executing '{}' with Id '{}'", policy.getName(), policy.getId());
                policy.execute(context, context.getInput());
            }
            return result;
        }
    }
}
