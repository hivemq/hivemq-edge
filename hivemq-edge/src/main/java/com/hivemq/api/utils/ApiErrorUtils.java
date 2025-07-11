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
package com.hivemq.api.utils;

import com.hivemq.api.model.ApiErrorMessage;
import com.hivemq.api.model.ApiErrorMessages;
import com.hivemq.http.error.Error;
import com.hivemq.util.ErrorResponseUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Simon L Johnson
 */
public class ApiErrorUtils {

    public static  ApiErrorMessages createErrorContainer(){
        return new ApiErrorMessages();
    }

    public static void validateRequiredField(final ApiErrorMessages apiErrorMessages, final String fieldName, final String fieldValue, final boolean allowEmpty) {
        if(Objects.isNull(fieldValue)){
            apiErrorMessages.addError(new ApiErrorMessage(fieldName, "Invalid user supplied data", "Supplied field was null"));
        }
        else if(!allowEmpty && "".equals(fieldValue.trim())){
            apiErrorMessages.addError(new ApiErrorMessage(fieldName, "Invalid user supplied data", "Supplied field was empty"));
        }
    }

    public static void validateRequiredEntity(final ApiErrorMessages apiErrorMessages, final String entityName, final Object entity) {
        if(Objects.isNull(entity)){
            apiErrorMessages.addError(new ApiErrorMessage(entityName, "Invalid user supplied data", null));
        }
    }

    public static void validateRequiredFieldRegex(final ApiErrorMessages apiErrorMessages, final String fieldName, final String value, final String regex) {
        if(value == null || value.isEmpty()){
            apiErrorMessages.addError(new ApiErrorMessage(fieldName, "Required field was null or empty", null));
        }
        if(!value.matches(regex)){
            apiErrorMessages.addError(new ApiErrorMessage(fieldName, "Required field did not conform to regex", null));
        }
    }

    public  static void validateFieldLengthBetweenIncl(final ApiErrorMessages apiErrorMessages, final String fieldName, final String value, final int min, final int max) {
        if(value != null){
            final int len = value.length();
            if(len < min || len > max){
                apiErrorMessages.addError(new ApiErrorMessage(fieldName, String.format("Length of field value [%s] was outside the allowed bounds [%s, %s]", len, min, max), null));
            }
        }
    }

    public  static void validateFieldValueBetweenIncl(final ApiErrorMessages apiErrorMessages, final String fieldName, final int value, final int min, final int max) {
        if(value < min || value > max){
            apiErrorMessages.addError(new ApiErrorMessage(fieldName, String.format("Size of field value [%s] was outside the allowed bounds [%s, %s]", value, min, max), null));
        }
    }

    public static void addValidationError(final ApiErrorMessages apiErrorMessages, final String fieldName, final String details){
        apiErrorMessages.addError(new ApiErrorMessage(fieldName, "Invalid user supplied data", details));
    }

    public static boolean hasRequestErrors(final @NotNull ApiErrorMessages apiErrorMessages){
        return !apiErrorMessages.getErrors().isEmpty();
    }

}
