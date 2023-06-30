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
package com.hivemq.datagov.ioc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.hivemq.datagov.DataGovernanceService;
import com.hivemq.datagov.impl.DataGovernanceServiceImpl;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

@Module
public abstract class DataGovernanceModule {

    @Binds
    abstract @NotNull DataGovernanceService dataGovernanceService(@NotNull DataGovernanceServiceImpl dataGovernanceService);

//    @Binds
//    abstract @NotNull DataGovernancePolicyProvider dataGovernancePolicyProvider(@NotNull DataGovernancePolicyProviderImpl dataGovernanceService);
//
//    @Binds
//    abstract @NotNull DataGovernanceFunctionProvider dataGovernanceFunctionProvider(@NotNull DataGovernanceFunctionProviderImpl dataGovernanceJsonSchemaProviderImp);
//
//    @Binds
//    abstract @NotNull DataGovernanceJsonSchemaProvider dataGovernanceJsonSchemaProvider(@NotNull DataGovernanceJsonSchemaProviderImpl dataGovernanceJsonSchemaProviderImpl);
//
//    @Binds
//    abstract @NotNull DataGovernanceTokenProvider dataGovernanceTokenProvider(@NotNull DataGovernanceTokenProviderImpl dataGovernanceTokenProviderImpl);
//
//    @Binds
//    abstract @NotNull DataGovernanceProviders dataGovernanceProviders(@NotNull DataGovernanceProvidersImpl dataGovernanceProvidersImpl);

    @Provides
    @Singleton
    static ObjectMapper mapper(){
        ObjectMapper mapper = new ObjectMapper();
        configureMapper(mapper);
        return mapper;
    }

    public static void configureMapper(final @NotNull ObjectMapper mapper){
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
//        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
}
