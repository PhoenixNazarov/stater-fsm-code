from enum import Enum

from pydantic import BaseModel

from stater_state_machine import Context, StaterStateMachine, Transition, StaterStateMachineBuilder, ContextJsonAdapter


class States(Enum):
    CLOSE = "CLOSE"
    AJAR = "AJAR"
    OPEN = "OPEN"


class DoorFSMContext(BaseModel, Context):
    degree_of_opening: int = 100


class TypesDoorStateMachine(StaterStateMachine[States, DoorFSMContext]):
    def ajarPlus(self):
        self.transition("ajarPlus")

    def ajarMinus(self):
        self.transition("ajarMinus")

    def preOpen(self):
        self.transition("preOpen")

    def preClose(self):
        self.transition("preClose")

    def open(self):
        self.transition("open")

    def close(self):
        self.transition("close")


def typed_door_factory(*args, **kwargs):
    return TypesDoorStateMachine(*args, **kwargs)


def _test_door(door: StaterStateMachine[States, DoorFSMContext]):
    assert door.get_state() == States.OPEN
    assert door.get_context().degree_of_opening == 100
    door.transition("preClose")
    assert door.get_state() == States.AJAR
    assert door.get_context().degree_of_opening == 99
    while door.get_context().degree_of_opening > 1:
        door.transition("ajarMinus")
        assert door.get_state() == States.AJAR
    assert door.get_context().degree_of_opening == 1
    door.transition("close")
    assert door.get_context().degree_of_opening == 0
    assert door.get_state() == States.CLOSE
    door.transition("preOpen")
    assert door.get_context().degree_of_opening == 1
    assert door.get_state() == States.AJAR
    door.transition("ajarPlus")
    assert door.get_state() == States.AJAR
    assert door.get_context().degree_of_opening == 2
    while door.get_context().degree_of_opening < 99:
        door.transition("ajarPlus")
        assert door.get_state() == States.AJAR
    door.transition("open")
    assert door.get_state() == States.OPEN
    assert door.get_context().degree_of_opening == 100


def typed_test_door(door: TypesDoorStateMachine):
    assert door.get_state() == States.OPEN
    assert door.get_context().degree_of_opening == 100
    door.preClose()
    assert door.get_state() == States.AJAR
    assert door.get_context().degree_of_opening == 99
    while door.get_context().degree_of_opening > 1:
        door.ajarMinus()
        assert door.get_state() == States.AJAR
    assert door.get_context().degree_of_opening == 1
    door.close()
    assert door.get_context().degree_of_opening == 0
    assert door.get_state() == States.CLOSE
    door.preOpen()
    assert door.get_context().degree_of_opening == 1
    assert door.get_state() == States.AJAR
    door.ajarPlus()
    assert door.get_state() == States.AJAR
    assert door.get_context().degree_of_opening == 2
    while door.get_context().degree_of_opening < 99:
        door.ajarPlus()
        assert door.get_state() == States.AJAR
    door.open()
    assert door.get_state() == States.OPEN
    assert door.get_context().degree_of_opening == 100


def test_simple_build():
    door_fsm = TypesDoorStateMachine(
        start_state=States.OPEN,
        states=[States.OPEN, States.CLOSE, States.AJAR],
        context=DoorFSMContext(),
        transitions=[
            Transition(name="preOpen", start=States.CLOSE, end=States.AJAR,
                       event=lambda ctx: setattr(ctx, 'degree_of_opening', 1)),
            Transition(name="preClose", start=States.OPEN, end=States.AJAR,
                       event=lambda ctx: setattr(ctx, 'degree_of_opening', 99)),
            Transition(name="open", start=States.AJAR, end=States.OPEN,
                       condition=lambda ctx: ctx.degree_of_opening >= 99,
                       event=lambda ctx: setattr(ctx, 'degree_of_opening', 100)),
            Transition(name="close", start=States.AJAR, end=States.CLOSE,
                       condition=lambda ctx: ctx.degree_of_opening <= 1,
                       event=lambda ctx: setattr(ctx, 'degree_of_opening', 0)),
            Transition(name="ajarPlus", start=States.AJAR, end=States.AJAR,
                       condition=lambda ctx: 1 <= ctx.degree_of_opening <= 98,
                       event=lambda ctx: setattr(ctx, 'degree_of_opening', ctx.degree_of_opening + 1)),
            Transition(name="ajarMinus", start=States.AJAR, end=States.AJAR,
                       condition=lambda ctx: 2 <= ctx.degree_of_opening <= 99,
                       event=lambda ctx: setattr(ctx, 'degree_of_opening', ctx.degree_of_opening - 1)),
        ]
    )
    _test_door(door_fsm)
    typed_test_door(door_fsm)


