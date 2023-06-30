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

/**
 * Convert messages from one type to another. Parameterized using Google convention where FromT is the type you
 * are converting from the ToT, which is the type you are converting to.
 *
 * @author Simon L Johnson
 */
public interface ITranscoder<FromT, ToT> {


    /**
     * Convert the messages from the message supplied into the output format. May
     * return null if a message type is not supported, or may throw if the
     * message transposition led to a fatal error occurring (for example a protocol
     * violation).
     *
     * @param messageIn the message you will convert from
     * @return a new instance of the message you are converting to
     * @throws MessageTranscodingException a fatal error converting the type, for example a protocol violation
     */
    @NotNull TranscodingResult<FromT, ToT> transcode(@NotNull ITranscodingContext context, @NotNull FromT messageIn);

    boolean canHandle(@NotNull ITranscodingContext context, @NotNull Class<? extends FromT> messageType);

}
