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

package com.hivemq.protocols.fsmjochen;

import java.util.concurrent.CompletableFuture;

public class PATest implements ProtocolAdapterFsmJochen {
    FsmExperiment fsmExperiment;

    public PATest(FsmExperiment fsmExperiment) {
        this.fsmExperiment = fsmExperiment;
    }

    public void start() {
        CompletableFuture.runAsync(() -> {
            if (true) {
                fsmExperiment.transitionTo(FsmExperiment.State.Started);
            } else{
                fsmExperiment.transitionTo(FsmExperiment.State.Error);
            }
        });
    };

    public void stop() {
        CompletableFuture.runAsync(() -> {

            //DO ALL THE WORK

            if (true) {
                fsmExperiment.transitionTo(FsmExperiment.State.Stopped);
            } else{
                fsmExperiment.transitionTo(FsmExperiment.State.Error);
            }
        });
    }
}
