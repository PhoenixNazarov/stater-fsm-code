export interface Context {
}

export interface ContextJsonAdapter<C> {
    toJson(context: C): string

    fromJson(json: string): C
}

export type FSMEvent<C> = (context: C) => void
export type NameFSMEvent<C> = (name: string, context: C) => void
export type StateFSMEvent<T, C> = (state: T, context: C) => void
export type TransitionMiddleware<C> = (context: C, FSMEvent: FSMEvent<C>) => void
export type TransitionNameMiddleware<C> = (name: string, context: C, FSMEvent: NameFSMEvent<C>) => void
export type StateMachineFactory<T, C extends Context> = (
    transitions: Transition<T, C>[],
    state: T,
    states: T[],
    context: C,
    transitionMiddlewares: Map<string, TransitionMiddleware<C>[]>,
    transitionAllMiddlewares: TransitionNameMiddleware<C>[],
    transitionCallbacks: Map<string, FSMEvent<C>[]>,
    transitionAllCallbacks: NameFSMEvent<C>[],
    stateCallbacks: Map<T, FSMEvent<C>[]>,
    stateAllCallbacks: StateFSMEvent<T, C>[],
    contextJsonAdapter?: ContextJsonAdapter<C>
) => StaterStateMachine<T, C>;

export interface Transition<T, C extends Context> {
    name: string
    start: T
    end: T
    condition?: ((context: C) => boolean)
    event?: FSMEvent<C>
}

interface JsonSchema<T> {
    states: T[]
    startState: T
    transitions: Transition<T, Context>[]
}

interface JsonState<T> {
    state: T
    context: string
}


export abstract class StaterStateMachine<T, C extends Context> {
    private state: T;
    private transitionsGroupedStart: Map<T, Transition<T, C>[]>;
    private transitionsByName: Map<string, Transition<T, C>>;
    private transitionMiddlewares: Map<string, TransitionMiddleware<C>[]>;
    private transitionAllMiddlewares: TransitionNameMiddleware<C>[];
    private transitionCallbacks: Map<string, FSMEvent<C>[]>;
    private transitionAllCallbacks: NameFSMEvent<C>[];
    private stateCallbacks: Map<T, FSMEvent<C>[]>;
    private stateAllCallbacks: StateFSMEvent<T, C>[];
    private contextJsonAdapter?: ContextJsonAdapter<C>;

