package com.pkg.core;

public interface CallStep<I, O> {

    O operate(I input);
}
