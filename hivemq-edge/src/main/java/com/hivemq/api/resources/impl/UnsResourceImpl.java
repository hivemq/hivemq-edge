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

import com.hivemq.api.AbstractApi;
import com.hivemq.api.model.ApiErrorMessages;
import com.hivemq.api.model.uns.ISA95ApiBean;
import com.hivemq.api.model.uns.NamespaceProfileBean;
import com.hivemq.api.model.uns.NamespaceProfilesList;
import com.hivemq.api.resources.UnsApi;
import com.hivemq.api.utils.ApiErrorUtils;
import com.hivemq.api.utils.ApiValidation;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.uns.NamespaceUtils;
import com.hivemq.uns.UnifiedNamespaceService;
import com.hivemq.uns.config.ISA95;
import com.hivemq.uns.config.NamespaceProfile;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Simon L Johnson
 */
public class UnsResourceImpl extends AbstractApi implements UnsApi {

    private final @NotNull UnifiedNamespaceService unifiedNamespaceService;

    @Inject
    public UnsResourceImpl(
            final @NotNull ConfigurationService configurationService,
            final @NotNull UnifiedNamespaceService unifiedNamespaceService) {
        this.unifiedNamespaceService = unifiedNamespaceService;
    }

    @Override
    public Response getProfiles() {
        List<NamespaceProfile> profiles = unifiedNamespaceService.getConfiguredProfiles(true);
        NamespaceProfilesList list = new NamespaceProfilesList(profiles.stream().
                map(NamespaceProfileBean::convert).collect(Collectors.toList()));
        return Response.status(200).entity(list).build();
    }

    @Override
    public Response setActiveProfile(final NamespaceProfileBean bean) {
        ApiErrorMessages errorMessages = ApiErrorUtils.createErrorContainer();
        ApiErrorUtils.validateRequiredEntity(errorMessages, "bean", bean);
        if(!ApiValidation.validAlphaNumericSpacesAndDashes(bean.getName(), false)){
            ApiErrorUtils.addValidationError(errorMessages, "name", "Name must be a non-null valid alpha-numeric string with spaces");
        }

        if(bean.getSegments() == null || bean.getSegments().isEmpty()){
            ApiErrorUtils.addValidationError(errorMessages, "segments", "UNS Profile must contain at least 1 segment");
        }

        if(ApiErrorUtils.hasRequestErrors(errorMessages)){
            return ApiErrorUtils.badRequest(errorMessages);
        } else {
            NamespaceProfile profile = NamespaceProfileBean.unconvert(bean, true);
            List<NamespaceProfile> all = unifiedNamespaceService.getConfiguredProfiles(true);
            NamespaceUtils.replaceNamespaceProfile(all, profile, true);
            unifiedNamespaceService.setConfiguredProfiles(all);
            return Response.status(200).build();
        }
    }
}
