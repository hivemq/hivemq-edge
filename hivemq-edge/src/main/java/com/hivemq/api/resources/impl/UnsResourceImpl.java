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
import com.hivemq.api.errors.ConfigWritingDisabled;
import com.hivemq.api.errors.ValidationError;
import com.hivemq.api.model.ApiErrorMessages;
import com.hivemq.api.utils.ApiErrorUtils;
import com.hivemq.api.utils.ApiValidation;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.edge.api.UnsApi;
import com.hivemq.edge.api.model.ISA95ApiBean;
import com.hivemq.uns.UnifiedNamespaceService;
import com.hivemq.uns.config.ISA95;
import com.hivemq.util.ErrorResponseUtil;
import org.jetbrains.annotations.NotNull;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

/**
 * @author Simon L Johnson
 */
public class UnsResourceImpl extends AbstractApi implements UnsApi {

    private final @NotNull UnifiedNamespaceService unifiedNamespaceService;
    private final @NotNull SystemInformation systemInformation;

    @Inject
    public UnsResourceImpl(
            final @NotNull UnifiedNamespaceService unifiedNamespaceService,
            final @NotNull SystemInformation systemInformation) {
        this.unifiedNamespaceService = unifiedNamespaceService;
        this.systemInformation = systemInformation;
    }

    @Override
    public @NotNull Response getIsa95() {
        final ISA95 isa95 = unifiedNamespaceService.getISA95();
        final ISA95ApiBean isa95ApiBean = ISA95.convert(isa95);
        return Response.ok(isa95ApiBean).build();
    }

    @Override
    public @NotNull Response setIsa95(final @NotNull ISA95ApiBean isa95) {
        if (!systemInformation.isConfigWriteable()) {
            return ErrorResponseUtil.errorResponse(new ConfigWritingDisabled());
        }

        final ApiErrorMessages errorMessages = ApiErrorUtils.createErrorContainer();

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
            return ErrorResponseUtil.errorResponse(new ValidationError(errorMessages.toErrorList()));
        } else {
            unifiedNamespaceService.setISA95(ISA95.unconvert(isa95));
            return Response.ok().build();
        }
    }
}
