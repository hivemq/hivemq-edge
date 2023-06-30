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
package com.hivemq.mqttsn;

/**
 * Used to be notified when a AWAKE flush is complete (this maybe partial if the client has restrictions on receive max).
 * When fired, this should deal with notifying the client of the completion by way of PINGRESP
 * @author Simon L Johnson
 */
@FunctionalInterface
public interface IAwakeFlushCompleteCallback {

    void flushComplete();

}
