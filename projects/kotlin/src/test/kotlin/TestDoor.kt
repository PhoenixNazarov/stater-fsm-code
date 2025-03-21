import org.example.*

fun main() {
    val tester = TestDoor()

    tester.testSimpleBuild()
    tester.testBuilder()
    tester.testBuilder2()
    tester.testAutoTransition()
    tester.testBuilderFactory()
    tester.testJsonSchema()
    tester.testStringGeneric()
    tester.testJsonDump()
    tester.testMiddlewareAndCallbacks()
}


class TestDoor {
    enum class States {
        CLOSE, AJAR, OPEN
    }

    data class DoorFSMContext(
        var degreeOfOpening: Int = 100,
    ) : Context


    class TypesDoorStateMachine(
        transitions: List<Transition<States, DoorFSMContext>>,
        startState: States,
        states: List<States>,
        context: DoorFSMContext,
        transitionMiddlewares: Map<String, List<TransitionMiddleware<DoorFSMContext>>> = mapOf(),
        transitionAllMiddlewares: List<TransitionNameMiddleware<DoorFSMContext>> = listOf(),
        transitionCallbacks: Map<String, List<Event<DoorFSMContext>>> = mapOf(),
        transitionAllCallbacks: List<NameEvent<DoorFSMContext>> = listOf(),
        stateCallbacks: Map<States, List<Event<DoorFSMContext>>> = mapOf(),
        stateAllCallbacks: List<StateEvent<States, DoorFSMContext>> = listOf(),
        contextJsonAdapter: ContextJsonAdapter<DoorFSMContext>? = null,
    ) : StaterStateMachine<States, DoorFSMContext>(
        transitions = transitions,
        startState = startState,
        states = states,
        context = context,
        transitionMiddlewares = transitionMiddlewares,
        transitionAllMiddlewares = transitionAllMiddlewares,
        transitionCallbacks = transitionCallbacks,
        transitionAllCallbacks = transitionAllCallbacks,
        stateCallbacks = stateCallbacks,
        stateAllCallbacks = stateAllCallbacks,
        contextJsonAdapter = contextJsonAdapter
    ) {
        fun ajarPlus() = transition("ajarPlus")
        fun ajarMinus() = transition("ajarMinus")
        fun preOpen() = transition("preOpen")
        fun preClose() = transition("preClose")
        fun open() = transition("open")
        fun close() = transition("close")
    }

    private val typedDoorFactory: StateMachineFactory<States, DoorFSMContext> = {
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
            contextJsonAdapterA,
        ->
        TypesDoorStateMachine(
            transitions = transitionsA,
            startState = startStateA,
            states = statesA,
            context = contextA,
            transitionMiddlewares = transitionMiddlewaresA,
            transitionAllMiddlewares = transitionAllMiddlewaresA,
            transitionCallbacks = transitionCallbacksA,
            transitionAllCallbacks = transitionAllCallbacksA,
            stateCallbacks = stateCallbacksA,
            stateAllCallbacks = stateAllCallbacksA,
            contextJsonAdapter = contextJsonAdapterA
        )
    }

    private fun assert(value: Boolean) {
        if (!value) {
            error("")
        }
    }

    private fun testDoor(door: StaterStateMachine<States, DoorFSMContext>) {
        assert(door.getState() == States.OPEN)
        assert(door.getContext().degreeOfOpening == 100)
        door.transition("preClose")
        assert(door.getState() == States.AJAR)
        assert(door.getContext().degreeOfOpening == 99)
        while (door.getContext().degreeOfOpening > 1) {
            door.transition("ajarMinus")
            assert(door.getState() == States.AJAR)
        }
        assert(door.getContext().degreeOfOpening == 1)
        door.transition("close")
        assert(door.getContext().degreeOfOpening == 0)
        assert(door.getState() == States.CLOSE)
        door.transition("preOpen")
        assert(door.getContext().degreeOfOpening == 1)
        assert(door.getState() == States.AJAR)
        door.transition("ajarPlus")
        assert(door.getState() == States.AJAR)
        assert(door.getContext().degreeOfOpening == 2)
        while (door.getContext().degreeOfOpening < 99) {
            door.transition("ajarPlus")
            assert(door.getState() == States.AJAR)
        }
        door.transition("open")
        assert(door.getState() == States.OPEN)
        assert(door.getContext().degreeOfOpening == 100)
    }

