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
import com.hivemq.api.resources.UnsApi;
import com.hivemq.api.utils.ApiErrorUtils;
import com.hivemq.api.utils.ApiValidation;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.uns.UnifiedNamespaceService;
import com.hivemq.uns.config.ISA95;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

/**
 * @author Simon L Johnson
 */
public class UnsResourceImpl extends AbstractApi implements UnsApi {

    private final @NotNull ConfigurationService configurationService;
    private final @NotNull UnifiedNamespaceService unifiedNamespaceService;

    @Inject
    public UnsResourceImpl(
            final @NotNull ConfigurationService configurationService,
            final @NotNull UnifiedNamespaceService unifiedNamespaceService) {
        this.configurationService = configurationService;
        this.unifiedNamespaceService = unifiedNamespaceService;
    }

    @Override
    public Response getIsa95() {
        ISA95 isa95 = unifiedNamespaceService.getISA95();
        ISA95ApiBean isa95ApiBean = ISA95ApiBean.convert(isa95);
        return Response.status(200).entity(isa95ApiBean).build();
    }

    @Override
    public Response setIsa95(final ISA95ApiBean isa95) {

        ApiErrorMessages errorMessages = ApiErrorUtils.createErrorContainer();

        //-- Ensure we apply all validation
        ApiErrorUtils.validateRequiredEntity(errorMessages, "isa95", isa95);
        if(!ApiValidation.validAlphaNumericSpacesAndDashes(isa95.getEnterprise(), true)){
            ApiErrorUtils.addValidationError(errorMessages, "enterprise", "Must be a valid alpha-numeric string with spaces");
        }
        if(!ApiValidation.validAlphaNumericSpacesAndDashes(isa95.getArea(), true)){
            ApiErrorUtils.addValidationError(errorMessages, "area", "Must be a valid alpha-numeric string with spaces");
        }
        if(!ApiValidation.validAlphaNumericSpacesAndDashes(isa95.getSite(), true)){
            ApiErrorUtils.addValidationError(errorMessages, "site", "Must be a valid alpha-numeric string with spaces");
        }
        if(!ApiValidation.validAlphaNumericSpacesAndDashes(isa95.getProductionLine(), true)){
            ApiErrorUtils.addValidationError(errorMessages, "productionLine", "Must be a valid alpha-numeric string with spaces");
        }
        if(!ApiValidation.validAlphaNumericSpacesAndDashes(isa95.getWorkCell(), true)){
            ApiErrorUtils.addValidationError(errorMessages, "workCell", "Must be a valid alpha-numeric string with spaces");
        }

        if(ApiErrorUtils.hasRequestErrors(errorMessages)){
            return ApiErrorUtils.badRequest(errorMessages);
        } else {
            unifiedNamespaceService.setISA95(ISA95ApiBean.unconvert(isa95));
            return Response.status(200).build();
        }
    }
}
