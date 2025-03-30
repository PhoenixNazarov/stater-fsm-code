package com.stater.statemachine.kotlin

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.text.Collator
import java.util.*


interface Context

class EmptyContext : Context

interface ContextJsonAdapter<C : Context> {
    fun toJson(context: C): String
    fun fromJson(json: String): C
}

typealias Event<C> = (context: C) -> Unit
typealias NameEvent<C> = (name: String, context: C) -> Unit
typealias StateEvent<T, C> = (state: T, context: C) -> Unit
typealias TransitionMiddleware<C> = (C, Event<C>) -> Unit
typealias TransitionNameMiddleware<C> = (String, C, NameEvent<C>) -> Unit
typealias StateMachineFactory<T, C> = (
    transitions: List<Transition<T, C>>,
    context: C,
    state: T,
    states: Set<T>,
    transitionMiddlewares: Map<String, List<TransitionMiddleware<C>>>,
    transitionAllMiddlewares: List<TransitionNameMiddleware<C>>,
    transitionCallbacks: Map<String, List<Event<C>>>,
    transitionAllCallbacks: List<NameEvent<C>>,
    stateCallbacks: Map<T, List<Event<C>>>,
    stateAllCallbacks: List<StateEvent<T, C>>,
    contextJsonAdapter: ContextJsonAdapter<C>?,
) -> StaterStateMachine<T, C>

data class Transition<T, C : Context>(
    val name: String,
    val start: T,
    val end: T,
    @JsonIgnore val condition: ((C) -> Boolean)? = null,
    @JsonIgnore val event: Event<C>? = null,
)

data class JsonSchema<T>(
    val states: List<T>,
    val startState: T,
    val transitions: List<Transition<T, *>>,
)

data class JsonState<T>(
    val state: T,
    val context: String,
)

abstract class StaterStateMachine<T, C : Context>(
    private val transitions: List<Transition<T, C>>,
    private var context: C,
    private var startState: T = transitions[0].start,
    private val states: Set<T> = transitions.map { it.start }.toSet() + transitions.map { it.end }.toSet(),
    private var state: T = startState,
    private val transitionsGroupedStart: Map<T, List<Transition<T, C>>> = transitions.groupBy { it.start },
    private val transitionsByName: Map<String, Transition<T, C>> = transitions.associateBy { it.name },

    private val transitionMiddlewares: Map<String, List<TransitionMiddleware<C>>> = mapOf(),
    private val transitionAllMiddlewares: List<TransitionNameMiddleware<C>> = listOf(),

    private val transitionCallbacks: Map<String, List<Event<C>>> = mapOf(),
    private val transitionAllCallbacks: List<NameEvent<C>> = listOf(),

    private val stateCallbacks: Map<T, List<Event<C>>> = mapOf(),
    private val stateAllCallbacks: List<StateEvent<T, C>> = listOf(),
    private var contextJsonAdapter: ContextJsonAdapter<C>? = null,
    private var enableEvents_: Boolean = true,
) {
    fun getState(): T = state
    fun getContext(): C = context

    fun transition(name: String) {
        val transition = transitionsByName[name] ?: error("Transition not found: $name")

        if (state != transition.start) {
            error("Start state does not match transition's start state: ${transition.start}")
        }

        fun conditionHandler() {
            transition.condition?.let {
                if (!it(context)) {
                    error("Condition return false for transition $name")
                }
            }
        }

        var index = 0
        var index2 = 0
        val transitionMiddleware = transitionMiddlewares[name]

        fun internalNext(context: C) {
            return if (transitionMiddleware != null && index2 < transitionMiddleware.size) {
                transitionMiddleware[index2++](context, ::internalNext)
            } else {
                conditionHandler()
            }
        }

        fun next(name: String, context: C) {
            return if (index < transitionAllMiddlewares.size) {
                transitionAllMiddlewares[index++](name, context, ::next)
            } else {
                internalNext(context)
            }
        }

        if (enableEvents_)
            next(name, context)

        state = transition.end
        if (!enableEvents_) return
        transition.event?.let { it(context) }

        transitionAllCallbacks.forEach { it(name, context) }
        transitionCallbacks[name]?.forEach { it(context) }
        stateAllCallbacks.forEach { it(state, context) }
        stateCallbacks[state]?.forEach { it(context) }
    }

    fun autoTransition() {
        (transitionsGroupedStart[state] ?: emptyList()).forEach {
            try {
                transition(it.name)
                return@forEach
            } catch (_: Exception) {
            }
        }
    }

    fun toJsonSchema(): String {
        val collator = Collator.getInstance(Locale.getDefault()).apply {
            strength = Collator.PRIMARY
        }
        return jacksonObjectMapper().writeValueAsString(
            JsonSchema(
                states = states.toList().sortedWith { a, b -> collator.compare(a.toString(), b.toString()) },
                startState = startState,
                transitions = transitions
            )
        ) ?: error("Cant serialize")
    }

    fun toJson() = jacksonObjectMapper().writeValueAsString(
        JsonState(
            state = state, context = contextJsonAdapter!!.toJson(context)
        )
    ) ?: error("Cant serialize")

    fun fromJson(json: String, stateConverter: (String) -> T) =
        jacksonObjectMapper().readValue<JsonState<T>>(json).let {
            this.state = stateConverter(it.state.toString())
            this.context = contextJsonAdapter!!.fromJson(it.context)
        }

    fun enableEvents() {
        enableEvents_ = true
    }

    fun disableEvents() {
        enableEvents_ = false
    }
}


