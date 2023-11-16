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
package com.hivemq.extensions.ioc;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extension.sdk.api.events.EventRegistry;
import com.hivemq.extension.sdk.api.services.admin.AdminService;
import com.hivemq.extension.sdk.api.services.auth.SecurityRegistry;
import com.hivemq.extension.sdk.api.services.builder.PublishBuilder;
import com.hivemq.extension.sdk.api.services.builder.RetainedPublishBuilder;
import com.hivemq.extension.sdk.api.services.builder.TopicPermissionBuilder;
import com.hivemq.extension.sdk.api.services.builder.TopicSubscriptionBuilder;
import com.hivemq.extension.sdk.api.services.builder.WillPublishBuilder;
import com.hivemq.extension.sdk.api.services.cluster.ClusterService;
import com.hivemq.extension.sdk.api.services.interceptor.EdgeInterceptorRegistry;
import com.hivemq.extension.sdk.api.services.interceptor.GlobalInterceptorRegistry;
import com.hivemq.extension.sdk.api.services.intializer.InitializerRegistry;
import com.hivemq.extension.sdk.api.services.publish.PublishService;
import com.hivemq.extension.sdk.api.services.publish.RetainedMessageStore;
import com.hivemq.extension.sdk.api.services.session.ClientService;
import com.hivemq.extension.sdk.api.services.subscription.SubscriptionStore;
import com.hivemq.extensions.ExtensionBootstrap;
import com.hivemq.extensions.ExtensionBootstrapImpl;
import com.hivemq.extensions.client.parameter.ServerInformationImpl;
import com.hivemq.extensions.events.EventRegistryImpl;
import com.hivemq.extensions.events.LifecycleEventListeners;
import com.hivemq.extensions.events.LifecycleEventListenersImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginOutputAsyncerImpl;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.PluginTaskExecutorServiceImpl;
import com.hivemq.extensions.handler.PluginAuthenticatorService;
import com.hivemq.extensions.handler.PluginAuthenticatorServiceImpl;
import com.hivemq.extensions.handler.PluginAuthorizerService;
import com.hivemq.extensions.handler.PluginAuthorizerServiceImpl;
import com.hivemq.extensions.ioc.annotation.PluginStartStop;
import com.hivemq.extensions.ioc.annotation.PluginTaskQueue;
import com.hivemq.extensions.loader.ExtensionBuilderDependencies;
import com.hivemq.extensions.loader.ExtensionBuilderDependenciesImpl;
import com.hivemq.extensions.loader.ExtensionLifecycleHandler;
import com.hivemq.extensions.loader.ExtensionLifecycleHandlerImpl;
import com.hivemq.extensions.loader.ExtensionLoader;
import com.hivemq.extensions.loader.ExtensionLoaderImpl;
import com.hivemq.extensions.loader.ExtensionServicesDependencies;
import com.hivemq.extensions.loader.ExtensionServicesDependenciesImpl;
import com.hivemq.extensions.loader.ExtensionStaticInitializer;
import com.hivemq.extensions.loader.ExtensionStaticInitializerImpl;
import com.hivemq.extensions.loader.HiveMQExtensionFactory;
import com.hivemq.extensions.loader.HiveMQExtensionFactoryImpl;
import com.hivemq.extensions.services.admin.AdminServiceImpl;
import com.hivemq.extensions.services.auth.Authenticators;
import com.hivemq.extensions.services.auth.AuthenticatorsImpl;
import com.hivemq.extensions.services.auth.Authorizers;
import com.hivemq.extensions.services.auth.AuthorizersImpl;
import com.hivemq.extensions.services.auth.SecurityRegistryImpl;
import com.hivemq.extensions.services.builder.PublishBuilderImpl;
import com.hivemq.extensions.services.builder.RetainedPublishBuilderImpl;
import com.hivemq.extensions.services.builder.TopicPermissionBuilderImpl;
import com.hivemq.extensions.services.builder.TopicSubscriptionBuilderImpl;
import com.hivemq.extensions.services.builder.WillPublishBuilderImpl;
import com.hivemq.extensions.services.cluster.ClusterServiceNoopImpl;
import com.hivemq.extensions.services.initializer.InitializerRegistryImpl;
import com.hivemq.extensions.services.initializer.Initializers;
import com.hivemq.extensions.services.initializer.InitializersImpl;
import com.hivemq.extensions.services.interceptor.EdgeInterceptorRegistryImpl;
import com.hivemq.extensions.services.interceptor.GlobalInterceptorRegistryImpl;
import com.hivemq.extensions.services.interceptor.Interceptors;
import com.hivemq.extensions.services.interceptor.InterceptorsImpl;
import com.hivemq.extensions.services.publish.PublishServiceImpl;
import com.hivemq.extensions.services.publish.RetainedMessageStoreImpl;
import com.hivemq.extensions.services.session.ClientServiceImpl;
import com.hivemq.extensions.services.subscription.SubscriptionStoreImpl;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

@Module
public abstract class ExtensionModule {

    @Binds
    abstract @NotNull ExtensionBootstrap extensionBootstrap(@NotNull ExtensionBootstrapImpl extensionBootstrap);

