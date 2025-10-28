package com.pkg.core;

public class CallStepChain<I, O> {

    private final CallStep<I, O> composed;

    private CallStepChain(CallStep<I, O> composed) {
        this.composed = composed;
    }

    public static <I, O> CallStepChain<I, O> startWith(CallStep<I, O> first) {
        return new CallStepChain<>(first);
    }

    public <N> CallStepChain<I, N> add(CallStep<? super O, ? extends N> next) {
        return new CallStepChain<>(composed.andThen(next));
    }

    public CallStep<I, O> toCallStep() {
        return composed;
    }
}
