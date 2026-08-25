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
package com.hivemq.bootstrap.provider;

import static com.hivemq.configuration.service.MqttConfigurationService.QueuedMessagesStrategy;

import com.google.common.annotations.VisibleForTesting;
import com.hivemq.bootstrap.factories.ClientQueueLocalPersistenceFactory;
import com.hivemq.configuration.service.PersistenceConfigurationService;
import com.hivemq.configuration.service.PersistenceMode;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.extensions.core.PersistencesService;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.persistence.clientqueue.ClientQueueLocalPersistence;
import com.hivemq.persistence.local.memory.ClientQueueMemoryLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.util.LocalPersistenceFileUtil;
import jakarta.inject.Inject;
import java.lang.reflect.Method;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientQueueLocalPersistenceProvider {

    private static final Logger log = LoggerFactory.getLogger(ClientQueueLocalPersistenceProvider.class);

    private final @NotNull PersistencesService persistencesService;
    private final @NotNull ClientQueueMemoryLocalPersistence clientQueueMemoryLocalPersistence;
    private final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil;
    private final @NotNull PublishPayloadPersistence payloadPersistence;
    private final @NotNull MessageDroppedService messageDroppedService;
    private final @NotNull PersistenceStartup persistenceStartup;
    private final @NotNull PersistenceConfigurationService persistenceConfigurationService;

    @Inject
    ClientQueueLocalPersistenceProvider(
            final @NotNull PersistencesService persistencesService,
            final @NotNull ClientQueueMemoryLocalPersistence clientQueueMemoryLocalPersistence,
            final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil,
            final @NotNull PublishPayloadPersistence payloadPersistence,
            final @NotNull MessageDroppedService messageDroppedService,
            final @NotNull PersistenceStartup persistenceStartup,
            final @NotNull PersistenceConfigurationService persistenceConfigurationService) {
        this.persistencesService = persistencesService;
        this.clientQueueMemoryLocalPersistence = clientQueueMemoryLocalPersistence;
        this.localPersistenceFileUtil = localPersistenceFileUtil;
        this.payloadPersistence = payloadPersistence;

        this.messageDroppedService = messageDroppedService;
        this.persistenceStartup = persistenceStartup;
        this.persistenceConfigurationService = persistenceConfigurationService;
    }

    public @NotNull ClientQueueLocalPersistence get() {

        final ClientQueueLocalPersistenceFactory persistenceFactory =
                persistencesService.getClientQueueLocalPersistenceFactory();

        if (persistenceConfigurationService.getMode() == PersistenceMode.IN_MEMORY) {
            return clientQueueMemoryLocalPersistence;
        }

        if (persistenceFactory == null) {
            log.error(
                    "File Persistence is specified in config.xml, but no provider for a file persistence is available. Check that the commercial module is present in the module folder and a valid license is present in the license folder.");
            throw new UnrecoverableException();
        }

        final ClientQueueLocalPersistence persistence = persistenceFactory.buildClientSessionLocalPersistence(
                localPersistenceFileUtil, payloadPersistence, messageDroppedService, persistenceStartup);
        warnIfModulePredatesQueuePolicy(persistence);
        return persistence;
    }

    /**
     * Says out loud what a module older than this core gives up, once, at start-up.
     * <p>
     * The module in {@code HIVEMQ_HOME/modules} is compiled separately from core and the two meet only
     * here, so an operator who replaces the core zip and leaves {@code modules/} alone gets a pairing
     * nobody built together. {@link ClientQueueLocalPersistence} keeps that pairing working — the
     * signature core calls carries the default, so an older module degrades instead of dying with
     * {@code AbstractMethodError} on the first queued publish. Degrading quietly is its own problem,
     * which is what this answers (EDG-882 review v02, R2-01).
     * <p>
     * A WARN rather than a refusal: what is lost is a bound that did not exist before, so the node is
     * exactly as correct as the previous release. Refusing to boot over it would turn a stale module
     * into an outage, which is the failure this whole change exists to remove.
     */
    private static void warnIfModulePredatesQueuePolicy(final @NotNull ClientQueueLocalPersistence persistence) {
        if (implementsQueuePolicyAdd(persistence.getClass())) {
            return;
        }
        log.warn(
                "The file persistence module '{}' was built before the queue-policy contract (EDG-882) and cannot"
                        + " bound QoS 0 messages by queue limit. The node runs normally, but payload sampling is"
                        + " unbounded on this node: one sampled QoS 0 topic can grow until it exhausts the"
                        + " node-wide QoS 0 memory budget and QoS 0 publishes start being dropped for every"
                        + " client (EDG-885). Replace the module in the modules folder with the one shipped"
                        + " alongside this HiveMQ Edge version.",
                persistence.getClass().getName());
    }

    /**
     * @return whether the loaded implementation supplies its own {@code applyMaxToQos0} overload. Read
     *     off the resolved method's declaring class rather than by calling it: a class that does not
     *     override it resolves to the interface's default, and a default is not distinguishable from an
     *     override by any cheaper means.
     */
    @VisibleForTesting
    static boolean implementsQueuePolicyAdd(
            final @NotNull Class<? extends ClientQueueLocalPersistence> implementation) {
        try {
            final Method add = implementation.getMethod(
                    "add",
                    String.class,
                    boolean.class,
                    PUBLISH.class,
                    long.class,
                    QueuedMessagesStrategy.class,
                    boolean.class,
                    boolean.class,
                    int.class);
            return !add.getDeclaringClass().isInterface();
        } catch (final NoSuchMethodException | SecurityException e) {
            // Neither can happen for a class that satisfies the interface at all, and the answer to a
            // probe that cannot run is not to refuse the node: say nothing and let it start.
            log.debug("Could not determine the queue-policy contract of '{}'", implementation.getName(), e);
            return true;
        }
    }
}