def test_builder():
    door_fsm = (
        StaterStateMachineBuilder[States, DoorFSMContext]()
        .add_transition('preOpen', States.CLOSE, States.AJAR, event=lambda ctx: setattr(ctx, 'degree_of_opening', 1))
        .add_transition('preClose', States.OPEN, States.AJAR, event=lambda ctx: setattr(ctx, 'degree_of_opening', 99))
        .add_transition('open', States.AJAR, States.OPEN, lambda ctx: ctx.degree_of_opening >= 99,
                        lambda ctx: setattr(ctx, 'degree_of_opening', 100))
        .add_transition('close', States.AJAR, States.CLOSE, lambda ctx: ctx.degree_of_opening <= 1,
                        lambda ctx: setattr(ctx, 'degree_of_opening', 0))
        .add_transition('ajarPlus', States.AJAR, States.AJAR, lambda ctx: 1 <= ctx.degree_of_opening <= 98,
                        lambda ctx: setattr(ctx, 'degree_of_opening', ctx.degree_of_opening + 1))
        .add_transition('ajarMinus', States.AJAR, States.AJAR, lambda ctx: 2 <= ctx.degree_of_opening <= 99,
                        lambda ctx: setattr(ctx, 'degree_of_opening', ctx.degree_of_opening - 1))
        .set_context(DoorFSMContext())
        .set_start_state(States.OPEN)
        .build()
    )
    _test_door(door_fsm)


def structure_build():
    return (
        StaterStateMachineBuilder[States, DoorFSMContext]()
        .add_transition("preOpen", States.CLOSE, States.AJAR)
        .add_transition("preClose", States.OPEN, States.AJAR)
        .add_transition("open", States.AJAR, States.OPEN)
        .add_transition("close", States.AJAR, States.CLOSE)
        .add_transition("ajarPlus", States.AJAR, States.AJAR)
        .add_transition("ajarMinus", States.AJAR, States.AJAR)
    )


def events_build(builder: StaterStateMachineBuilder[States, DoorFSMContext]):
    return (
        builder
        .set_transition_event("preOpen", lambda ctx: setattr(ctx, 'degree_of_opening', 1))
        .set_transition_event("preClose", lambda ctx: setattr(ctx, 'degree_of_opening', 99))
        .set_transition_condition("open", lambda ctx: ctx.degree_of_opening >= 99)
        .set_transition_event("open", lambda ctx: setattr(ctx, 'degree_of_opening', 100))
        .set_transition_condition("close", lambda ctx: ctx.degree_of_opening <= 1)
        .set_transition_event("close", lambda ctx: setattr(ctx, 'degree_of_opening', 0))
        .set_transition_condition("ajarPlus", lambda ctx: 1 <= ctx.degree_of_opening <= 98)
        .set_transition_event("ajarPlus", lambda ctx: setattr(ctx, 'degree_of_opening', ctx.degree_of_opening + 1))
        .set_transition_condition("ajarMinus", lambda ctx: 2 <= ctx.degree_of_opening <= 99)
        .set_transition_event("ajarMinus", lambda ctx: setattr(ctx, 'degree_of_opening', ctx.degree_of_opening - 1))
    )


def test_builder_2():
    door_fsm = (
        events_build(structure_build())
        .set_context(DoorFSMContext())
        .set_start_state(States.OPEN)
        .build()
    )
    _test_door(door_fsm)


def test_auto_transition():
    door_fsm = (
        events_build(structure_build())
        .set_context(DoorFSMContext())
        .set_start_state(States.OPEN)
        .build()
    )

    door_fsm.auto_transition()
    assert (door_fsm.get_state() == States.AJAR)


def test_builder_factory():
    door_fsm = (
        events_build(structure_build())
        .set_context(DoorFSMContext())
        .set_start_state(States.OPEN)
        .set_factory(typed_door_factory)
        .build()
    )

    _test_door(door_fsm)
    assert isinstance(door_fsm, TypesDoorStateMachine)
    if isinstance(door_fsm, TypesDoorStateMachine):
        typed_test_door(door_fsm)