    @Binds
    abstract @NotNull ExtensionStaticInitializer extensionStaticInitializer(@NotNull ExtensionStaticInitializerImpl extensionStaticInitializer);

    @Binds
    abstract @NotNull HiveMQExtensionFactory extensionFactory(@NotNull HiveMQExtensionFactoryImpl extensionFactory);

    @Binds
    abstract @NotNull ExtensionLoader extensionLoader(@NotNull ExtensionLoaderImpl extensionLoader);

    @Binds
    abstract @NotNull ExtensionServicesDependencies extensionServicesDependencies(@NotNull ExtensionServicesDependenciesImpl extensionServicesDependencies);

    @Binds
    abstract @NotNull ExtensionLifecycleHandler extensionLifecycleHandler(@NotNull ExtensionLifecycleHandlerImpl extensionLifecycleHandler);

    @Binds
    abstract @NotNull Authenticators authenticators(@NotNull AuthenticatorsImpl authenticators);

    @Binds
    abstract @NotNull Authorizers authorizers(@NotNull AuthorizersImpl authorizers);

    @Binds
    abstract @NotNull SecurityRegistry securityRegistry(@NotNull SecurityRegistryImpl securityRegistry);

    @Provides
    @PluginStartStop
    @Singleton
    static @NotNull ExecutorService pluginStartStopExecutorService(ExtensionStartStopExecutorProvider extensionStartStopExecutorProvider) {
        return extensionStartStopExecutorProvider.get();
    }

    @Binds
    abstract @NotNull PluginTaskExecutorService pluginTaskExecutorService(@NotNull PluginTaskExecutorServiceImpl pluginTaskExecutorService);

    @Binds
    abstract @NotNull PluginOutPutAsyncer pluginOutPutAsyncer(@NotNull PluginOutputAsyncerImpl pluginOutputAsyncer);

    @Binds
    abstract @NotNull InitializerRegistry initializerRegistry(@NotNull InitializerRegistryImpl initializerRegistry);

    @Binds
    abstract @NotNull Initializers initializers(@NotNull InitializersImpl initializers);

    @Binds
    abstract @NotNull ServerInformation serverInformation(@NotNull ServerInformationImpl serverInformation);

    @Provides
    @PluginTaskQueue
    @Singleton
    static @NotNull AtomicLong pluginTaskQueueAtomicLong() {
        return new AtomicLong(0);
    }

    @Binds
    abstract @NotNull RetainedMessageStore retainedMessageStore(@NotNull RetainedMessageStoreImpl retainedMessageStore);

    @Binds
    abstract @NotNull ClientService clientService(@NotNull ClientServiceImpl clientService);

    @Binds
    abstract @NotNull RetainedPublishBuilder retainedPublishBuilder(@NotNull RetainedPublishBuilderImpl retainedPublishBuilder);

    @Binds
    abstract @NotNull SubscriptionStore subscriptionStore(@NotNull SubscriptionStoreImpl subscriptionStore);

    @Binds
    abstract @NotNull TopicSubscriptionBuilder topicSubscriptionBuilder(@NotNull TopicSubscriptionBuilderImpl topicSubscriptionBuilder);

    @Binds
    abstract @NotNull TopicPermissionBuilder topicPermissionBuilder(@NotNull TopicPermissionBuilderImpl topicPermissionBuilder);

    @Binds
    abstract @NotNull ExtensionBuilderDependencies extensionBuilderDependencies(@NotNull ExtensionBuilderDependenciesImpl extensionBuilderDependencies);

    @Binds
    public abstract @NotNull PublishService publishService(@NotNull PublishServiceImpl publishService);

    @Binds
    abstract @NotNull PublishBuilder publishBuilder(@NotNull PublishBuilderImpl publishBuilder);

    @Binds
    abstract @NotNull WillPublishBuilder willPublishBuilder(@NotNull WillPublishBuilderImpl willPublishBuilder);

    @Binds
    abstract @NotNull EventRegistry eventRegistry(@NotNull EventRegistryImpl eventRegistry);

    @Binds
    abstract @NotNull LifecycleEventListeners lifecycleEventListeners(@NotNull LifecycleEventListenersImpl lifecycleEventListeners);

    @Binds
    abstract @NotNull ClusterService clusterService(@NotNull ClusterServiceNoopImpl clusterServiceNoop);

    @Binds
    abstract @NotNull PluginAuthorizerService pluginAuthorizerService(@NotNull PluginAuthorizerServiceImpl pluginAuthorizerService);

    @Binds
    abstract @NotNull PluginAuthenticatorService pluginAuthenticatorService(@NotNull PluginAuthenticatorServiceImpl pluginAuthenticatorService);

    @Binds
    abstract @NotNull GlobalInterceptorRegistry globalInterceptorRegistry(@NotNull GlobalInterceptorRegistryImpl globalInterceptorRegistry);

    @Binds
    abstract @NotNull Interceptors interceptors(@NotNull InterceptorsImpl interceptors);

    @Binds
    abstract @NotNull AdminService adminService(@NotNull AdminServiceImpl adminService);

    @Binds
    abstract @NotNull EdgeInterceptorRegistry edgeInterceptorRegistry(@NotNull EdgeInterceptorRegistryImpl edgeInterceptorRegistry);


}
