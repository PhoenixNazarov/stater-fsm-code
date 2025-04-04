package com.stater.statemachine.java;

import java.util.function.BiConsumer;

@FunctionalInterface
public interface StateEvent<T, C> extends BiConsumer<T, C> {
}
