export class StaterStateMachine {
    state;
    transitionsGroupedStart;
    transitionsByName;
    transitionMiddlewares;
    transitionAllMiddlewares;
    transitionCallbacks;
    transitionAllCallbacks;
    stateCallbacks;
    stateAllCallbacks;
    contextJsonAdapter;
    transitions;
    startState;
    states;
    context;

    constructor(transitions, startState, states, context, transitionMiddlewares = new Map(), transitionAllMiddlewares = [], transitionCallbacks = new Map(), transitionAllCallbacks = [], stateCallbacks = new Map(), stateAllCallbacks = [], contextJsonAdapter, state = startState) {
        this.transitions = transitions
        this.startState = startState
        this.states = states
        this.context = context
        this.state = state;
        this.transitionsGroupedStart = new Map();
        this.transitionsByName = new Map();
        transitions.forEach(el => {
            this.transitionsByName.set(el.name, el)
            let grouped = this.transitionsGroupedStart.get(el.start)
            if (grouped) {
                grouped.push(el)
            } else {
                this.transitionsGroupedStart.set(el.start, [el])
            }
        })
        this.transitionMiddlewares = transitionMiddlewares;
        this.transitionAllMiddlewares = transitionAllMiddlewares;
        this.transitionCallbacks = transitionCallbacks;
        this.transitionAllCallbacks = transitionAllCallbacks;
        this.stateCallbacks = stateCallbacks;
        this.stateAllCallbacks = stateAllCallbacks;
        this.contextJsonAdapter = contextJsonAdapter;
    }

    getState() {
        return this.state;
    }

    getContext() {
        return this.context;
    }

    transition(name) {
        const transition = this.transitionsByName.get(name);
        if (!transition) {
            throw new Error(`Transition not found: ${name}`);
        }

        const conditionHandler = () => {
            if (this.state !== transition.start) {
                throw new Error(`Start state does not match transition's start state: ${transition.start}`);
            }
            if (transition.condition && !transition.condition(this.context)) {
                throw new Error(`Condition return false for transition ${name}`);
            }
        };

        let index = 0;
        let index2 = 0;
        const transitionMiddleware = this.transitionMiddlewares.get(name);

        const internalNext = (context) => {
            if (transitionMiddleware && index2 < transitionMiddleware.length) {
                transitionMiddleware[index2++](context, internalNext);
            } else {
                conditionHandler();
            }
        }

        const next = (name, context) => {
            if (index < this.transitionAllMiddlewares.length) {
                this.transitionAllMiddlewares[index++](name, context, next);
            } else {
                internalNext(context);
            }
        }

        next(name, this.context);

        this.state = transition.end;
        if (transition.event) {
            transition.event(this.context);
        }

        this.transitionAllCallbacks.forEach(callback => callback(name, this.context));
        this.transitionCallbacks.get(name)?.forEach(callback => callback(this.context));
        this.stateAllCallbacks.forEach(callback => callback(this.state, this.context));
        this.stateCallbacks.get(this.state)?.forEach(callback => callback(this.context));
    }

    autoTransition() {
        const transitionsForState = this.transitionsGroupedStart.get(this.state) || [];
        for (const transition of transitionsForState) {
            try {
                this.transition(transition.name);
                return;
            } catch (_) {
            }
        }
    }

    toJsonSchema() {
        return JSON.stringify({
            states: this.states, startState: this.startState, transitions: this.transitions
        });
    }

    toJson() {
        if (!this.contextJsonAdapter) {
            throw new Error('ContextJsonAdapter is not provided');
        }
        return JSON.stringify({
            state: this.state, context: this.contextJsonAdapter.toJson(this.context)
        });
    }

    fromJson(json, stateConverter) {
        const jsonState = JSON.parse(json);
        this.state = stateConverter(jsonState.state.toString());
        if (this.contextJsonAdapter) {
            this.context = this.contextJsonAdapter.fromJson(jsonState.context);
        }
    }
}


