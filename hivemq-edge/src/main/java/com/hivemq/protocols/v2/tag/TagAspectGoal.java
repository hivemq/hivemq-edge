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

/**
 * The goal of a single tag aspect — the three-condition rule. An aspect is driven if and only if
 * <b>all three</b> conditions hold:
 * <ol>
 * <li>the adapter direction is activated — northbound for a read aspect, southbound for a write aspect;</li>
 * <li>the tag aspect is activated in the configuration ({@code read-activated} / {@code write-activated});</li>
 * <li>the tag is used — a configured mapping consumes (read) or produces to (write) it.</li>
 * </ol>
 * The user never sets {@code used} directly; it is derived from the configuration's mapping graph
 * and flips as part of the tags-only gentlest transition. Deactivating an adapter direction returns that side's
 * aspects to {@code DEACTIVATED} without touching the other side — the direction switch is a master switch.
 *
 * @param directionActivated whether the adapter direction this aspect belongs to is activated.
 * @param aspectActivated    the persisted per-aspect activation preference for this tag.
 * @param used               whether a configured mapping consumes or produces this tag.
 */
public record TagAspectGoal(boolean directionActivated, boolean aspectActivated, boolean used) {

    /**
     * @return the goal in which no condition holds — the aspect must be deactivated.
     */
    public static TagAspectGoal inactive() {
        return new TagAspectGoal(false, false, false);
    }

    /**
     * @return {@code true} when all three conditions hold and the aspect must be driven.
     */
    public boolean active() {
        return directionActivated && aspectActivated && used;
    }
}