    private fun typedTestDoor(door: TypesDoorStateMachine) {
        assert(door.getState() == States.OPEN)
        assert(door.getContext().degreeOfOpening == 100)
        door.preClose()
        assert(door.getState() == States.AJAR)
        assert(door.getContext().degreeOfOpening == 99)
        while (door.getContext().degreeOfOpening > 1) {
            door.ajarMinus()
            assert(door.getState() == States.AJAR)
        }
        assert(door.getContext().degreeOfOpening == 1)
        door.close()
        assert(door.getContext().degreeOfOpening == 0)
        assert(door.getState() == States.CLOSE)
        door.preOpen()
        assert(door.getContext().degreeOfOpening == 1)
        assert(door.getState() == States.AJAR)
        door.ajarPlus()
        assert(door.getState() == States.AJAR)
        assert(door.getContext().degreeOfOpening == 2)
        while (door.getContext().degreeOfOpening < 99) {
            door.ajarPlus()
            assert(door.getState() == States.AJAR)
        }
        door.open()
        assert(door.getState() == States.OPEN)
        assert(door.getContext().degreeOfOpening == 100)
    }

    fun testSimpleBuild() {
        val doorFSM = TypesDoorStateMachine(
            startState = States.OPEN,
            states = listOf(States.OPEN, States.CLOSE, States.AJAR),
            context = DoorFSMContext(),
            transitions = listOf(
                Transition(name = "preOpen",
                    start = States.CLOSE,
                    end = States.AJAR,
                    event = { it.degreeOfOpening = 1 }),
                Transition(name = "preClose",
                    start = States.OPEN,
                    end = States.AJAR,
                    event = { it.degreeOfOpening = 99 }),
                Transition(name = "open",
                    start = States.AJAR,
                    end = States.OPEN,
                    condition = { it.degreeOfOpening >= 99 },
                    event = { it.degreeOfOpening = 100 }),
                Transition(name = "close",
                    start = States.AJAR,
                    end = States.CLOSE,
                    condition = { it.degreeOfOpening <= 1 },
                    event = { it.degreeOfOpening = 0 }),
                Transition(name = "ajarPlus",
                    start = States.AJAR,
                    end = States.AJAR,
                    condition = { it.degreeOfOpening in 1..98 },
                    event = { it.degreeOfOpening++ }),
                Transition(name = "ajarMinus",
                    start = States.AJAR,
                    end = States.AJAR,
                    condition = { it.degreeOfOpening in 2..99 },
                    event = { it.degreeOfOpening-- })
            )
        )
        testDoor(doorFSM)
        typedTestDoor(doorFSM)
    }

    fun testBuilder() {
        val doorFSM = StaterStateMachineBuilder<States, DoorFSMContext>()
            .addTransition("preOpen", States.CLOSE, States.AJAR, event = { it.degreeOfOpening = 1 })
            .addTransition("preClose", States.OPEN, States.AJAR, event = { it.degreeOfOpening = 99 })
            .addTransition("open", States.AJAR, States.OPEN, { it.degreeOfOpening >= 99 }, { it.degreeOfOpening = 100 })
            .addTransition("close", States.AJAR, States.CLOSE, { it.degreeOfOpening <= 1 }, { it.degreeOfOpening = 0 })
            .addTransition(
                "ajarPlus",
                States.AJAR,
                States.AJAR,
                { it.degreeOfOpening in 1..98 },
                { it.degreeOfOpening++ })
            .addTransition(
                "ajarMinus",
                States.AJAR,
                States.AJAR,
                { it.degreeOfOpening in 2..99 },
                { it.degreeOfOpening-- })
            .setContext(DoorFSMContext())
            .setStartState(States.OPEN)
            .build()

        testDoor(doorFSM)
    }

    private fun structureBuild() =
        StaterStateMachineBuilder<States, DoorFSMContext>()
            .addTransition("preOpen", States.CLOSE, States.AJAR)
            .addTransition("preClose", States.OPEN, States.AJAR)
            .addTransition("open", States.AJAR, States.OPEN)
            .addTransition("close", States.AJAR, States.CLOSE)
            .addTransition("ajarPlus", States.AJAR, States.AJAR)
            .addTransition("ajarMinus", States.AJAR, States.AJAR)

    private fun eventsBuild(builder: StaterStateMachineBuilder<States, DoorFSMContext>) =
        builder.setTransitionEvent("preOpen") { it.degreeOfOpening = 1 }
            .setTransitionEvent("preClose") { it.degreeOfOpening = 99 }
            .setTransitionCondition("open") { it.degreeOfOpening >= 99 }
            .setTransitionEvent("open") { it.degreeOfOpening = 100 }
            .setTransitionCondition("close") { it.degreeOfOpening <= 1 }
            .setTransitionEvent("close") { it.degreeOfOpening = 0 }
            .setTransitionCondition("ajarPlus") { it.degreeOfOpening in 1..98 }
            .setTransitionEvent("ajarPlus") { it.degreeOfOpening++ }
            .setTransitionCondition("ajarMinus") { it.degreeOfOpening in 2..99 }
            .setTransitionEvent("ajarMinus") { it.degreeOfOpening-- }

