import org.example.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

enum class States {
    OPEN,
    AJAR,
    CLOSE
}

class TypesDoorStateMachine(
    transitions: List<Transition<States, EmptyContext>>,
    startState: States,
    states: Set<States>,
    context: EmptyContext,
    transitionMiddlewares: Map<String, List<TransitionMiddleware<EmptyContext>>> = mapOf(),
    transitionAllMiddlewares: List<TransitionNameMiddleware<EmptyContext>> = listOf(),
    transitionCallbacks: Map<String, List<Event<EmptyContext>>> = mapOf(),
    transitionAllCallbacks: List<NameEvent<EmptyContext>> = listOf(),
    stateCallbacks: Map<States, List<Event<EmptyContext>>> = mapOf(),
    stateAllCallbacks: List<StateEvent<States, EmptyContext>> = listOf(),
    contextJsonAdapter: ContextJsonAdapter<EmptyContext>? = null,
) : StaterStateMachine<States, EmptyContext>(
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

private val typedDoorFactory: StateMachineFactory<States, EmptyContext> = {
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
        context = contextA,
        startState = startStateA,
        states = statesA,
        transitionMiddlewares = transitionMiddlewaresA,
        transitionAllMiddlewares = transitionAllMiddlewaresA,
        transitionCallbacks = transitionCallbacksA,
        transitionAllCallbacks = transitionAllCallbacksA,
        stateCallbacks = stateCallbacksA,
        stateAllCallbacks = stateAllCallbacksA,
        contextJsonAdapter = contextJsonAdapterA
    )
}

val builderDoorStateMachine = StaterStateMachineBuilder<States, EmptyContext>()
    .setStartState(States.OPEN)
    .setContext(EmptyContext())
    .setFactory(typedDoorFactory)
    .addTransition("preOpen", States.CLOSE, States.AJAR)
    .addTransition("preClose", States.OPEN, States.AJAR)
    .addTransition("open", States.AJAR, States.OPEN)
    .addTransition("close", States.AJAR, States.CLOSE)
    .addTransition("ajarPlus", States.AJAR, States.AJAR)
    .addTransition("ajarMinus", States.AJAR, States.AJAR)

val eventBuilderDoorStateMachine = builderDoorStateMachine
//    .setTransitionCondition()

fun createStateMachine() = builderDoorStateMachine.build()

fun testScenario1(fsm: TypesDoorStateMachine) {
    fsm.ajarPlus()
    fsm.open()
}


