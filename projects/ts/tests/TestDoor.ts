import {
    Context,
    ContextJsonAdapter,
    FSMEvent,
    NameFSMEvent,
    StateFSMEvent,
    StateMachineFactory,
    StaterStateMachine, StaterStateMachineBuilder,
    Transition,
    TransitionMiddleware,
    TransitionNameMiddleware
} from "../src/StaterStateMachine";

enum States {
    CLOSE = "CLOSE", AJAR = "AJAR", OPEN = "OPEN"
}

interface DoorFSMContext extends Context {
    degreeOfOpening: number
}

let buildDoorDSMContext: () => DoorFSMContext = () => {
    return {degreeOfOpening: 100}
}


class TypesDoorStateMachine extends StaterStateMachine<States, DoorFSMContext> {
    constructor(
        transitions: Transition<States, DoorFSMContext>[],
        startState: States,
        states: States[],
        context: DoorFSMContext,
        transitionMiddlewares: Map<string, TransitionMiddleware<DoorFSMContext>[]> = new Map(),
        transitionAllMiddlewares: TransitionNameMiddleware<DoorFSMContext>[] = [],
        transitionCallbacks: Map<string, FSMEvent<DoorFSMContext>[]> = new Map(),
        transitionAllCallbacks: NameFSMEvent<DoorFSMContext>[] = [],
        stateCallbacks: Map<States, FSMEvent<DoorFSMContext>[]> = new Map(),
        stateAllCallbacks: StateFSMEvent<States, DoorFSMContext>[] = [],
        contextJsonAdapter: ContextJsonAdapter<DoorFSMContext> | undefined = undefined
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

    ajarPlus() {
        this.transition("ajarPlus");
    }

    ajarMinus() {
        this.transition("ajarMinus");
    }

    preOpen() {
        this.transition("preOpen");
    }

    preClose() {
        this.transition("preClose");
    }

    open() {
        this.transition("open");
    }

    close() {
        this.transition("close");
    }
}


const typedDoorFactory: StateMachineFactory<States, DoorFSMContext> = (
    transitionsA: Transition<States, DoorFSMContext>[],
    startStateA: States,
    statesA: States[],
    contextA: DoorFSMContext,
    transitionMiddlewaresA: Map<string, TransitionMiddleware<DoorFSMContext>[]>,
    transitionAllMiddlewaresA: TransitionNameMiddleware<DoorFSMContext>[],
    transitionCallbacksA: Map<string, FSMEvent<DoorFSMContext>[]>,
    transitionAllCallbacksA: NameFSMEvent<DoorFSMContext>[],
    stateCallbacksA: Map<States, FSMEvent<DoorFSMContext>[]>,
    stateAllCallbacksA: StateFSMEvent<States, DoorFSMContext>[],
    contextJsonAdapterA: ContextJsonAdapter<DoorFSMContext> | undefined
): StaterStateMachine<States, DoorFSMContext> => {
    return new TypesDoorStateMachine(
        transitionsA,
        startStateA,
        statesA,
        contextA,
        transitionMiddlewaresA,
        transitionAllMiddlewaresA,
        transitionCallbacksA,
        transitionAllCallbacksA,
        stateCallbacksA,
        stateAllCallbacksA,
        contextJsonAdapterA
    );
};


function testDoor(door: StaterStateMachine<States, DoorFSMContext>): void {
    console.assert(door.getState() === States.OPEN);
    console.assert(door.getContext().degreeOfOpening === 100);
    door.transition("preClose");
    console.assert(door.getState() === States.AJAR);
    console.assert(door.getContext().degreeOfOpening === 99);
    while (door.getContext().degreeOfOpening > 1) {
        door.transition("ajarMinus");
        console.assert(door.getState() === States.AJAR);
    }
    console.assert(door.getContext().degreeOfOpening === 1);
    door.transition("close");
    console.assert(door.getContext().degreeOfOpening === 0);
    console.assert(door.getState() === States.CLOSE);
    door.transition("preOpen");
    console.assert(door.getContext().degreeOfOpening === 1);
    console.assert(door.getState() === States.AJAR);
    door.transition("ajarPlus");
    console.assert(door.getState() === States.AJAR);
    console.assert(door.getContext().degreeOfOpening === 2);
    while (door.getContext().degreeOfOpening < 99) {
        door.transition("ajarPlus");
        console.assert(door.getState() === States.AJAR);
    }
    door.transition("open");
    console.assert(door.getState() === States.OPEN);
    console.assert(door.getContext().degreeOfOpening === 100);
}

function typedTestDoor(door: TypesDoorStateMachine): void {
    console.assert(door.getState() === States.OPEN);
    console.assert(door.getContext().degreeOfOpening === 100);
    door.preClose();
    console.assert(door.getState() === States.AJAR);
    console.assert(door.getContext().degreeOfOpening === 99);
    while (door.getContext().degreeOfOpening > 1) {
        door.ajarMinus();
        console.assert(door.getState() === States.AJAR);
    }
    console.assert(door.getContext().degreeOfOpening === 1);
    door.close();
    console.assert(door.getContext().degreeOfOpening === 0);
    console.assert(door.getState() === States.CLOSE);
    door.preOpen();
    console.assert(door.getContext().degreeOfOpening === 1);
    console.assert(door.getState() === States.AJAR);
    door.ajarPlus();
    console.assert(door.getState() === States.AJAR);
    console.assert(door.getContext().degreeOfOpening === 2);
    while (door.getContext().degreeOfOpening < 99) {
        door.ajarPlus();
        console.assert(door.getState() === States.AJAR);
    }
    door.open();
    console.assert(door.getState() === States.OPEN);
    console.assert(door.getContext().degreeOfOpening === 100);
}

function testSimpleBuild(): void {
    const doorFSM = new TypesDoorStateMachine([
            {
                name: "preOpen",
                start: States.CLOSE,
                end: States.AJAR,
                event: (context) => {
                    context.degreeOfOpening = 1;
                }
            },
            {
                name: "preClose",
                start: States.OPEN,
                end: States.AJAR,
                event: (context) => {
                    context.degreeOfOpening = 99;
                }
            },
            {
                name: "open",
                start: States.AJAR,
                end: States.OPEN,
                condition: (context: DoorFSMContext) => context.degreeOfOpening >= 99,
                event: (context: DoorFSMContext) => {
                    context.degreeOfOpening = 100;
                }
            },
            {
                name: "close",
                start: States.AJAR,
                end: States.CLOSE,
                condition: (context: DoorFSMContext) => context.degreeOfOpening <= 1,
                event: (context: DoorFSMContext) => {
                    context.degreeOfOpening = 0;
                }
            },
            {
                name: "ajarPlus",
                start: States.AJAR,
                end: States.AJAR,
                condition: (context: DoorFSMContext) => context.degreeOfOpening >= 1 && context.degreeOfOpening <= 98,
                event: (context: DoorFSMContext) => {
                    context.degreeOfOpening++;
                }
            },
            {
                name: "ajarMinus",
                start: States.AJAR,
                end: States.AJAR,
                condition: (context: DoorFSMContext) => context.degreeOfOpening >= 2 && context.degreeOfOpening <= 99,
                event: (context: DoorFSMContext) => {
                    context.degreeOfOpening--;
                }
            }
        ],
        States.OPEN,
        [States.OPEN, States.CLOSE, States.AJAR],
        buildDoorDSMContext(),
    );

    testDoor(doorFSM);
    typedTestDoor(doorFSM);
}


function testBuilder() {
    const doorFSM = new StaterStateMachineBuilder<States, DoorFSMContext>()
        .addTransition("preOpen", States.CLOSE, States.AJAR, undefined, (context) => { context.degreeOfOpening = 1 })
        .addTransition("preClose", States.OPEN, States.AJAR, undefined, (context) => { context.degreeOfOpening = 99 })
        .addTransition("open", States.AJAR, States.OPEN, (context) => context.degreeOfOpening >= 99, (context) => { context.degreeOfOpening = 100 })
        .addTransition("close", States.AJAR, States.CLOSE, (context) => context.degreeOfOpening <= 1, (context) => { context.degreeOfOpening = 0 })
        .addTransition("ajarPlus", States.AJAR, States.AJAR, (context) => context.degreeOfOpening >= 1 && context.degreeOfOpening <= 98, (context) => { context.degreeOfOpening++ })
        .addTransition("ajarMinus", States.AJAR, States.AJAR, (context) => context.degreeOfOpening >= 2 && context.degreeOfOpening <= 99, (context) => { context.degreeOfOpening-- })
        .setContext(buildDoorDSMContext())
        .setStartState(States.OPEN)
        .build();

    testDoor(doorFSM);
}

function structureBuild() {
    return new StaterStateMachineBuilder<States, DoorFSMContext>()
        .addTransition("preOpen", States.CLOSE, States.AJAR)
        .addTransition("preClose", States.OPEN, States.AJAR)
        .addTransition("open", States.AJAR, States.OPEN)
        .addTransition("close", States.AJAR, States.CLOSE)
        .addTransition("ajarPlus", States.AJAR, States.AJAR)
        .addTransition("ajarMinus", States.AJAR, States.AJAR)
}

function eventsBuild(builder: StaterStateMachineBuilder<States, DoorFSMContext>): StaterStateMachineBuilder<States, DoorFSMContext> {
    return builder
        .setTransitionEvent("preOpen", (context: DoorFSMContext) => { context.degreeOfOpening = 1 })
        .setTransitionEvent("preClose", (context: DoorFSMContext) => { context.degreeOfOpening = 99 })
        .setTransitionCondition("open", (context: DoorFSMContext) => context.degreeOfOpening >= 99)
        .setTransitionEvent("open", (context: DoorFSMContext) => { context.degreeOfOpening = 100 })
        .setTransitionCondition("close", (context: DoorFSMContext) => context.degreeOfOpening <= 1)
        .setTransitionEvent("close", (context: DoorFSMContext) => { context.degreeOfOpening = 0 })
        .setTransitionCondition("ajarPlus", (context: DoorFSMContext) => context.degreeOfOpening >= 1 && context.degreeOfOpening <= 98)
        .setTransitionEvent("ajarPlus", (context: DoorFSMContext) => { context.degreeOfOpening++ })
        .setTransitionCondition("ajarMinus", (context: DoorFSMContext) => context.degreeOfOpening >= 2 && context.degreeOfOpening <= 99)
        .setTransitionEvent("ajarMinus", (context: DoorFSMContext) => { context.degreeOfOpening-- });
}


function testBuilder2() {
    const doorFSM = eventsBuild(structureBuild())
        .setContext(buildDoorDSMContext())
        .setStartState(States.OPEN)
        .build()

    testDoor(doorFSM)
}

function testAutoTransition() {
    const doorFSM = eventsBuild(structureBuild())
        .setContext(buildDoorDSMContext())
        .setStartState(States.OPEN)
        .build()

    doorFSM.autoTransition()
    console.assert(doorFSM.getState() == States.AJAR)
}


function testBuilderFactory() {
    const doorFSM = eventsBuild(structureBuild())
        .setContext(buildDoorDSMContext())
        .setStartState(States.OPEN)
        .setFactory(typedDoorFactory)
        .build();

    testDoor(doorFSM);
    console.assert(doorFSM instanceof TypesDoorStateMachine)
    if (doorFSM instanceof TypesDoorStateMachine) {
        typedTestDoor(doorFSM);
    }
}

function testJsonSchema() {
    const validDoorFSM = eventsBuild(structureBuild())
        .setContext(buildDoorDSMContext())
        .setStartState(States.OPEN)
        .build();

    testDoor(validDoorFSM);

    const jsonSchema = validDoorFSM.toJsonSchema();

    const doorFSM = eventsBuild(
        structureBuild()
            .fromJsonSchema(jsonSchema, (stateStr: string) => States[stateStr as keyof typeof States])
    )
        .setContext(buildDoorDSMContext())
        .setFactory(typedDoorFactory)
        .build();

    console.assert(JSON.stringify(jsonSchema) === JSON.stringify(doorFSM.toJsonSchema()));

    testDoor(doorFSM);

    if (doorFSM instanceof TypesDoorStateMachine) {
        typedTestDoor(doorFSM);
    }
}

function testStringGeneric() {
    const validDoorFSM = eventsBuild(structureBuild())
        .setContext(buildDoorDSMContext())
        .setStartState(States.OPEN)
        .build();

    testDoor(validDoorFSM);

    const jsonSchema = validDoorFSM.toJsonSchema();

    const stringDoorFSM = new StaterStateMachineBuilder<string, DoorFSMContext>()
        .fromJsonSchema(jsonSchema, (stateStr: string) => stateStr)
        .setContext(buildDoorDSMContext())
        .build();

    console.assert(stringDoorFSM.getState() === "OPEN");

}

function testJsonDump() {
    class JsonConverter implements ContextJsonAdapter<DoorFSMContext> {
        toJson(context: DoorFSMContext): string {
            return context.degreeOfOpening.toString();
        }

        fromJson(json: string): DoorFSMContext {
            return {degreeOfOpening: parseInt(json)};
        }
    }

    const validDoorFSM = eventsBuild(structureBuild())
        .setContext(buildDoorDSMContext())
        .setStartState(States.OPEN)
        .setContextJsonAdapter(new JsonConverter())
        .build();

    console.assert(validDoorFSM.getState() === States.OPEN);
    console.assert(validDoorFSM.getContext().degreeOfOpening === 100);

    const dump = validDoorFSM.toJson();

    validDoorFSM.transition("preClose");
    console.assert(validDoorFSM.getState() === States.AJAR);
    console.assert(validDoorFSM.getContext().degreeOfOpening === 99);

    validDoorFSM.fromJson(dump, (stateStr: string) => States[stateStr as keyof typeof States]);
    console.assert(validDoorFSM.getState() === States.OPEN);
    console.assert(validDoorFSM.getContext().degreeOfOpening === 100);

}

function testMiddlewareAndCallbacks(){
    let transitionMiddleware = 0;
    let transitionAllMiddleware = 0;
    let subscribeOnTransition = 0;
    let subscribeOnAllTransition = 0;
    let subscribeOnState = 0;
    let subscribeOnAllState = 0;

    const validDoorFSM = eventsBuild(structureBuild())
        .setContext(buildDoorDSMContext())
        .setStartState(States.OPEN)
        .transitionMiddleware("open", (context, next) => {
            transitionMiddleware++;
            next(context);
        })
        .transitionAllMiddleware((name, context, next) => {
            transitionAllMiddleware++;
            next(name, context);
        })
        .subscribeOnTransition("open", () => {
            subscribeOnTransition++;
        })
        .subscribeOnAllTransition((name, context) => {
            subscribeOnAllTransition++;
        })
        .subscribeOnState(States.AJAR, () => {
            subscribeOnState++;
        })
        .subscribeOnAllState((name, context) => {
            subscribeOnAllState++;
        })
        .build();

    testDoor(validDoorFSM);

    console.assert(transitionMiddleware === 1);
    console.assert(transitionAllMiddleware === 200);
    console.assert(subscribeOnTransition === 1);
    console.assert(subscribeOnAllTransition === 200);
    console.assert(subscribeOnState === 198);
    console.assert(subscribeOnAllState === 200);
}


testSimpleBuild()
testBuilder()
testBuilder2()
testAutoTransition()
testBuilderFactory()
testJsonSchema()
testStringGeneric()
testJsonDump()
testMiddlewareAndCallbacks()