package com.hivemq.edge.adapters.opcua.client;

public sealed interface Result<S, F> permits Success, Failure {

}