class BaseFSM<T, C : Context>(
    transitions: List<Transition<T, C>>,
    context: C,
    startState: T,
    states: Set<T>,
    transitionMiddlewares: Map<String, List<TransitionMiddleware<C>>>,
    transitionAllMiddlewares: List<TransitionNameMiddleware<C>>,
    transitionCallbacks: Map<String, List<Event<C>>>,
    transitionAllCallbacks: List<NameEvent<C>>,
    stateCallbacks: Map<T, List<Event<C>>>,
    stateAllCallbacks: List<StateEvent<T, C>>,
    contextJsonAdapter: ContextJsonAdapter<C>?,
) : StaterStateMachine<T, C>(
    transitions = transitions,
    context = context,
    startState = startState,
    states = states,
    transitionMiddlewares = transitionMiddlewares,
    transitionAllMiddlewares = transitionAllMiddlewares,
    transitionCallbacks = transitionCallbacks,
    transitionAllCallbacks = transitionAllCallbacks,
    stateCallbacks = stateCallbacks,
    stateAllCallbacks = stateAllCallbacks,
    contextJsonAdapter = contextJsonAdapter
)


class StaterStateMachineBuilder<T, C : Context>(
    private val transitions: MutableMap<String, Transition<T, C>> = mutableMapOf(),
    private var state: T? = null,
    private var states: MutableSet<T> = mutableSetOf(),
    private var context: C? = null,

    private val transitionMiddlewares: MutableMap<String, MutableList<TransitionMiddleware<C>>> = mutableMapOf(),
    private val transitionAllMiddlewares: MutableList<TransitionNameMiddleware<C>> = mutableListOf(),

    private val transitionCallbacks: MutableMap<String, MutableList<Event<C>>> = mutableMapOf(),
    private val transitionAllCallbacks: MutableList<NameEvent<C>> = mutableListOf(),

    private val stateCallbacks: MutableMap<T, MutableList<Event<C>>> = mutableMapOf(),
    private val stateAllCallbacks: MutableList<StateEvent<T, C>> = mutableListOf(),
    private var factory: StateMachineFactory<T, C> = {
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
        BaseFSM(
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
    },
    private var contextJsonAdapter: ContextJsonAdapter<C>? = null,
) {
    fun addTransition(
        name: String,
        start: T,
        end: T,
        condition: ((C) -> Boolean)? = null,
        event: ((C) -> Unit)? = null,
    ) = apply {
        addState(start)
        addState(end)
        transitions[name] = Transition(name, start, end, condition, event)
    }

    fun addState(state: T) = apply {
        if (!states.contains(state)) {
            states.add(state)
        }
    }

    fun setTransitionCondition(name: String, condition: ((C) -> Boolean)) = apply {
        val transition = transitions[name] ?: error("Transition not found: $name")
        transitions[name] = transition.copy(condition = condition)
    }

    fun setTransitionEvent(name: String, event: ((C) -> Unit)) = apply {
        val transition = transitions[name] ?: error("Transition not found: $name")
        transitions[name] = transition.copy(event = event)
    }

    fun transitionMiddleware(name: String, middleware: TransitionMiddleware<C>) = apply {
        val middlewares = transitionMiddlewares.getOrDefault(name, mutableListOf())
        middlewares.add(middleware)
        transitionMiddlewares[name] = middlewares
    }

    fun transitionAllMiddleware(middleware: TransitionNameMiddleware<C>) = apply {
        transitionAllMiddlewares.add(middleware)
    }

    fun subscribeOnTransition(name: String, callback: Event<C>) = apply {
        val callbacks = transitionCallbacks.getOrDefault(name, mutableListOf())
        callbacks.add(callback)
        transitionCallbacks[name] = callbacks
    }

    fun subscribeOnAllTransition(callback: NameEvent<C>) = apply { transitionAllCallbacks.add(callback) }

    fun subscribeOnState(state: T, callback: Event<C>) = apply {
        val callbacks = stateCallbacks.getOrDefault(state, mutableListOf())
        callbacks.add(callback)
        stateCallbacks[state] = callbacks
    }

    fun subscribeOnAllState(callback: StateEvent<T, C>) = apply { stateAllCallbacks.add(callback) }

    fun setStartState(state: T) = apply { this.state = state }
    fun setContext(context: C) = apply { this.context = context }

    fun fromJsonSchema(schema: String, stateConverter: (String) -> T) = apply {
        jacksonObjectMapper().readValue<JsonSchema<T>>(schema).let {
            it.states.forEach { it2 -> addState(stateConverter(it2.toString())) }
            it.transitions.forEach { it2 ->
                addTransition(
                    it2.name,
                    stateConverter(it2.start.toString()),
                    stateConverter(it2.end.toString())
                )
            }
            setStartState(stateConverter(it.startState.toString()))
        }
    }

    fun setFactory(factory: StateMachineFactory<T, C>) = apply { this.factory = factory }

    fun setContextJsonAdapter(contextJsonAdapter: ContextJsonAdapter<C>) = apply {
        this.contextJsonAdapter = contextJsonAdapter
    }

    fun build(): StaterStateMachine<T, C> {
        return factory(
            transitions.values.toList(),
            context ?: error("Context must be set"),
            state ?: transitions.values.toList()[0].start,
            states,
            transitionMiddlewares,
            transitionAllMiddlewares,
            transitionCallbacks,
            transitionAllCallbacks,
            stateCallbacks,
            stateAllCallbacks,
            contextJsonAdapter
        )
    }
}