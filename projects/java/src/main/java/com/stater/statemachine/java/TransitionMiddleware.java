package com.stater.statemachine.java;

@FunctionalInterface
public interface TransitionMiddleware<C> {
    void apply(C context, Event<C> next);
}
