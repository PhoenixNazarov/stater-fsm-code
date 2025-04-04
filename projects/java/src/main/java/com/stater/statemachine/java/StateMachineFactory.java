package com.stater.statemachine.java;

import java.util.List;
import java.util.Map;
import java.util.Set;

@FunctionalInterface
public interface StateMachineFactory<T, C extends Context> {
    StaterStateMachine<T, C> create(List<Transition<T, C>> transitions, C context, T startState, Set<T> states, Map<String, List<TransitionMiddleware<C>>> transitionMiddlewares, List<TransitionNameMiddleware<C>> transitionAllMiddlewares, Map<String, List<Event<C>>> transitionCallbacks, List<NameEvent<C>> transitionAllCallbacks, Map<T, List<Event<C>>> stateCallbacks, List<StateEvent<T, C>> stateAllCallbacks, ContextJsonAdapter<C> contextJsonAdapter);
}
