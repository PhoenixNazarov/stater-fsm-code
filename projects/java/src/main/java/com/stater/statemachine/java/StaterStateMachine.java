package com.stater.statemachine.java;

import java.text.Collator;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

interface Context {
}

class EmptyContext implements Context {
}

interface ContextJsonAdapter<C extends Context> {
    String toJson(C context);

    C fromJson(String json);
}

@FunctionalInterface
interface Event<C> extends Consumer<C> {
}

@FunctionalInterface
interface NameEvent<C> extends BiConsumer<String, C> {
}

@FunctionalInterface
interface StateEvent<T, C> extends BiConsumer<T, C> {
}

@FunctionalInterface
interface TransitionMiddleware<C> {
    void apply(C context, Event<C> next);
}

@FunctionalInterface
interface TransitionNameMiddleware<C> {
    void apply(String name, C context, NameEvent<C> next);
}

@FunctionalInterface
interface StateMachineFactory<T, C extends Context> {
    StaterStateMachine<T, C> create(List<Transition<T, C>> transitions, C context, T startState, Set<T> states, Map<String, List<TransitionMiddleware<C>>> transitionMiddlewares, List<TransitionNameMiddleware<C>> transitionAllMiddlewares, Map<String, List<Event<C>>> transitionCallbacks, List<NameEvent<C>> transitionAllCallbacks, Map<T, List<Event<C>>> stateCallbacks, List<StateEvent<T, C>> stateAllCallbacks, ContextJsonAdapter<C> contextJsonAdapter);
}

record TransitionJson<T>(String name, T start, T end) {
}

record Transition<T, C extends Context>(String name, T start, T end, Predicate<C> condition, Consumer<C> event) {
}

record JsonSchema<T>(List<T> states, T startState, List<TransitionJson<T>> transitions) {
}

record JsonState<T>(T state, String context) {
}


abstract class StaterStateMachine<T, C extends Context> {
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
        if (transition.condition() != null && !transition.condition().test(context)) {
            throw new IllegalStateException("Condition return false for transition " + name);
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
        return objectMapper.writeValueAsString(new JsonSchema<>(sortedStates, startState, transitions.stream().map(transition -> new TransitionJson<>(transition.name(), transition.start(), transition.end())).toList()));
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

class BaseFSM<T, C extends Context> extends StaterStateMachine<T, C> {
    public BaseFSM(List<Transition<T, C>> transitions, C context, T startState, Set<T> states, Map<String, List<TransitionMiddleware<C>>> transitionMiddlewares, List<TransitionNameMiddleware<C>> transitionAllMiddlewares, Map<String, List<Event<C>>> transitionCallbacks, List<NameEvent<C>> transitionAllCallbacks, Map<T, List<Event<C>>> stateCallbacks, List<StateEvent<T, C>> stateAllCallbacks, ContextJsonAdapter<C> contextJsonAdapter) {
        super(transitions, context, startState, states, transitionMiddlewares, transitionAllMiddlewares, transitionCallbacks, transitionAllCallbacks, stateCallbacks, stateAllCallbacks, contextJsonAdapter);
    }

    public BaseFSM(List<Transition<T, C>> transitions, C context, T startState) {
        super(transitions, context, startState);
    }
}


class StaterStateMachineBuilder<T, C extends Context> {
    private final Map<String, Transition<T, C>> transitions = new HashMap<>();
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
