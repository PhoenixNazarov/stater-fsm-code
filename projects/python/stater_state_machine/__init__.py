import json
from abc import ABC, abstractmethod
from typing import Callable, TypeVar, Generic, Optional, List, Dict, Set

from pydantic import BaseModel, Field

__all__ = [
    'Context',
    'EmptyContext',
    'StaterStateMachine',
    'Transition',
    'StaterStateMachineBuilder',
    'ContextJsonAdapter'
]


class Context:
    ...


class EmptyContext(Context):
    ...


T = TypeVar("T")
C = TypeVar("C", bound=Context)


class ContextJsonAdapter(Generic[C], ABC):
    @abstractmethod
    def to_json(self, context: C) -> str:
        ...

    @abstractmethod
    def from_json(self, json: str) -> C:
        ...


Event = Callable[[C], None]
NameEvent = Callable[[str, C], None]
StateEvent = Callable[[T, C], None]
TransitionMiddleware = Callable[[C, Event], None]
TransitionNameMiddleware = Callable[[str, C, NameEvent], None]


class Transition(BaseModel, Generic[T, C]):
    name: str
    start: T
    end: T
    condition: Optional[Callable[[C], bool]] = Field(None, exclude=True)
    event: Optional[Event[C]] = Field(None, exclude=True)


class JsonSchema(BaseModel, Generic[T]):
    states: List[T]
    startState: T
    transitions: List[Transition[T, C]]

    class Config:
        arbitrary_types_allowed = True


class JsonState(BaseModel, Generic[T]):
    state: T
    context: str


class StaterStateMachine(Generic[T, C]):
    def __init__(
            self,
            transitions: List[Transition[T, C]],
            context: C,
            start_state: Optional[T] = None,
            states: Optional[Set[T]] = None,
            transition_middlewares: Optional[Dict[str, List[Callable[[C, Callable], None]]]] = None,
            transition_all_middlewares: Optional[List[Callable[[str, C, Callable], None]]] = None,
            transition_callbacks: Optional[Dict[str, List[Callable[[C], None]]]] = None,
            transition_all_callbacks: Optional[List[Callable[[str, C], None]]] = None,
            state_callbacks: Optional[Dict[T, List[Callable[[C], None]]]] = None,
            state_all_callbacks: Optional[List[Callable[[T, C], None]]] = None,
            context_json_adapter: Optional['ContextJsonAdapter[C]'] = None,
            state: Optional[T] = None,
    ):
        self.__transitions = transitions
        if states is None:
            self.__states = set()
            for i in self.__transitions:
                self.__states.add(i.start)
                self.__states.add(i.end)
        else:
            self.__states = states
        self.__start_state = start_state or self.__transitions[0].start
        self.__state = state or start_state
        self.__context = context
        self.__transitions_grouped_start = {t.start: [] for t in transitions}
        for t in transitions:
            self.__transitions_grouped_start[t.start].append(t)
        self.__transitions_by_name = {t.name: t for t in transitions}
        self.__transition_middlewares = transition_middlewares or {}
        self.__transition_all_middlewares = transition_all_middlewares or []
        self.__transition_callbacks = transition_callbacks or {}
        self.__transition_all_callbacks = transition_all_callbacks or []
        self.__state_callbacks = state_callbacks or {}
        self.__state_all_callbacks = state_all_callbacks or []
        self.__context_json_adapter = context_json_adapter
        self.__enable_events = True

    def get_state(self) -> T:
        return self.__state

    def get_context(self) -> C:
        return self.__context

    def transition(self, name: str):
        transition = self.__transitions_by_name.get(name)
        if not transition:
            raise ValueError(f"Transition not found: {name}")

        if self.__state != transition.start:
            raise ValueError(f"Start state does not match transition's start state: {transition.start}")

        def condition_handler():
            if transition.condition and not transition.condition(self.__context):
                raise ValueError(f"Condition returned false for transition {name}")

        index = 0
        index2 = 0
        transition_middleware = self.__transition_middlewares.get(name)

        def internal_next(context: C):
            nonlocal index2
            if transition_middleware and index2 < len(transition_middleware):
                index2 += 1
                transition_middleware[index2 - 2](context, internal_next)
            else:
                return condition_handler()

        def next(name: str, context: C):
            nonlocal index
            if index < len(self.__transition_all_middlewares):
                index += 1
                self.__transition_all_middlewares[index - 1](name, context, next)
            else:
                return internal_next(context)

        if self.__enable_events:
            next(name, self.__context)

        self.__state = transition.end

        if not self.__enable_events:
            return
        if transition.event:
            transition.event(self.__context)

        for callback in self.__transition_all_callbacks:
            callback(name, self.__context)
        for callback in self.__transition_callbacks.get(name, []):
            callback(self.__context)
        for callback in self.__state_all_callbacks:
            callback(self.__state, self.__context)
        for callback in self.__state_callbacks.get(self.__state, []):
            callback(self.__context)

    def auto_transition(self):
        for transition in self.__transitions_grouped_start.get(self.__state, []):
            try:
                self.transition(transition.name)
                return
            except Exception:
                pass

    def to_json_schema(self) -> str:
        return JsonSchema(
            states=sorted(list(self.__states), key=lambda i: str(i).casefold()),
            startState=self.__start_state,
            transitions=self.__transitions
        ).json()

    def to_json(self) -> str:
        if not self.__context_json_adapter:
            raise ValueError("ContextJsonAdapter is not set")
        return JsonState(
            state=self.__state,
            context=self.__context_json_adapter.to_json(self.__context)
        ).json()

    def from_json(self, json_str: str, state_converter: Callable[[str], T]):
        if not self.__context_json_adapter:
            raise ValueError("ContextJsonAdapter is not set")
        data = JsonState.parse_raw(json_str)
        self.__state = state_converter(data.state)
        self.__context = self.__context_json_adapter.from_json(data.context)

    def disable_events(self):
        self.__enable_events = False

    def enable_events(self):
        self.__enable_events = True


