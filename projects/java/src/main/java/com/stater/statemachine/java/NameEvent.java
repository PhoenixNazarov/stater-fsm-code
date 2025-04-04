package com.stater.statemachine.java;

import java.util.function.BiConsumer;

@FunctionalInterface
public interface NameEvent<C> extends BiConsumer<String, C> {
}
