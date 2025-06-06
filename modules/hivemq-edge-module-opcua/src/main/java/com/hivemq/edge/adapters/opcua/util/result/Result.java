package com.hivemq.edge.adapters.opcua.util.result;

public sealed interface Result<S,F> permits Success, Failure{

}
