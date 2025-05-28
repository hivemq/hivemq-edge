/*
 *  Copyright 2019-present HiveMQ GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.hivemq.api.errors.datahub;

import com.hivemq.api.errors.validation.ValidationError;
import com.hivemq.api.errors.validation.ValidationErrors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class BehaviorPolicyValidationErrors extends ValidationErrors<BehaviorPolicyValidationErrors> {
    private BehaviorPolicyValidationErrors(
            final @NotNull String title,
            final @Nullable String detail,
            final @Nullable List<ValidationError<?>> errors,
            final int status,
            final @Nullable String code) {
        super(title, detail, errors, status, code);
    }

    public static BehaviorPolicyValidationErrors of(
            final @Nullable List<ValidationError<?>> errors) {
        return new BehaviorPolicyValidationErrors("Behavior policy is invalid",
                "Behavior policy is invalid",
                errors,
                400,
                null);
    }

    @Override
    public @NotNull String toString() {
        return "BahaviorPolicyValidationErrors{" +
                "errors=" +
                errors +
                ", code='" +
                code +
                '\'' +
                ", detail='" +
                detail +
                '\'' +
                ", status=" +
                status +
                ", title='" +
                title +
                '\'' +
                ", type='" +
                type +
                '\'' +
                '}';
    }
}
