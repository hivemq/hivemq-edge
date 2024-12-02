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
package com.hivemq.extensions.executor;

import org.jetbrains.annotations.NotNull;
import com.hivemq.extensions.executor.task.*;

import java.util.function.Supplier;

/**
 * The main interface for the handling of extension tasks (interceptors, i.e.).
 * <p>
 * There are three kind of available tasks:
 * <ul>
 * <li> {@link PluginInTask} needing only a {@link PluginTaskInput} object and containing no callback in the {@link
 * PluginInTaskContext}
 * <li> {@link PluginOutTask} needing only a {@link PluginTaskOutput} object and containing a callback in the {@link
 * PluginOutTaskContext}
 * <li> {@link PluginInOutTask} needing a {@link PluginTaskInput} and a {@link PluginTaskOutput} object and containing
 * a
 * callback in the {@link PluginInOutTaskContext}
 * </ul>
 * <p>
 * The {@link PluginTaskExecutorService} will execute the {@link PluginTask} in the ThreadPool of the extension system.
 * <p>
 * A HiveMQ core developer, who wishes to implement a new interceptor, needs only to think about the creation of the
 * needed objects, not about the threading.
 *
 * @author Georg Held
 * @author Christoph Schäbel
 */
public interface PluginTaskExecutorService {

    /**
     * Handle a {@link PluginTask}, that can not affect the execution of HiveMQ, but provides additional information to
     * the extension developer.
     *
     * @param pluginInTaskContext a {@link PluginTaskContext} containing only the needed information for the scheduler.
     * @param pluginInputSupplier a supplier for the the {@link PluginTaskInput} object.
     * @param pluginTask          a wrapper around the specific interceptor i.e..
     * @param <I>                 a type extending the {@link PluginTaskInput} marker interface.
     * @throws java.util.concurrent.RejectedExecutionException when task executor is shut down.
     */
    <I extends PluginTaskInput> void handlePluginInTaskExecution(final @NotNull PluginInTaskContext pluginInTaskContext,
                                                                    final @NotNull Supplier<I> pluginInputSupplier,
                                                                    final @NotNull PluginInTask<I> pluginTask);

    /**
     * Handle a {@link PluginTask}, that can affect the execution of HiveMQ, but provides no additional information to
     * the extension developer.
     *
     * @param pluginOutTaskContext a {@link PluginTaskContext} containing the needed information for the scheduler and a
     *                             callback for the further processing of the extension call result.
     * @param pluginOutputSupplier a supplier for the the {@link PluginTaskOutput} object.
     * @param pluginTask           a wrapper around the specific interceptor i.e..
     * @param <O>                  a type extending the {@link PluginTaskOutput} marker interface.
     * @throws java.util.concurrent.RejectedExecutionException when task executor is shut down.
     */
    <O extends PluginTaskOutput> void handlePluginOutTaskExecution(final @NotNull PluginOutTaskContext<O> pluginOutTaskContext,
                                                                      final @NotNull Supplier<O> pluginOutputSupplier,
                                                                      final @NotNull PluginOutTask<O> pluginTask);

    /**
     * Handle a {@link PluginTask}, that can affect the execution of HiveMQ and provides additional information to
     * the extension developer.
     *
     * @param pluginInOutContext   a {@link PluginTaskContext} containing the needed information for the scheduler and a
     *                             callback for the further processing of the extension call result.
     * @param pluginInputSupplier  a supplier for the the {@link PluginTaskInput} object.
     * @param pluginOutputSupplier a supplier for the the {@link PluginTaskOutput} object.
     * @param pluginTask           a wrapper around the specific interceptor i.e..
     * @param <I>                  a type extending the {@link PluginTaskInput} marker interface.
     * @param <O>                  a type extending the {@link PluginTaskOutput} marker interface.
     * @throws java.util.concurrent.RejectedExecutionException when task executor is shut down.
     */
    <I extends PluginTaskInput, O extends PluginTaskOutput> void handlePluginInOutTaskExecution(final @NotNull PluginInOutTaskContext<O> pluginInOutContext,
                                                                                                   final @NotNull Supplier<I> pluginInputSupplier,
                                                                                                   final @NotNull Supplier<O> pluginOutputSupplier,
                                                                                                   final @NotNull PluginInOutTask<I, O> pluginTask);



