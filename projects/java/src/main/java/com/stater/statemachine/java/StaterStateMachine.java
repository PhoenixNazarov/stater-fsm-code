package com.stater.statemachine.java;

import java.io.Console;
import java.text.Collator;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


public abstract class StaterStateMachine<T, C extends Context> {
    private C context;
    private final T startState;
    private final Set<T> states;
    private T state;
    private final List<Transition<T, C>> transitions;
    private Map<T, List<Transition<T, C>>> transitionsGroupedStart = new HashMap<>();
    private Map<String, Transition<T, C>> transitionsByName = new HashMap<>();
    private Map<String, List<TransitionMiddleware<C>>> transitionMiddlewares = new HashMap<>();
    private List<TransitionNameMiddleware<C>> transitionAllMiddlewares = new ArrayList<>();
    private Map<String, List<Event<C>>> transitionCallbacks = new HashMap<>();
    private List<NameEvent<C>> transitionAllCallbacks = new ArrayList<>();
    private Map<T, List<Event<C>>> stateCallbacks = new HashMap<>();
    private List<StateEvent<T, C>> stateAllCallbacks = new ArrayList<>();
    private ContextJsonAdapter<C> contextJsonAdapter;
    private boolean enableEvents = true;

    public StaterStateMachine(List<Transition<T, C>> transitions, C context, T startState, Set<T> states, Map<String, List<TransitionMiddleware<C>>> transitionMiddlewares, List<TransitionNameMiddleware<C>> transitionAllMiddlewares, Map<String, List<Event<C>>> transitionCallbacks, List<NameEvent<C>> transitionAllCallbacks, Map<T, List<Event<C>>> stateCallbacks, List<StateEvent<T, C>> stateAllCallbacks, ContextJsonAdapter<C> contextJsonAdapter) {
        this.transitions = transitions;
        this.context = context;
        this.startState = startState;
        this.states = states;
        this.state = startState;
        this.transitionsGroupedStart = new HashMap<>();
        for (Transition<T, C> transition : transitions) {
            transitionsGroupedStart.computeIfAbsent(transition.start(), k -> new ArrayList<>()).add(transition);
        }
        this.transitionsByName = new HashMap<>();
        for (Transition<T, C> transition : transitions) {
            transitionsByName.put(transition.name(), transition);
        }
        this.transitionMiddlewares = transitionMiddlewares;
        this.transitionAllMiddlewares = transitionAllMiddlewares;
        this.transitionCallbacks = transitionCallbacks;
        this.transitionAllCallbacks = transitionAllCallbacks;
        this.stateCallbacks = stateCallbacks;
        this.stateAllCallbacks = stateAllCallbacks;
        this.contextJsonAdapter = contextJsonAdapter;
    }

    public StaterStateMachine(List<Transition<T, C>> transitions, C context, T startState) {
        this.transitions = transitions;
        this.context = context;
        this.startState = startState;
        this.states = Stream.concat(transitions.stream().map(Transition::start), transitions.stream().map(Transition::end)).collect(Collectors.toSet());
        this.state = startState;
        for (Transition<T, C> transition : transitions) {
            transitionsGroupedStart.computeIfAbsent(transition.start(), k -> new ArrayList<>()).add(transition);
        }
        for (Transition<T, C> transition : transitions) {
            transitionsByName.put(transition.name(), transition);
        }
    }

    public T getState() {
        return state;
    }

    public C getContext() {
        return context;
    }

    public void transition(String name) {
        Transition<T, C> transition = transitionsByName.get(name);
        if (transition == null) {
            throw new IllegalStateException("Transition not found: " + name);
        }
        if (!state.equals(transition.start())) {
            throw new IllegalStateException("Start state does not match transition's start state: " + transition.start());
        }

        Runnable conditionHandler = () -> {
            if (transition.condition() != null && !transition.condition().test(context)) {
                throw new IllegalStateException("Condition return false for transition " + name);
            }
        };

        List<TransitionMiddleware<C>> transitionMiddleware = transitionMiddlewares.get(name);
        Iterator<TransitionMiddleware<C>> middlewareIterator = (transitionMiddleware != null) ? transitionMiddleware.iterator() : Collections.emptyIterator();

        Consumer<C> internalNext = new Consumer<>() {
            @Override
            public void accept(C ctx) {
                if (middlewareIterator.hasNext()) {
                    middlewareIterator.next().apply(ctx, this::accept);
                } else {
                    conditionHandler.run();
                }
            }
        };

        Iterator<TransitionNameMiddleware<C>> allMiddlewareIterator = transitionAllMiddlewares.iterator();
        BiConsumer<String, C> next = new BiConsumer<>() {
            @Override
            public void accept(String transitionName, C ctx) {
                if (allMiddlewareIterator.hasNext()) {
                    allMiddlewareIterator.next().apply(transitionName, ctx, this::accept);
                } else {
                    internalNext.accept(ctx);
                }
            }
        };

        if (enableEvents) {
            next.accept(name, context);
        }

        state = transition.end();
        if (enableEvents && transition.event() != null) {
            transition.event().accept(context);
        }
        transitionCallbacks.getOrDefault(name, Collections.emptyList()).forEach(event -> event.accept(context));
        transitionAllCallbacks.forEach(event -> event.accept(name, context));
        stateCallbacks.getOrDefault(state, Collections.emptyList()).forEach(event -> event.accept(context));
        stateAllCallbacks.forEach(event -> event.accept(state, context));
    }

    public void autoTransition() {
        List<Transition<T, C>> possibleTransitions = transitionsGroupedStart.getOrDefault(state, Collections.emptyList());
        for (Transition<T, C> transition : possibleTransitions) {
            try {
                transition(transition.name());
                return;
            } catch (Exception ignored) {
            }
        }
    }

    public String toJsonSchema() throws Exception {
        Collator collator = Collator.getInstance(Locale.getDefault());
        collator.setStrength(Collator.PRIMARY);
        List<T> sortedStates = new ArrayList<>(states);
        sortedStates.sort(Comparator.comparing(T::toString, collator));
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(new JsonSchema<>(sortedStates, startState, transitions.stream().map(transition -> new Transition<T, EmptyContext>(transition.name(), transition.start(), transition.end(), null, null)).toList()));
    }

    public String toJson() throws Exception {
        if (contextJsonAdapter == null) {
            throw new IllegalStateException("ContextJsonAdapter is not set");
        }
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(new JsonState<>(state, contextJsonAdapter.toJson(context)));
    }

    public void fromJson(String json, Function<String, T> stateConverter) throws Exception {
        JsonState<T> jsonState = new ObjectMapper().readValue(json, new TypeReference<>() {
        });
        this.state = stateConverter.apply(jsonState.state().toString());
        this.context = contextJsonAdapter.fromJson(jsonState.context());
    }

    public void enableEvents() {
        this.enableEvents = true;
    }

    public void disableEvents() {
        this.enableEvents = false;
    }
}

