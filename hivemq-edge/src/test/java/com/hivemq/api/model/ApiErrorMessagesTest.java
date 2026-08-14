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
package com.hivemq.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.http.error.Error;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * What an API caller is actually told.
 *
 * <p>EDG-891 P2: a rejected enum value produced the constant {@code "Invalid user supplied data"},
 * while the message naming the permitted values was built, carried all the way here, and then dropped
 * because only the title was mapped onto the wire model.
 */
class ApiErrorMessagesTest {

    @Test
    void theSpecificDetailIsWhatReachesTheCaller() {
        final ApiErrorMessages messages = new ApiErrorMessages();
        messages.addError(
                new ApiErrorMessage(
                        "$.tls.tlsChecksFull.trustMode",
                        "Invalid user supplied data",
                        "$.tls.tlsChecksFull.trustMode: does not have a value in the enumeration [CHAIN, ALLOW_LIST, ANY_CERT]"));

        final List<Error> errors = messages.toErrorList();

        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).getDetail())
                .as("the caller needs to know which values are allowed, not that something was invalid")
                .contains("enumeration")
                .contains("ANY_CERT")
                .isNotEqualTo("Invalid user supplied data");
        assertThat(errors.get(0).getParameter()).isEqualTo("$.tls.tlsChecksFull.trustMode");
    }

    @Test
    void theTitleIsUsedWhenThereIsNoDetail() {
        final ApiErrorMessages messages = new ApiErrorMessages();
        messages.addError(new ApiErrorMessage("id", "Required field was null or empty", null));

        assertThat(messages.toErrorList().get(0).getDetail()).isEqualTo("Required field was null or empty");
    }

    @Test
    void aBlankDetailFallsBackToTheTitle() {
        final ApiErrorMessages messages = new ApiErrorMessages();
        messages.addError(new ApiErrorMessage("id", "Required field was null or empty", "   "));

        assertThat(messages.toErrorList().get(0).getDetail()).isEqualTo("Required field was null or empty");
    }

    @Test
    void neitherTitleNorDetailYieldsAnEmptyStringRatherThanNull() {
        final ApiErrorMessages messages = new ApiErrorMessages();
        messages.addError(new ApiErrorMessage("id", null, null));

        assertThat(messages.toErrorList().get(0).getDetail()).isEmpty();
    }

    @Test
    void everyErrorIsCarriedThroughInOrder() {
        final ApiErrorMessages messages = new ApiErrorMessages();
        messages.addError(new ApiErrorMessage("a", "title-a", "detail-a"));
        messages.addError(new ApiErrorMessage("b", "title-b", null));

        assertThat(messages.toErrorList())
                .extracting(Error::getDetail, Error::getParameter)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("detail-a", "a"),
                        org.assertj.core.groups.Tuple.tuple("title-b", "b"));
    }
}
