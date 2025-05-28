package com.hivemq.edge.adapters.opcua.client;

public record Success<S,F>(S result) implements Result<S,F>{
    public static <S,F> Success<S,F> of(final S result) {
        return new Success<>(result);
    }
}