class BaseFSM extends StaterStateMachine {
    constructor(transitions, startState, states, context, transitionMiddlewares, transitionAllMiddlewares, transitionCallbacks, transitionAllCallbacks, stateCallbacks, stateAllCallbacks, contextJsonAdapter) {
        super(transitions, startState, states, context, transitionMiddlewares, transitionAllMiddlewares, transitionCallbacks, transitionAllCallbacks, stateCallbacks, stateAllCallbacks, contextJsonAdapter);
    }
}


export class StaterStateMachineBuilder {
    transitions = new Map();
    state = null;
    states = [];
    context = null;

    transitionMiddlewares = new Map();
    transitionAllMiddlewares = [];

    transitionCallbacks = new Map();
    transitionAllCallbacks = [];

    stateCallbacks = new Map();
    stateAllCallbacks = [];

    factory = (transitions, startState, states, context, transitionMiddlewares, transitionAllMiddlewares, transitionCallbacks, transitionAllCallbacks, stateCallbacks, stateAllCallbacks, contextJsonAdapter) => {
        return new BaseFSM(transitions, startState, states, context, transitionMiddlewares, transitionAllMiddlewares, transitionCallbacks, transitionAllCallbacks, stateCallbacks, stateAllCallbacks, contextJsonAdapter);
    };

    contextJsonAdapter;

    addTransition(name, start, end, condition, event) {
        this.addState(start);
        this.addState(end);
        this.transitions.set(name, {name, start, end, condition, event});
        return this;
    }

    addState(state) {
        if (!this.states.includes(state)) {
            this.states.push(state);
        }
        return this;
    }

    setTransitionCondition(name, condition) {
        const transition = this.transitions.get(name);
        if (!transition) throw new Error(`Transition not found: ${name}`);
        transition.condition = condition;
        return this;
    }

    setTransitionEvent(name, event) {
        const transition = this.transitions.get(name);
        if (!transition) throw new Error(`Transition not found: ${name}`);
        transition.event = event;
        return this;
    }

    transitionMiddleware(name, middleware) {
        const middlewares = this.transitionMiddlewares.get(name) || [];
        middlewares.push(middleware);
        this.transitionMiddlewares.set(name, middlewares);
        return this;
    }

    transitionAllMiddleware(middleware) {
        this.transitionAllMiddlewares.push(middleware);
        return this;
    }

    subscribeOnTransition(name, callback) {
        const callbacks = this.transitionCallbacks.get(name) || [];
        callbacks.push(callback);
        this.transitionCallbacks.set(name, callbacks);
        return this;
    }

    subscribeOnAllTransition(callback) {
        this.transitionAllCallbacks.push(callback);
        return this;
    }

    subscribeOnState(state, callback) {
        const callbacks = this.stateCallbacks.get(state) || [];
        callbacks.push(callback);
        this.stateCallbacks.set(state, callbacks);
        return this;
    }

    subscribeOnAllState(callback) {
        this.stateAllCallbacks.push(callback);
        return this;
    }

    setStartState(state) {
        this.state = state;
        return this;
    }

    setContext(context) {
        this.context = context;
        return this;
    }

    fromJsonSchema(schema, stateConverter) {
        const jsonSchema = JSON.parse(schema);
        jsonSchema.states.forEach((state) => this.addState(stateConverter(state)));
        jsonSchema.transitions.forEach((transition) => {
            this.addTransition(transition.name, stateConverter(transition.start), stateConverter(transition.end));
        });
        this.setStartState(stateConverter(jsonSchema.startState));
        return this;
    }

    setFactory(factory) {
        this.factory = factory;
        return this;
    }

    setContextJsonAdapter(contextJsonAdapter) {
        this.contextJsonAdapter = contextJsonAdapter;
        return this;
    }

    build() {
        if (!this.state || !this.context) {
            throw new Error("Start state and context must be set");
        }

        return this.factory(Array.from(this.transitions.values()), this.state, this.states, this.context, this.transitionMiddlewares, this.transitionAllMiddlewares, this.transitionCallbacks, this.transitionAllCallbacks, this.stateCallbacks, this.stateAllCallbacks, this.contextJsonAdapter);
    }
}