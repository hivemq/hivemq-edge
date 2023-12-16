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
package com.hivemq.bootstrap.ioc;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.HiveMQEdgeGateway;
import com.hivemq.api.ioc.ApiModule;
import com.hivemq.bootstrap.netty.ioc.NettyModule;
import com.hivemq.bridge.ioc.BridgeModule;
import com.hivemq.common.executors.ioc.ExecutorsModule;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.ioc.ConfigurationModule;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.datagov.ioc.DataGovernanceModule;
import com.hivemq.edge.HiveMQCapabilityService;
import com.hivemq.edge.modules.ModuleLoader;
import com.hivemq.edge.modules.api.adapters.ModuleServices;
import com.hivemq.edge.modules.ioc.ModulesModule;
import com.hivemq.edge.modules.ioc.RemoteServiceModule;
import com.hivemq.extensions.core.PersistencesService;
import com.hivemq.extensions.ioc.ExtensionModule;
import com.hivemq.http.JaxrsHttpServer;
import com.hivemq.logging.EventLog;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.metrics.ioc.MetricsModule;
import com.hivemq.mqtt.ioc.MQTTHandlerModule;
import com.hivemq.mqtt.ioc.MQTTServiceModule;
import com.hivemq.mqttsn.ioc.MqttsnServiceModule;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.persistence.ioc.PersistenceModule;
import com.hivemq.security.ioc.SecurityModule;
import com.hivemq.throttling.ioc.ThrottlingModule;
import com.hivemq.uns.ioc.UnsServiceModule;
import dagger.BindsInstance;
import dagger.Component;

import javax.inject.Singleton;
import java.util.Set;

@SuppressWarnings({"NullabilityAnnotations", "UnusedReturnValue"})
@Component(modules = {
        ConfigurationModule.class,
        NettyModule.class,
        MQTTHandlerModule.class,
        PersistenceModule.class,
        MetricsModule.class,
        ThrottlingModule.class,
        MQTTServiceModule.class,
        SecurityModule.class,
        MqttsnServiceModule.class,
        ExecutorsModule.class,
        ExtensionModule.class,
        BridgeModule.class,
        ApiModule.class,
        ModulesModule.class,
        UnsServiceModule.class,
        DataGovernanceModule.class,
        RemoteServiceModule.class})
@Singleton
public interface Injector {

    ConfigurationService configurationService();

    HiveMQEdgeGateway edgeGateway();

    JaxrsHttpServer apiServer();

    ShutdownHooks shutdownHooks();

    Set<Boolean> initEagerSingletons(); //workaround to do eager initializations

    Persistences persistences();

    Extensions extensions();

    ModuleServices moduleServices();

    ModuleLoader moduleLoader();

    Services services();

//    UnsServiceModule uns();

//    ExecutorsModule executors();

    @Component.Builder
    interface Builder {

        @BindsInstance
        Builder systemInformation(SystemInformation systemInformation);

        @BindsInstance
        Builder metricRegistry(MetricRegistry metricRegistry);

        @BindsInstance
        Builder configurationService(ConfigurationService configurationService);

        @BindsInstance
        Builder moduleLoader(ModuleLoader moduleLoader);

        @BindsInstance
        Builder shutdownHooks(ShutdownHooks shutdownHooks);

        @BindsInstance
        Builder persistenceService(PersistencesService persistencesService);

        @BindsInstance
        Builder persistenceStartUp(PersistenceStartup persistenceStartup);

        @BindsInstance
        Builder capabilityService(HiveMQCapabilityService capabilityService);

        Injector build();

    }

}
