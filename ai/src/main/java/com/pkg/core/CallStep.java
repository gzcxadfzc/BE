package com.pkg.core;


import java.util.Objects;

public interface CallStep<I, O> {

    O operate(I input);

    default <N> CallStep<I, N> andThen(CallStep<? super O, ? extends N> next) {
        Objects.requireNonNull(next);
        return input -> next.operate(operate(input));
    }
}
