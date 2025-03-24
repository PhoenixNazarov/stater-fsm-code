import org.example.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class TestDoor {
    enum class States {
        CLOSE, AJAR, OPEN
    }

    data class DoorFSMContext(
        var degreeOfOpening: Int = 100,
    ) : Context


    class TypesDoorStateMachine(
        transitions: List<Transition<States, DoorFSMContext>> = listOf(
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
        ),
        startState: States = States.OPEN,
        states: Set<States> = setOf(States.OPEN, States.CLOSE, States.AJAR),
        context: DoorFSMContext = DoorFSMContext(),
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
            contextA,
            startStateA,
            statesA,
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

    private fun testDoor(door: StaterStateMachine<States, DoorFSMContext>) {
        assertEquals(door.getState(), States.OPEN)
        assertEquals(door.getContext().degreeOfOpening, 100)
        door.transition("preClose")
        assertEquals(door.getState(), States.AJAR)
        assertEquals(door.getContext().degreeOfOpening, 99)
        while (door.getContext().degreeOfOpening > 1) {
            door.transition("ajarMinus")
            assertEquals(door.getState(), States.AJAR)
        }
        assertEquals(door.getContext().degreeOfOpening, 1)
        door.transition("close")
        assertEquals(door.getContext().degreeOfOpening, 0)
        assertEquals(door.getState(), States.CLOSE)
        door.transition("preOpen")
        assertEquals(door.getContext().degreeOfOpening, 1)
        assertEquals(door.getState(), States.AJAR)
        door.transition("ajarPlus")
        assertEquals(door.getState(), States.AJAR)
        assertEquals(door.getContext().degreeOfOpening, 2)
        while (door.getContext().degreeOfOpening < 99) {
            door.transition("ajarPlus")
            assertEquals(door.getState(), States.AJAR)
        }
        door.transition("open")
        assertEquals(door.getState(), States.OPEN)
        assertEquals(door.getContext().degreeOfOpening, 100)
    }

    private fun typedTestDoor(door: TypesDoorStateMachine) {
        assertEquals(door.getState(), States.OPEN)
        assertEquals(door.getContext().degreeOfOpening, 100)
        door.preClose()
        assertEquals(door.getState(), States.AJAR)
        assertEquals(door.getContext().degreeOfOpening, 99)
        while (door.getContext().degreeOfOpening > 1) {
            door.ajarMinus()
            assertEquals(door.getState(), States.AJAR)
        }
        assertEquals(door.getContext().degreeOfOpening, 1)
        door.close()
        assertEquals(door.getContext().degreeOfOpening, 0)
        assertEquals(door.getState(), States.CLOSE)
        door.preOpen()
        assertEquals(door.getContext().degreeOfOpening, 1)
        assertEquals(door.getState(), States.AJAR)
        door.ajarPlus()
        assertEquals(door.getState(), States.AJAR)
        assertEquals(door.getContext().degreeOfOpening, 2)
        while (door.getContext().degreeOfOpening < 99) {
            door.ajarPlus()
            assertEquals(door.getState(), States.AJAR)
        }
        door.open()
        assertEquals(door.getState(), States.OPEN)
        assertEquals(door.getContext().degreeOfOpening, 100)
    }

    @Test
    fun testSimpleBuild() {
        val doorFSM = TypesDoorStateMachine()
        testDoor(doorFSM)
        typedTestDoor(doorFSM)
    }

    @Test
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

    @Test
    fun testBuilder2() {
        val doorFSM = eventsBuild(structureBuild())
            .setContext(DoorFSMContext())
            .setStartState(States.OPEN)
            .build()

        testDoor(doorFSM)
    }

    @Test
    fun testAutoTransition() {
        val doorFSM = eventsBuild(structureBuild())
            .setContext(DoorFSMContext())
            .setStartState(States.OPEN)
            .build()

        doorFSM.autoTransition()
        assertEquals(doorFSM.getState(), States.AJAR)
    }

    @Test
    fun testBuilderFactory() {
        val doorFSM = eventsBuild(structureBuild())
            .setContext(DoorFSMContext())
            .setStartState(States.OPEN)
            .setFactory(typedDoorFactory)
            .build()


        testDoor(doorFSM)
        assertTrue { doorFSM is TypesDoorStateMachine }
        if (doorFSM is TypesDoorStateMachine) typedTestDoor(doorFSM)
    }

    @Test
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
        assertEquals(jsonSchema, doorFSM.toJsonSchema())

        testDoor(doorFSM)

        assertTrue { doorFSM is TypesDoorStateMachine }
        if (doorFSM is TypesDoorStateMachine) typedTestDoor(doorFSM)
    }

    @Test
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
        assertEquals(stringDoorFSM.getState(), "OPEN")
    }

    @Test
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
        assertEquals(validDoorFSM.getState(), States.OPEN)
        assertEquals(validDoorFSM.getContext().degreeOfOpening, 100)
        val dump = validDoorFSM.toJson()
        validDoorFSM.transition("preClose")
        assertEquals(validDoorFSM.getState(), States.AJAR)
        assertEquals(validDoorFSM.getContext().degreeOfOpening, 99)
        validDoorFSM.fromJson(dump) { States.valueOf(it) }
        assertEquals(validDoorFSM.getState(), States.OPEN)
        assertEquals(validDoorFSM.getContext().degreeOfOpening, 100)
    }

    @Test
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
        assertEquals(transitionMiddleware, 1)
        assertEquals(transitionAllMiddleware, 200)
        assertEquals(subscribeOnTransition, 1)
        assertEquals(subscribeOnAllTransition, 200)
        assertEquals(subscribeOnState, 198)
        assertEquals(subscribeOnAllState, 200)
    }
}