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
package com.hivemq.api.v2.errors;

import com.hivemq.api.errors.ErrorFactory;
import com.hivemq.common.i18n.I18nProtocolAdapterV2Error;
import com.hivemq.edge.api.v2.model.AdapterActivationInvalidError;
import com.hivemq.edge.api.v2.model.AdapterNotConnectedError;
import com.hivemq.edge.api.v2.model.AdapterNotFoundError;
import com.hivemq.edge.api.v2.model.AdapterTypeNotFoundError;
import com.hivemq.edge.api.v2.model.BrowseFailedError;
import com.hivemq.edge.api.v2.model.BrowseFilterInvalidError;
import com.hivemq.edge.api.v2.model.BrowseInProgressError;
import com.hivemq.edge.api.v2.model.BrowseInterruptedError;
import com.hivemq.edge.api.v2.model.BrowseNotSupportedError;
import com.hivemq.edge.api.v2.model.BrowseTimeoutError;
import com.hivemq.edge.api.v2.model.TagNotFoundError;
import com.hivemq.http.HttpStatus;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Builds the Nevsky (v2) protocol-adapter REST error objects (design §11). Each method returns a generated, typed
 * {@code ApiProblemDetails} subtype with its {@code type} URI derived from the class name (so it matches the
 * OpenAPI discriminator) and its {@code title}/{@code detail} assembled from the {@link I18nProtocolAdapterV2Error}
 * catalog. The resource hands the result to {@code ErrorResponseUtil.errorResponse(...)}, which serializes it as
 * {@code application/problem+json}.
 */
public final class ProtocolAdapterV2ErrorFactory extends ErrorFactory {
    private ProtocolAdapterV2ErrorFactory() {
        super();
    }

    public static @NotNull AdapterNotFoundError adapterNotFoundError(final @NotNull String adapterId) {
        return AdapterNotFoundError.builder()
                .type(type(AdapterNotFoundError.class))
                .title(I18nProtocolAdapterV2Error.ADAPTERS_V2_ERROR_ADAPTER_NOT_FOUND_TITLE.get())
                .detail(I18nProtocolAdapterV2Error.ADAPTERS_V2_ERROR_ADAPTER_NOT_FOUND_DETAIL_WITH_ID.get(
                        Map.of("adapterId", adapterId)))
                .adapterId(adapterId)
                .status(HttpStatus.NOT_FOUND_404)
                .build();
    }

    public static @NotNull AdapterTypeNotFoundError adapterTypeNotFoundError(final @NotNull String adapterId) {
        return AdapterTypeNotFoundError.builder()
                .type(type(AdapterTypeNotFoundError.class))
                .title(I18nProtocolAdapterV2Error.ADAPTERS_V2_ERROR_ADAPTER_TYPE_NOT_FOUND_TITLE.get())
                .detail(I18nProtocolAdapterV2Error.ADAPTERS_V2_ERROR_ADAPTER_TYPE_NOT_FOUND_DETAIL_WITH_ID.get(
                        Map.of("adapterId", adapterId)))
                .adapterId(adapterId)
                .status(HttpStatus.NOT_FOUND_404)
                .build();
    }

    public static @NotNull TagNotFoundError tagNotFoundError(
            final @NotNull String adapterId, final @NotNull String tagName) {
        return TagNotFoundError.builder()
                .type(type(TagNotFoundError.class))
                .title(I18nProtocolAdapterV2Error.ADAPTERS_V2_ERROR_TAG_NOT_FOUND_TITLE.get())
                .detail(I18nProtocolAdapterV2Error.ADAPTERS_V2_ERROR_TAG_NOT_FOUND_DETAIL_WITH_ADAPTER_AND_TAG.get(
                        Map.of("adapterId", adapterId, "tagName", tagName)))
                .adapterId(adapterId)
                .tagName(tagName)
                .status(HttpStatus.NOT_FOUND_404)
                .build();
    }

    public static @NotNull BrowseNotSupportedError browseNotSupportedError(final @NotNull String adapterId) {
        return BrowseNotSupportedError.builder()
                .type(type(BrowseNotSupportedError.class))
                .title(I18nProtocolAdapterV2Error.ADAPTERS_V2_ERROR_BROWSE_NOT_SUPPORTED_TITLE.get())
                .detail(I18nProtocolAdapterV2Error.ADAPTERS_V2_ERROR_BROWSE_NOT_SUPPORTED_DETAIL_WITH_ID.get(
                        Map.of("adapterId", adapterId)))
                .adapterId(adapterId)
                .status(HttpStatus.BAD_REQUEST_400)
                .build();
    }

