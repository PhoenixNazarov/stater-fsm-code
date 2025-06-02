import json
import sys

import pytest

from route_finder.generated_fsm import builder_validator_state_machine, States

pytestmark = pytest.mark.skipif(
    sys.version_info <= (3, 10),
    reason="Не работает на Python 3.10-"
)


def test_init():
    builder_validator_state_machine.build()


def test_scenario_0():
    sm = builder_validator_state_machine.build()
    sm.disable_events()


    sm.checking_ship()
    assert sm.get_state() == States.CHECKED_SHIP

    sm.checking_train()
    assert sm.get_state() == States.CHECKED_TRAIN

    sm.train_checking_auto()
    assert sm.get_state() == States.CHECKED_AUTO

    sm.collect()
    assert sm.get_state() == States.VALID


def test_scenario_1():
    sm = builder_validator_state_machine.build()
    sm.disable_events()


    sm.checking_ship()
    assert sm.get_state() == States.CHECKED_SHIP

    sm.ship_checking_auto()
    assert sm.get_state() == States.CHECKED_AUTO

    sm.collect()
    assert sm.get_state() == States.VALID


def test_json_schema():
    sm = builder_validator_state_machine.build()
    assert json.loads("""{
  "states": [
    "CHECKED_AUTO",
    "CHECKED_SHIP",
    "CHECKED_TRAIN",
    "INITIAL",
    "VALID"
  ],
  "startState": "INITIAL",
  "transitions": [
    {
      "name": "checking_ship",
      "start": "INITIAL",
      "end": "CHECKED_SHIP"
    },
    {
      "name": "checking_train",
      "start": "CHECKED_SHIP",
      "end": "CHECKED_TRAIN"
    },
    {
      "name": "train_checking_auto",
      "start": "CHECKED_TRAIN",
      "end": "CHECKED_AUTO"
    },
    {
      "name": "ship_checking_auto",
      "start": "CHECKED_SHIP",
      "end": "CHECKED_AUTO"
    },
    {
      "name": "collect",
      "start": "CHECKED_AUTO",
      "end": "VALID"
    }
  ]
}""") == json.loads(sm.to_json_schema())


