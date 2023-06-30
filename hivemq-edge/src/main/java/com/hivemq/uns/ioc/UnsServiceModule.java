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
package com.hivemq.uns.ioc;

import com.hivemq.datagov.impl.UnifiedNamespaceDataGovernancePolicy;
import com.hivemq.datagov.model.DataGovernancePolicy;
import com.hivemq.uns.UnifiedNamespaceService;
import com.hivemq.uns.impl.UnifiedNamespaceServiceImpl;
import dagger.Binds;
import dagger.Module;

import javax.inject.Singleton;

/**
 * Services related to the functioning of the UNS aspects of the system
 *
 * @author Simon L Johnson
 */
@Module
public abstract class UnsServiceModule {

    @Binds
    @Singleton
    abstract UnifiedNamespaceService unifiedNamespaceService(UnifiedNamespaceServiceImpl unifiedNamespaceService);

    @Binds
//    @IntoSet
    abstract DataGovernancePolicy provideDataGovernancePolicies(UnifiedNamespaceDataGovernancePolicy policy);

}