    public static @NotNull BrowseFilterInvalidError browseFilterInvalidError(
            final @NotNull String adapterId, final @NotNull String reason) {
        return BrowseFilterInvalidError.builder()
                .type(type(BrowseFilterInvalidError.class))
                .title(I18nProtocolAdapterV2Error.ADAPTERS_V2_ERROR_BROWSE_FILTER_INVALID_TITLE.get())
                .detail(
                        I18nProtocolAdapterV2Error.ADAPTERS_V2_ERROR_BROWSE_FILTER_INVALID_DETAIL_WITH_ID_AND_REASON
                                .get(Map.of("adapterId", adapterId, "reason", reason)))
                .adapterId(adapterId)
                .reason(reason)
                .status(HttpStatus.BAD_REQUEST_400)
                .build();
    }

    public static @NotNull AdapterNotConnectedError adapterNotConnectedError(final @NotNull String adapterId) {
        return AdapterNotConnectedError.builder()
                .type(type(AdapterNotConnectedError.class))
                .title(I18nProtocolAdapterV2Error.ADAPTERS_V2_ERROR_ADAPTER_NOT_CONNECTED_TITLE.get())
                .detail(I18nProtocolAdapterV2Error.ADAPTERS_V2_ERROR_ADAPTER_NOT_CONNECTED_DETAIL_WITH_ID.get(
                        Map.of("adapterId", adapterId)))
                .adapterId(adapterId)
                .status(HttpStatus.CONFLICT_409)
                .build();
    }

    public static @NotNull BrowseInProgressError browseInProgressError(final @NotNull String adapterId) {
        return BrowseInProgressError.builder()
                .type(type(BrowseInProgressError.class))
                .title(I18nProtocolAdapterV2Error.ADAPTERS_V2_ERROR_BROWSE_IN_PROGRESS_TITLE.get())
                .detail(I18nProtocolAdapterV2Error.ADAPTERS_V2_ERROR_BROWSE_IN_PROGRESS_DETAIL_WITH_ID.get(
                        Map.of("adapterId", adapterId)))
                .adapterId(adapterId)
                .status(HttpStatus.CONFLICT_409)
                .build();
    }

    public static @NotNull BrowseTimeoutError browseTimeoutError(final @NotNull String adapterId) {
        return BrowseTimeoutError.builder()
                .type(type(BrowseTimeoutError.class))
                .title(I18nProtocolAdapterV2Error.ADAPTERS_V2_ERROR_BROWSE_TIMEOUT_TITLE.get())
                .detail(I18nProtocolAdapterV2Error.ADAPTERS_V2_ERROR_BROWSE_TIMEOUT_DETAIL_WITH_ID.get(
                        Map.of("adapterId", adapterId)))
                .adapterId(adapterId)
                .status(HttpStatus.GATEWAY_TIMEOUT_504)
                .build();
    }

    public static @NotNull AdapterActivationInvalidError adapterActivationInvalidError() {
        return AdapterActivationInvalidError.builder()
                .type(type(AdapterActivationInvalidError.class))
                .title(I18nProtocolAdapterV2Error.ADAPTERS_V2_ERROR_ACTIVATION_INVALID_TITLE.get())
                .detail(I18nProtocolAdapterV2Error.ADAPTERS_V2_ERROR_ACTIVATION_INVALID_DETAIL.get())
                .status(HttpStatus.BAD_REQUEST_400)
                .build();
    }

    public static @NotNull BrowseInterruptedError browseInterruptedError(final @NotNull String adapterId) {
        return BrowseInterruptedError.builder()
                .type(type(BrowseInterruptedError.class))
                .title(I18nProtocolAdapterV2Error.ADAPTERS_V2_ERROR_BROWSE_INTERRUPTED_TITLE.get())
                .detail(I18nProtocolAdapterV2Error.ADAPTERS_V2_ERROR_BROWSE_INTERRUPTED_DETAIL_WITH_ID.get(
                        Map.of("adapterId", adapterId)))
                .adapterId(adapterId)
                .status(HttpStatus.SERVICE_UNAVAILABLE_503)
                .build();
    }

    public static @NotNull BrowseFailedError browseFailedError(final @NotNull String adapterId) {
        return BrowseFailedError.builder()
                .type(type(BrowseFailedError.class))
                .title(I18nProtocolAdapterV2Error.ADAPTERS_V2_ERROR_BROWSE_FAILED_TITLE.get())
                .detail(I18nProtocolAdapterV2Error.ADAPTERS_V2_ERROR_BROWSE_FAILED_DETAIL_WITH_ID.get(
                        Map.of("adapterId", adapterId)))
                .adapterId(adapterId)
                .status(HttpStatus.INTERNAL_SERVER_ERROR_500)
                .build();
    }
}
