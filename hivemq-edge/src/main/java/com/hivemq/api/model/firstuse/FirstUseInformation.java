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
package com.hivemq.api.model.firstuse;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author Simon L Johnson
 */
public class FirstUseInformation {

    @JsonProperty("firstUse")
    @Schema(description = "A mandatory Boolean indicating if the gateway is in firstUse mode", required = true)
    private final @NotNull Boolean firstUse;

    @JsonProperty("prefillUsername")
    @Schema(description = "A String indicating if the prefill data for the username/password page.", nullable = true)
    private final @NotNull String prefillUsername;

    @JsonProperty("prefillPassword")
    @Schema(description = "A String indicating if the prefill data for the username/password page.", nullable = true)
    private final @NotNull String prefillPassword;

    @JsonProperty("firstUseTitle")
    @Schema(description = "A header string to use when firstUse = true.", nullable = true)
    private final @NotNull String firstUseTitle;

    @JsonProperty("firstUseDescription")
    @Schema(description = "A description string to use when firstUse = true.", nullable = true)
    private final @NotNull String firstUseDescription;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public FirstUseInformation(
            @NotNull @JsonProperty("firstUse") final Boolean firstUse,
            @Nullable @JsonProperty("prefillUsername") final String prefillUsername,
            @Nullable @JsonProperty("prefillPassword") final String prefillPassword,
            @Nullable @JsonProperty("firstUseTitle") final String firstUseTitle,
            @Nullable @JsonProperty("firstUseDescription") final String firstUseDescription) {
        this.firstUse = firstUse;
        this.prefillUsername = prefillUsername;
        this.prefillPassword = prefillPassword;
        this.firstUseTitle = firstUseTitle;
        this.firstUseDescription = firstUseDescription;
    }

    public Boolean getFirstUse() {
        return firstUse;
    }

    public String getPrefillUsername() {
        return prefillUsername;
    }

    public String getPrefillPassword() {
        return prefillPassword;
    }

    public String getFirstUseTitle() {
        return firstUseTitle;
    }

    public String getFirstUseDescription() {
        return firstUseDescription;
    }
}
