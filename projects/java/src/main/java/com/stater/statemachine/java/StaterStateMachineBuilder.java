package com.stater.statemachine.java;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class StaterStateMachineBuilder<T, C extends Context> {
    private final Map<String, Transition<T, C>> transitions = new LinkedHashMap<>();
    private T state;
    private final Set<T> states = new HashSet<>();
    private C context;

    private final Map<String, List<TransitionMiddleware<C>>> transitionMiddlewares = new HashMap<>();
    private final List<TransitionNameMiddleware<C>> transitionAllMiddlewares = new ArrayList<>();

    private final Map<String, List<Event<C>>> transitionCallbacks = new HashMap<>();
    private final List<NameEvent<C>> transitionAllCallbacks = new ArrayList<>();

    private final Map<T, List<Event<C>>> stateCallbacks = new HashMap<>();
    private final List<StateEvent<T, C>> stateAllCallbacks = new ArrayList<>();
    private ContextJsonAdapter<C> contextJsonAdapter;

    private StateMachineFactory<T, C> factory = BaseFSM::new;

    public StaterStateMachineBuilder<T, C> addTransition(String name, T start, T end, Predicate<C> condition, Consumer<C> event) {
        states.add(start);
        states.add(end);
        transitions.put(name, new Transition<>(name, start, end, condition, event));
        return this;
    }

    public StaterStateMachineBuilder<T, C> addTransition(String name, T start, T end, Predicate<C> condition) {
        states.add(start);
        states.add(end);
        transitions.put(name, new Transition<>(name, start, end, condition, c -> {
        }));
        return this;
    }

    public StaterStateMachineBuilder<T, C> addTransition(String name, T start, T end, Consumer<C> event) {
        states.add(start);
        states.add(end);
        transitions.put(name, new Transition<>(name, start, end, c -> true, event));
        return this;
    }

    public StaterStateMachineBuilder<T, C> addTransition(String name, T start, T end) {
        states.add(start);
        states.add(end);
        transitions.put(name, new Transition<>(name, start, end, c -> true, c -> {
        }));
        return this;
    }

    public StaterStateMachineBuilder<T, C> addState(T state) {
        states.add(state);
        return this;
    }

    public StaterStateMachineBuilder<T, C> setTransitionCondition(String name, Predicate<C> condition) {
        Transition<T, C> transition = transitions.get(name);
        if (transition == null) throw new IllegalStateException("Transition not found: " + name);
        transitions.put(name, new Transition<>(name, transition.start(), transition.end(), condition, transition.event()));
        return this;
    }

    public StaterStateMachineBuilder<T, C> setTransitionEvent(String name, Consumer<C> event) {
        Transition<T, C> transition = transitions.get(name);
        if (transition == null) throw new IllegalStateException("Transition not found: " + name);
        transitions.put(name, new Transition<>(name, transition.start(), transition.end(), transition.condition(), event));
        return this;
    }

    public StaterStateMachineBuilder<T, C> transitionMiddleware(String name, TransitionMiddleware<C> middleware) {
        transitionMiddlewares.computeIfAbsent(name, k -> new ArrayList<>()).add(middleware);
        return this;
    }

    public StaterStateMachineBuilder<T, C> transitionAllMiddleware(TransitionNameMiddleware<C> middleware) {
        transitionAllMiddlewares.add(middleware);
        return this;
    }

    public StaterStateMachineBuilder<T, C> subscribeOnTransition(String name, Event<C> callback) {
        transitionCallbacks.computeIfAbsent(name, k -> new ArrayList<>()).add(callback);
        return this;
    }

    public StaterStateMachineBuilder<T, C> subscribeOnAllTransition(NameEvent<C> callback) {
        transitionAllCallbacks.add(callback);
        return this;
    }

    public StaterStateMachineBuilder<T, C> subscribeOnState(T state, Event<C> callback) {
        stateCallbacks.computeIfAbsent(state, k -> new ArrayList<>()).add(callback);
        return this;
    }

    public StaterStateMachineBuilder<T, C> subscribeOnAllState(StateEvent<T, C> callback) {
        stateAllCallbacks.add(callback);
        return this;
    }

    public StaterStateMachineBuilder<T, C> setStartState(T state) {
        this.state = state;
        return this;
    }

    public StaterStateMachineBuilder<T, C> setContext(C context) {
        this.context = context;
        return this;
    }

    public StaterStateMachineBuilder<T, C> fromJsonSchema(String schema, Function<String, T> stateConverter) throws JsonProcessingException {
        JsonSchema<T> schemaObject = new ObjectMapper().readValue(schema, new TypeReference<>() {
        });

        schemaObject.states().forEach(state -> addState(stateConverter.apply(state.toString())));
        schemaObject.transitions().forEach(transition -> addTransition(transition.name(), stateConverter.apply(transition.start().toString()), stateConverter.apply(transition.end().toString())));
        setStartState(stateConverter.apply(schemaObject.startState().toString()));
        return this;
    }

    public StaterStateMachineBuilder<T, C> setFactory(StateMachineFactory<T, C> factory) {
        this.factory = factory;
        return this;
    }

    public StaterStateMachineBuilder<T, C> setContextJsonAdapter(ContextJsonAdapter<C> contextJsonAdapter) {
        this.contextJsonAdapter = contextJsonAdapter;
        return this;
    }

    public StaterStateMachine<T, C> build() {
        if (context == null) throw new IllegalStateException("Context must be set");
        if (state == null && !transitions.isEmpty()) state = transitions.values().iterator().next().start();
        return factory.create(new ArrayList<>(transitions.values()), context, state, states, transitionMiddlewares, transitionAllMiddlewares, transitionCallbacks, transitionAllCallbacks, stateCallbacks, stateAllCallbacks, contextJsonAdapter);
    }
}