    constructor(
        private transitions: Transition<T, C>[],
        private startState: T,
        private states: T[],
        private context: C,
        transitionMiddlewares: Map<string, TransitionMiddleware<C>[]> = new Map(),
        transitionAllMiddlewares: TransitionNameMiddleware<C>[] = [],
        transitionCallbacks: Map<string, FSMEvent<C>[]> = new Map(),
        transitionAllCallbacks: NameFSMEvent<C>[] = [],
        stateCallbacks: Map<T, FSMEvent<C>[]> = new Map(),
        stateAllCallbacks: StateFSMEvent<T, C>[] = [],
        contextJsonAdapter?: ContextJsonAdapter<C>,
        state: T = startState
    ) {
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

    public getState(): T {
        return this.state;
    }

    public getContext(): C {
        return this.context;
    }

    public transition(name: string): void {
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

        const internalNext = (context: C): void => {
            if (transitionMiddleware && index2 < transitionMiddleware.length) {
                transitionMiddleware[index2++](context, internalNext);
            } else {
                conditionHandler();
            }
        };

        const next = (name: string, context: C): void => {
            if (index < this.transitionAllMiddlewares.length) {
                this.transitionAllMiddlewares[index++](name, context, next);
            } else {
                internalNext(context);
            }
        };

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

    public autoTransition(): void {
        const transitionsForState = this.transitionsGroupedStart.get(this.state) || [];
        for (const transition of transitionsForState) {
            try {
                this.transition(transition.name);
                return;
            } catch (_) {
            }
        }
    }

    public toJsonSchema(): string {
        return JSON.stringify({
            states: this.states,
            startState: this.startState,
            transitions: this.transitions
        });
    }

    public toJson(): string {
        if (!this.contextJsonAdapter) {
            throw new Error('ContextJsonAdapter is not provided');
        }
        return JSON.stringify({
            state: this.state,
            context: this.contextJsonAdapter.toJson(this.context)
        });
    }

    public fromJson(json: string, stateConverter: (state: string) => T): void {
        const jsonState = JSON.parse(json) as JsonState<T>;
        this.state = stateConverter(String(jsonState.state));
        if (this.contextJsonAdapter) {
            this.context = this.contextJsonAdapter.fromJson(jsonState.context);
        }
    }
}


class BaseFSM<T, C extends Context> extends StaterStateMachine<T, C> {
    constructor(
        transitions: Transition<T, C>[],
        startState: T,
        states: T[],
        context: C,
        transitionMiddlewares: Map<string, TransitionMiddleware<C>[]>,
        transitionAllMiddlewares: TransitionNameMiddleware<C>[],
        transitionCallbacks: Map<string, FSMEvent<C>[]>,
        transitionAllCallbacks: NameFSMEvent<C>[],
        stateCallbacks: Map<T, FSMEvent<C>[]>,
        stateAllCallbacks: StateFSMEvent<T, C>[],
        contextJsonAdapter?: ContextJsonAdapter<C>
    ) {
        super(
            transitions,
            startState,
            states,
            context,
            transitionMiddlewares,
            transitionAllMiddlewares,
            transitionCallbacks,
            transitionAllCallbacks,
            stateCallbacks,
            stateAllCallbacks,
            contextJsonAdapter
        );
    }
}


export class StaterStateMachineBuilder<T extends string, C extends Context> {
    private transitions: Map<string, Transition<T, C>> = new Map();
    private state: T | null = null;
    private states: T[] = [];
    private context: C | null = null;

    private transitionMiddlewares: Map<string, TransitionMiddleware<C>[]> = new Map();
    private transitionAllMiddlewares: TransitionNameMiddleware<C>[] = [];

    private transitionCallbacks: Map<string, FSMEvent<C>[]> = new Map();
    private transitionAllCallbacks: NameFSMEvent<C>[] = [];

    private stateCallbacks: Map<T, FSMEvent<C>[]> = new Map();
    private stateAllCallbacks: StateFSMEvent<T, C>[] = [];

    private factory: StateMachineFactory<T, C> = (
        transitions,
        startState,
        states,
        context,
        transitionMiddlewares,
        transitionAllMiddlewares,
        transitionCallbacks,
        transitionAllCallbacks,
        stateCallbacks,
        stateAllCallbacks,
        contextJsonAdapter
    ) => {
        return new BaseFSM(
            transitions,
            startState,
            states,
            context,
            transitionMiddlewares,
            transitionAllMiddlewares,
            transitionCallbacks,
            transitionAllCallbacks,
            stateCallbacks,
            stateAllCallbacks,
            contextJsonAdapter
        );
    };

    private contextJsonAdapter?: ContextJsonAdapter<C>;

    addTransition(
        name: string,
        start: T,
        end: T,
        condition?: (context: C) => boolean,
        event?: FSMEvent<C>
    ): this {
        this.addState(start);
        this.addState(end);
        this.transitions.set(name, {name, start, end, condition, event});
        return this;
    }

    addState(state: T): this {
        if (!this.states.includes(state)) {
            this.states.push(state);
        }
        return this;
    }

    setTransitionCondition(name: string, condition: (context: C) => boolean): this {
        const transition = this.transitions.get(name);
        if (!transition) throw new Error(`Transition not found: ${name}`);
        transition.condition = condition;
        return this;
    }

    setTransitionEvent(name: string, event: (context: C) => void): this {
        const transition = this.transitions.get(name);
        if (!transition) throw new Error(`Transition not found: ${name}`);
        transition.event = event;
        return this;
    }

    transitionMiddleware(name: string, middleware: TransitionMiddleware<C>): this {
        const middlewares = this.transitionMiddlewares.get(name) || [];
        middlewares.push(middleware);
        this.transitionMiddlewares.set(name, middlewares);
        return this;
    }

    transitionAllMiddleware(middleware: TransitionNameMiddleware<C>): this {
        this.transitionAllMiddlewares.push(middleware);
        return this;
    }

    subscribeOnTransition(name: string, callback: FSMEvent<C>): this {
        const callbacks = this.transitionCallbacks.get(name) || [];
        callbacks.push(callback);
        this.transitionCallbacks.set(name, callbacks);
        return this;
    }

    subscribeOnAllTransition(callback: NameFSMEvent<C>): this {
        this.transitionAllCallbacks.push(callback);
        return this;
    }

    subscribeOnState(state: T, callback: FSMEvent<C>): this {
        const callbacks = this.stateCallbacks.get(state) || [];
        callbacks.push(callback);
        this.stateCallbacks.set(state, callbacks);
        return this;
    }

    subscribeOnAllState(callback: StateFSMEvent<T, C>): this {
        this.stateAllCallbacks.push(callback);
        return this;
    }

    setStartState(state: T): this {
        this.state = state;
        return this;
    }

    setContext(context: C): this {
        this.context = context;
        return this;
    }

    fromJsonSchema(schema: string, stateConverter: (state: string) => T): this {
        const jsonSchema: JsonSchema<T> = JSON.parse(schema);
        jsonSchema.states.forEach((state: string) => this.addState(stateConverter(state)));
        jsonSchema.transitions.forEach((transition) => {
            this.addTransition(
                transition.name,
                stateConverter(transition.start),
                stateConverter(transition.end)
            );
        });
        this.setStartState(stateConverter(jsonSchema.startState));
        return this;
    }

    setFactory(factory: StateMachineFactory<T, C>): this {
        this.factory = factory;
        return this;
    }

    setContextJsonAdapter(contextJsonAdapter: ContextJsonAdapter<C>): this {
        this.contextJsonAdapter = contextJsonAdapter;
        return this;
    }

    build(): StaterStateMachine<T, C> {
        if (!this.state || !this.context) {
            throw new Error("Start state and context must be set");
        }

        return this.factory(
            Array.from(this.transitions.values()),
            this.state,
            this.states,
            this.context,
            this.transitionMiddlewares,
            this.transitionAllMiddlewares,
            this.transitionCallbacks,
            this.transitionAllCallbacks,
            this.stateCallbacks,
            this.stateAllCallbacks,
            this.contextJsonAdapter
        );
    }
}