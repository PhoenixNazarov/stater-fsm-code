package com.stater.statemachine.java;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class BaseFSM<T, C extends Context> extends StaterStateMachine<T, C> {
    public BaseFSM(List<Transition<T, C>> transitions, C context, T startState, Set<T> states, Map<String, List<TransitionMiddleware<C>>> transitionMiddlewares, List<TransitionNameMiddleware<C>> transitionAllMiddlewares, Map<String, List<Event<C>>> transitionCallbacks, List<NameEvent<C>> transitionAllCallbacks, Map<T, List<Event<C>>> stateCallbacks, List<StateEvent<T, C>> stateAllCallbacks, ContextJsonAdapter<C> contextJsonAdapter) {
        super(transitions, context, startState, states, transitionMiddlewares, transitionAllMiddlewares, transitionCallbacks, transitionAllCallbacks, stateCallbacks, stateAllCallbacks, contextJsonAdapter);
    }

    public BaseFSM(List<Transition<T, C>> transitions, C context, T startState) {
        super(transitions, context, startState);
    }
}