    fun testBuilder2() {
        val doorFSM = eventsBuild(structureBuild())
            .setContext(DoorFSMContext())
            .setStartState(States.OPEN)
            .build()

        testDoor(doorFSM)
    }

    fun testAutoTransition() {
        val doorFSM = eventsBuild(structureBuild())
            .setContext(DoorFSMContext())
            .setStartState(States.OPEN)
            .build()

        doorFSM.autoTransition()
        assert(doorFSM.getState() == States.AJAR)
    }

    fun testBuilderFactory() {
        val doorFSM = eventsBuild(structureBuild())
            .setContext(DoorFSMContext())
            .setStartState(States.OPEN)
            .setFactory(typedDoorFactory)
            .build()


        testDoor(doorFSM)
        assert(doorFSM is TypesDoorStateMachine)
        if (doorFSM is TypesDoorStateMachine) typedTestDoor(doorFSM)
    }

    fun testJsonSchema() {
        val validDoorFSM = eventsBuild(structureBuild())
            .setContext(DoorFSMContext())
            .setStartState(States.OPEN)
            .build()

        testDoor(validDoorFSM)

        val jsonSchema = validDoorFSM.toJsonSchema()
        val doorFSM = eventsBuild(
            StaterStateMachineBuilder<States, DoorFSMContext>()
                .fromJsonSchema(jsonSchema) { States.valueOf(it) })
            .setContext(DoorFSMContext())
            .setFactory(typedDoorFactory)
            .build()
        assert(jsonSchema == doorFSM.toJsonSchema())

        testDoor(doorFSM)

        assert(doorFSM is TypesDoorStateMachine)
        if (doorFSM is TypesDoorStateMachine) typedTestDoor(doorFSM)
    }

    fun testStringGeneric() {
        val validDoorFSM = eventsBuild(structureBuild())
            .setContext(DoorFSMContext())
            .setStartState(States.OPEN)
            .build()
        testDoor(validDoorFSM)
        val jsonSchema = validDoorFSM.toJsonSchema()
        val stringDoorFSM = StaterStateMachineBuilder<String, DoorFSMContext>()
            .fromJsonSchema(jsonSchema) { it }
            .setContext(DoorFSMContext())
            .build()
        assert(stringDoorFSM.getState() == "OPEN")
    }

    fun testJsonDump() {
        class JsonConverter : ContextJsonAdapter<DoorFSMContext> {
            override fun toJson(context: DoorFSMContext) = context.degreeOfOpening.toString()
            override fun fromJson(json: String) = DoorFSMContext(degreeOfOpening = json.toInt())
        }

        val validDoorFSM = eventsBuild(structureBuild())
            .setContext(DoorFSMContext())
            .setStartState(States.OPEN)
            .setContextJsonAdapter(JsonConverter())
            .build()
        assert(validDoorFSM.getState() == States.OPEN)
        assert(validDoorFSM.getContext().degreeOfOpening == 100)
        val dump = validDoorFSM.toJson()
        validDoorFSM.transition("preClose")
        assert(validDoorFSM.getState() == States.AJAR)
        assert(validDoorFSM.getContext().degreeOfOpening == 99)
        validDoorFSM.fromJson(dump) { States.valueOf(it) }
        assert(validDoorFSM.getState() == States.OPEN)
        assert(validDoorFSM.getContext().degreeOfOpening == 100)
    }

    fun testMiddlewareAndCallbacks() {
        var transitionMiddleware = 0
        var transitionAllMiddleware = 0
        var subscribeOnTransition = 0
        var subscribeOnAllTransition = 0
        var subscribeOnState = 0
        var subscribeOnAllState = 0

        val validDoorFSM = eventsBuild(structureBuild())
            .setContext(DoorFSMContext())
            .setStartState(States.OPEN)
            .transitionMiddleware("open") { context, next -> transitionMiddleware++;next(context) }
            .transitionAllMiddleware { name, context, next -> transitionAllMiddleware++;next(name, context) }
            .subscribeOnTransition("open") { _ -> subscribeOnTransition++ }
            .subscribeOnAllTransition { _, _ -> subscribeOnAllTransition++ }
            .subscribeOnState(States.AJAR) { _ -> subscribeOnState++ }
            .subscribeOnAllState { _, _ -> subscribeOnAllState++ }
            .build()
        testDoor(validDoorFSM)
        assert(transitionMiddleware == 1)
        assert(transitionAllMiddleware == 200)
        assert(subscribeOnTransition == 1)
        assert(subscribeOnAllTransition == 200)
        assert(subscribeOnState == 198)
        assert(subscribeOnAllState == 200)
    }
}