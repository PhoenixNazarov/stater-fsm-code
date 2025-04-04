package com.stater.statemachine.java;

@FunctionalInterface
public interface TransitionNameMiddleware<C> {
    void apply(String name, C context, NameEvent<C> next);
}
