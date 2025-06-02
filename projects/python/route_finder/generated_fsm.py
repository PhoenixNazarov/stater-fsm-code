from enum import Enum

from route_finder.check_logic import check_ship, check_train, check_train_auto, check_ship_auto, collect
from route_finder.exceptions import RouteException
from route_finder.route_dto import Route
from stater_state_machine import StaterStateMachine, EmptyContext, StaterStateMachineBuilder


class States(Enum):
    CHECKED_AUTO = "CHECKED_AUTO"
    CHECKED_SHIP = "CHECKED_SHIP"
    CHECKED_TRAIN = "CHECKED_TRAIN"
    INITIAL = "INITIAL"
    VALID = "VALID"


class TypesValidatorStateMachine(StaterStateMachine[States, EmptyContext]):
    def checking_ship(self):
        self.transition("checking_ship")

    def checking_train(self):
        self.transition("checking_train")

    def train_checking_auto(self):
        self.transition("train_checking_auto")

    def ship_checking_auto(self):
        self.transition("ship_checking_auto")

    def collect(self):
        self.transition("collect")


def typed_validator_factory(*args, **kwargs):
    return TypesValidatorStateMachine(*args, **kwargs)


builder_validator_state_machine = (
    StaterStateMachineBuilder[States, EmptyContext]()
    .set_start_state(States.INITIAL)
    .set_factory(typed_validator_factory)
    .add_transition("checking_ship", States.INITIAL, States.CHECKED_SHIP)
    .add_transition("checking_train", States.CHECKED_SHIP, States.CHECKED_TRAIN)
    .add_transition("train_checking_auto", States.CHECKED_TRAIN, States.CHECKED_AUTO)
    .add_transition("ship_checking_auto", States.CHECKED_SHIP, States.CHECKED_AUTO)
    .add_transition("collect", States.CHECKED_AUTO, States.VALID)
    .set_transition_condition("checking_ship", check_ship)
    .set_transition_condition('checking_train', check_train)
    .set_transition_condition('train_checking_auto', check_train_auto)
    .set_transition_condition('ship_checking_auto', check_ship_auto)
    .set_transition_condition('collect', collect)
)


def fsm_processor(route: Route) -> Route | None:
    fsm = (
        builder_validator_state_machine
        .set_context(route)
        .build()
    )

    try:
        fsm.checking_ship()
        if fsm.get_context().train_id:
            fsm.checking_train()
            fsm.train_checking_auto()
        else:
            fsm.ship_checking_auto()
        fsm.collect()
        return fsm.get_context()
    except RouteException:
        return None
