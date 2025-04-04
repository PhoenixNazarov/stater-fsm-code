package com.stater.statemachine.java;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.function.Consumer;
import java.util.function.Predicate;

public record Transition<T, C extends Context>(
        String name,
        T start,
        T end,
        @JsonIgnore
        Predicate<C> condition,
        @JsonIgnore
        Consumer<C> event
) {
}
