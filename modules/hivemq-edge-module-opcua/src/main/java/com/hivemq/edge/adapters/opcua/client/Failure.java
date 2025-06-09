package com.hivemq.edge.adapters.opcua.client;

public record Failure<S,F>(F failure) implements Result<S,F>{
    public static <S,F> Failure<S,F> of(final F result) {
        return new Failure<>(result);
    }
}
