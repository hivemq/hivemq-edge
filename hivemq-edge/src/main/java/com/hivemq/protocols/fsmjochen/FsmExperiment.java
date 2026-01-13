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

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class FsmExperiment {

    private volatile State currentState = State.Stopped;

    private final ProtocolAdapterFsmJochen protocolAdapterFsmJochen;

    public enum State {
        Stopped,
        Starting,
        Started,
        Stopping,
        Error
    }

    final Map<State, List<State>> stateToStatesMap = Map.of(
            State.Stopped, List.of(State.Starting),
            State.Starting, List.of(State.Started, State.Stopping, State.Error),
            State.Started, List.of(State.Stopping, State.Error),
            State.Stopping, List.of(State.Error, State.Stopped),
            State.Error, List.of(State.Starting)
    );

    final Map<State, Function<ProtocolAdapterFsmJochen, State>> transitionsMap = Map.of(
            State.Stopped, pa -> State.Stopped,
            State.Stopping, pa -> {
                try {
                    pa.stop();
                    return State.Stopping;
                } catch (Exception e) {
                    return State.Error;
                }
            },
            State.Started, pa -> State.Started,
            State.Starting, pa -> {
                pa.start();
                return State.Stopping;
            },
            State.Error, pa -> State.Error
    );


    public FsmExperiment(ProtocolAdapterFsmJochen protocolAdapterFsmJochen) {
        this.protocolAdapterFsmJochen = protocolAdapterFsmJochen;
    }

    public synchronized boolean transitionTo(State targetState) {
        if(stateToStatesMap.get(currentState).contains(targetState)) {
            currentState = transitionsMap.get(targetState).apply(protocolAdapterFsmJochen);
            return true;
        } else {
            return false;
        }
    }


    public State getCurrentState() {
        return currentState;
    }

}