    /* Usage example:

    public class PublishAuthorizerHandler {

        @NotNull
        private final PluginOutPutAsyncer asyncer;
        @NotNull
        private final PluginTaskExecutorService service;
        @NotNull
        private final ClientSessionPersistence clientSessionPersistence;

        @Inject
        public PublishAuthorizerHandler(final @NotNull PluginOutPutAsyncer asyncer,
                          final @NotNull PluginTaskExecutorService service,
                          final @NotNull ClientSessionPersistence clientSessionPersistence) {
            this.asyncer = asyncer;
            this.service = service;
            this.clientSessionPersistence = clientSessionPersistence;
        }

        void handle(final @NotNull String id, final @NotNull PublishAuthorizer authorizer) {
            final PublishAuthorizerContext pluginInOutContext = new PublishAuthorizerContext(authorizer.getClass(), id, clientSessionPersistence);
            service.handlePluginInOutTaskExecution(pluginInOutContext,
                    () -> new PublishAuthorizerInputImpl(),
                    () -> new PublishAuthorizerOutputImpl(asyncer, pluginInOutContext),
                    new PublishAuthorizerTask(authorizer));
        }
    }

   public class PublishAuthorizerContext extends PluginInOutTaskContext<PublishAuthorizerOutputImpl> {
        @NotNull
        private final String clientId;
        @NotNull
        private final ClientSessionPersistence clientSessionPersistence;

        protected PublishAuthorizerContext(final @NotNull Class<?> taskClazz,
                                           final @NotNull String clientId,
                                           final @NotNull ClientSessionPersistence clientSessionPersistence) {
            super(taskClazz, clientId);
            this.clientId = clientId;
            this.clientSessionPersistence = clientSessionPersistence;
        }

        @Override
        public void pluginPost(final @NotNull PublishAuthorizerOutputImpl pluginOutput) {
            // handle extension result
            if (pluginOutput.isTimedOut() {
                if (pluginOutput.getTimeoutFallback() == TimeoutFallback.FAILURE) {
                    clientSessionPersistence.forceDisconnectClient(clientId, true);
                } else {
                    //continue
                }
            }

            if (pluginOutput.isDisconnect()) {
                clientSessionPersistence.forceDisconnectClient(clientId, true);
            }
        }
    }

    public class PublishAuthorizerOutputImpl extends AbstractAsyncOutput<PublishAuthorizeOutput> implements PublishAuthorizeOutput {

        @Nullable
        private final PluginOutPutAsyncer asyncer;

        @Nullable
        private final PublishAuthorizerContext pluginInOutContext;

        public PublishAuthorizerOutputImpl(@Nullable final PluginOutPutAsyncer asyncer, @Nullable final PublishAuthorizerContext pluginInOutContext) {

            this.asyncer = asyncer;
            this.pluginInOutContext = pluginInOutContext;
        }

        // ... implementation of PublishAuthorizeOutput methods

    }

    public class PublishAuthorizerTask implements PluginInOutTask<PublishAuthorizerInputImpl, PublishAuthorizerOutputImpl> {
        @NotNull
        private final PublishAuthorizer authorizer;

        public PublishAuthorizerTask(final @NotNull PublishAuthorizer authorizer) {
            this.authorizer = authorizer;
        }

        @NotNull
        @Override
        public PublishAuthorizerOutputImpl apply(final @NotNull PublishAuthorizerInputImpl publishAuthorizerInput, final @NotNull PublishAuthorizerOutputImpl publishAuthorizerOutput) {
            try {
                authorizer.authorizePublish(publishAuthorizerInput, publishAuthorizerOutput);
                return publishAuthorizerOutput;
            } catch (final Throwable t) {
                final PublishAuthorizerOutputImpl exceptionalOutput = new PublishAuthorizerOutputImpl(null, null);
                exceptionalOutput.disconnectClient();
                return exceptionalOutput;
            }
        }
    }
    */
}
