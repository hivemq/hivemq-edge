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
package com.hivemq.protocols.v2.tag;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * The three-condition rule (design §7.1): an aspect is driven only when its direction is activated, its
 * preference is activated, and the tag is used. Any single condition failing keeps it inactive.
 */
class TagAspectGoalTest {

    @Test
    void active_requiresAllThreeConditions() {
        assertThat(new TagAspectGoal(true, true, true).active()).isTrue();
    }

    @Test
    void active_isFalseWhenAnySingleConditionFails() {
        assertThat(new TagAspectGoal(false, true, true).active()).isFalse(); // direction off
        assertThat(new TagAspectGoal(true, false, true).active()).isFalse(); // preference off
        assertThat(new TagAspectGoal(true, true, false).active()).isFalse(); // not used
    }

    @Test
    void inactive_isNeverActive() {
        assertThat(TagAspectGoal.inactive().active()).isFalse();
    }
}
