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
package com.hivemq.codec.transcoder;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.Optional;

/**
 * Simple bean to encapsulate the results of a transcoding session. The result may have resulted in a failure OR a success but with no output.
 * @author Simon L Johnson
 */
public class TranscodingResult<FromT, ToT> {

    public enum RESULT {
        success, failure, pending
    }

    private RESULT result = RESULT.pending;
    private final ITranscodingContext context;
    private final FromT input;
    private Optional<ToT> output = Optional.empty();
    private String reasonString;
    private Throwable error;


    public TranscodingResult(@NotNull ITranscodingContext context, @NotNull final FromT input) {
        this.context = context;
        this.input = input;
    }

    public void setResult(@NotNull final RESULT result) {
        this.result = result;
    }

    public ITranscodingContext getContext() {
        return context;
    }

    public void setOutput(@NotNull final Optional<ToT> output) {
        this.result = RESULT.success;
        this.output = output;
    }

    @NotNull public RESULT getResult() {
        return result;
    }

    @Nullable public String getReasonString() {
        return reasonString;
    }

    public void setReasonString(@NotNull final String reasonString) {
        this.reasonString = reasonString;
    }

    @NotNull public FromT getInput() {
        return input;
    }

    @NotNull public Optional<ToT> getOutput() {
        return output;
    }

    public boolean isComplete(){
        return result != null && result != RESULT.pending;
    }

    public boolean isError(){
        return result != null && result == RESULT.failure;
    }

    public void setError(final Throwable error) {
        this.result = RESULT.failure;
        this.error = error;
    }

    @Nullable public Throwable getError() {
        return error;
    }

    @Override
    public String toString() {
        return "TranscodingResult{" +
                "result=" +
                result +
                ", context=" +
                context +
                ", input=" +
                input +
                ", output=" +
                output +
                ", reasonString='" +
                reasonString +
                '\'' +
                ", error=" +
                error +
                '}';
    }
}
