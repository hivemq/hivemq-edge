/*
 * Copyright 2024-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.redis;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class RedisProtocolAdapterInformationTest {

    @Test
    void getProtocolId_MustNotContainWhiteSpaces() {
        final RedisProtocolAdapterInformation information = new RedisProtocolAdapterInformation();
        assertFalse(information.getProtocolId().contains(" "));
    }


    @Test
    void getProtocolId_MustBeAlphaNummercialOrUnderscore() {
        final String ALPHA_NUM = "[A-Za-z0-9_]*";
        final Pattern alphaNumPattern = Pattern.compile(ALPHA_NUM);
        final RedisProtocolAdapterInformation information = new RedisProtocolAdapterInformation();
        assertTrue(alphaNumPattern.matcher(information.getProtocolId()).matches());
    }
}