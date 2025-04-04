package com.stater.statemachine.java;

import java.util.List;

public record JsonSchema<T>(List<T> states, T startState, List<Transition<T, EmptyContext>> transitions) {
}
