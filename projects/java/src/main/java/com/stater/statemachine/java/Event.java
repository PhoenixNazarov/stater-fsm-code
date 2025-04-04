package com.stater.statemachine.java;

import java.util.function.Consumer;

@FunctionalInterface
public interface Event<C> extends Consumer<C> {
}