class BaseFSM(Generic[T, C], StaterStateMachine[T, C]):
    ...


StateMachineFactory = Callable[[
    list[Transition[T, C]],
    C,
    T,
    Set[T],
    dict[str, list[TransitionMiddleware[C]]],
    list[TransitionNameMiddleware[C]],
    dict[str, list[Event[C]]],
    list[NameEvent[C]],
    dict[T, list[Event[C]]],
    list[StateEvent[T, C]],
    Optional[ContextJsonAdapter[C]]
], StaterStateMachine[T, C]]


class StaterStateMachineBuilder(Generic[T, C]):
    def __init__(self):
        self.__transitions: Dict[str, Transition[T, C]] = {}
        self.__state: Optional[T] = None
        self.__states: Set[T] = set()
        self.__context: Optional[C] = None
        self.__transition_middlewares: dict[str, list[TransitionMiddleware[C]]] = {}
        self.__transition_all_middlewares: list[TransitionNameMiddleware[C]] = []
        self.__transition_callbacks: dict[str, list[Event[C]]] = {}
        self.__transition_all_callbacks: list[NameEvent[C]] = []
        self.__state_callbacks: dict[T, list[Event[C]]] = {}
        self.__state_all_callbacks: list[StateEvent[T, C]] = []

        def default_factory(
                transitions_a,
                context_a,
                start_state_a,
                states_a,
                transition_middlewares_a,
                transition_all_middlewares_a,
                transition_callbacks_a,
                transition_all_callbacks_a,
                state_callbacks_a,
                state_all_callbacks_a,
                context_json_adapter_a
        ):
            return BaseFSM(
                transitions=transitions_a,
                context=context_a,
                start_state=start_state_a,
                states=states_a,
                transition_middlewares=transition_middlewares_a,
                transition_all_middlewares=transition_all_middlewares_a,
                transition_callbacks=transition_callbacks_a,
                transition_all_callbacks=transition_all_callbacks_a,
                state_callbacks=state_callbacks_a,
                state_all_callbacks=state_all_callbacks_a,
                context_json_adapter=context_json_adapter_a
            )

        self.__factory: StateMachineFactory[T, C] = default_factory
        self.__context_json_adapter: Optional[ContextJsonAdapter[C]] = None

    def add_transition(self, name: str, start: T, end: T, condition: Optional[Callable[[C], bool]] = None,
                       event: Optional[Callable[[C], None]] = None):
        self.add_state(start)
        self.add_state(end)
        self.__transitions[name] = Transition(name=name, start=start, end=end, condition=condition, event=event)
        return self

    def add_state(self, state: T):
        if state not in self.__states:
            self.__states.add(state)
        return self

    def set_transition_condition(self, name: str, condition: Callable[[C], bool]):
        if name not in self.__transitions:
            raise ValueError(f"Transition not found: {name}")
        self.__transitions[name].condition = condition
        return self

    def set_transition_event(self, name: str, event: Callable[[C], None]):
        if name not in self.__transitions:
            raise ValueError(f"Transition not found: {name}")
        self.__transitions[name].event = event
        return self

    def transition_middleware(self, name: str, middleware: Callable[[C, Callable], None]):
        self.__transition_middlewares.setdefault(name, []).append(middleware)
        return self

    def transition_all_middleware(self, middleware: Callable[[str, C, Callable], None]):
        self.__transition_all_middlewares.append(middleware)
        return self

    def subscribe_on_transition(self, name: str, callback: Callable[[C], None]):
        self.__transition_callbacks.setdefault(name, []).append(callback)
        return self

    def subscribe_on_all_transition(self, callback: Callable[[str, C], None]):
        self.__transition_all_callbacks.append(callback)
        return self

    def subscribe_on_state(self, state: T, callback: Callable[[C], None]):
        self.__state_callbacks.setdefault(state, []).append(callback)
        return self

    def subscribe_on_all_state(self, callback: Callable[[T, C], None]):
        self.__state_all_callbacks.append(callback)
        return self

    def set_start_state(self, state: T):
        self.__state = state
        return self

    def set_context(self, context: C):
        self.__context = context
        return self

    def from_json_schema(self, schema: str, state_converter: Callable[[str], T]):
        data = json.loads(schema)
        for state in data['states']:
            self.add_state(state_converter(state))
        for transition in data['transitions']:
            self.add_transition(
                transition['name'],
                state_converter(transition['start']),
                state_converter(transition['end'])
            )
        self.set_start_state(state_converter(data['startState']))
        return self

    def set_factory(self, factory: Callable[..., 'StaterStateMachine[T, C]']):
        self.__factory = factory
        return self

    def set_context_json_adapter(self, context_json_adapter: 'ContextJsonAdapter[C]'):
        self.__context_json_adapter = context_json_adapter
        return self

    def build(self) -> StaterStateMachine[T, C]:
        state = self.__state
        if state is None:
            state = list(self.__transitions.values())[0].start
        if self.__context is None:
            raise ValueError("Context must be set")
        return self.__factory(
            list(self.__transitions.values()),
            self.__context,
            state,
            self.__states,
            self.__transition_middlewares,
            self.__transition_all_middlewares,
            self.__transition_callbacks,
            self.__transition_all_callbacks,
            self.__state_callbacks,
            self.__state_all_callbacks,
            self.__context_json_adapter
        )