def test_json_schema():
    valid_door_fsm = (
        events_build(structure_build())
        .set_context(DoorFSMContext())
        .set_start_state(States.OPEN)
        .build()
    )

    _test_door(valid_door_fsm)
    json_schema = valid_door_fsm.to_json_schema()

    door_fsm = (
        events_build(
            StaterStateMachineBuilder[States, DoorFSMContext]()
            .from_json_schema(json_schema, lambda i: States(i)))
        .set_context(DoorFSMContext())
        .set_factory(typed_door_factory)
        .build()
    )
    print(door_fsm)

    assert json_schema == door_fsm.to_json_schema()

    _test_door(door_fsm)

    assert isinstance(door_fsm, TypesDoorStateMachine)
    if isinstance(door_fsm, TypesDoorStateMachine):
        typed_test_door(door_fsm)


def test_string_generic():
    valid_door_fsm = (
        events_build(structure_build())
        .set_context(DoorFSMContext())
        .set_start_state(States.OPEN)
        .build()
    )
    _test_door(valid_door_fsm)
    json_schema = valid_door_fsm.to_json_schema()
    string_door_fsm = (
        StaterStateMachineBuilder[str, DoorFSMContext]()
        .from_json_schema(json_schema, lambda i: i)
        .set_context(DoorFSMContext())
        .build()
    )
    assert (string_door_fsm.get_state() == "OPEN")


def test_json_dump():
    class JsonConverter(ContextJsonAdapter[DoorFSMContext]):
        def to_json(self, context: DoorFSMContext):
            return context.degree_of_opening

        def from_json(self, json: str):
            return DoorFSMContext(degree_of_opening=int(json))

    valid_door_fsm = (
        events_build(structure_build())
        .set_context(DoorFSMContext())
        .set_start_state(States.OPEN)
        .set_context_json_adapter(JsonConverter())
        .build()
    )

    assert (valid_door_fsm.get_state() == States.OPEN)
    assert (valid_door_fsm.get_context().degree_of_opening == 100)
    dump = valid_door_fsm.to_json()
    valid_door_fsm.transition("preClose")
    assert (valid_door_fsm.get_state() == States.AJAR)
    assert (valid_door_fsm.get_context().degree_of_opening == 99)
    valid_door_fsm.from_json(dump, lambda i: States(i))
    assert (valid_door_fsm.get_state() == States.OPEN)
    assert (valid_door_fsm.get_context().degree_of_opening == 100)


def test_middleware_and_callbacks():
    test = {
        'transition_middleware': 0,
        'transition_all_middleware': 0,
        'subscribe_on_transition': 0,
        'subscribe_on_all_transition': 0,
        'subscribe_on_state': 0,
        'subscribe_on_all_state': 0
    }

    def transition_middleware_f(context, next):
        test['transition_middleware'] += 1
        next(context)

    def transition_all_middleware_f(name, context, next):
        test['transition_all_middleware'] += 1
        next(name, context)

    def subscribe_on_transition_f(*args, **kwargs):
        test['subscribe_on_transition'] += 1

    def subscribe_on_all_transition_f(*args, **kwargs):
        test['subscribe_on_all_transition'] += 1

    def subscribe_on_state_f(*args, **kwargs):
        test['subscribe_on_state'] += 1

    def subscribe_on_all_state_f(*args, **kwargs):
        test['subscribe_on_all_state'] += 1

    valid_door_fsm = (
        events_build(structure_build())
        .set_context(DoorFSMContext())
        .set_start_state(States.OPEN)
        .transition_middleware("open", transition_middleware_f)
        .transition_all_middleware(transition_all_middleware_f)
        .subscribe_on_transition("open", subscribe_on_transition_f)
        .subscribe_on_all_transition(subscribe_on_all_transition_f)
        .subscribe_on_state(States.AJAR, subscribe_on_state_f)
        .subscribe_on_all_state(subscribe_on_all_state_f)
        .build()
    )

    _test_door(valid_door_fsm)
    assert test['transition_middleware'] == 1
    assert test['transition_all_middleware'] == 200
    assert test['subscribe_on_transition'] == 1
    assert test['subscribe_on_all_transition'] == 200
    assert test['subscribe_on_state'] == 198
    assert test['subscribe_on_all_state'] == 200
