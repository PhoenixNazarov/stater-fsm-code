# mypy: ignore-errors
import datetime
import sys

import pytest

from route_finder.database.dto import Port, Country, City, ShipRate, AutoRate, TrainRate
from route_finder.database.services import rate_service, map_service
from route_finder.generated_fsm import fsm_processor
from route_finder.route_dto import Route

pytestmark = pytest.mark.skipif(
    sys.version_info <= (3, 10),
    reason="Не работает на Python 3.10-"
)

country_ru = Country(
    name='россия',
    commission=0
)
map_service.add_country(country_ru)

country_ch = Country(
    name='китай',
    commission=20
)
map_service.add_country(country_ch)

city_vladivostok = City(
    name='владивосток',
    commission=1,
    country_name=country_ru.name
)
map_service.add_city(city_vladivostok)

city_krasnoyarsk = City(
    name='красноярск',
    commission=2,
    country_name=country_ru.name
)
map_service.add_city(city_krasnoyarsk)

city_kazan = City(
    name='казань',
    commission=3,
    country_name=country_ru.name
)
map_service.add_city(city_kazan)

city_disable = City(
    name='disable',
    commission=0,
    country_name=country_ru.name,
    disable=True
)
map_service.add_city(city_disable)

city_shanghai = City(
    name='шанхай',
    commission=4,
    country_name=country_ch.name
)
map_service.add_city(city_shanghai)

port_vladivostok = Port(
    name='владивосток',
    commission=10,
    disable=False,
    city_name=city_vladivostok.name
)
map_service.add_port(port_vladivostok)

station_krasnoyarsk = Port(
    name='красноясрк',
    commission=10,
    disable=False,
    city_name=city_krasnoyarsk.name
)
map_service.add_station(station_krasnoyarsk)

port_shanghai = Port(
    name='шанхай',
    commission=11,
    disable=False,
    city_name=city_shanghai.name
)
map_service.add_port(port_shanghai)

ship_rate_1 = ShipRate(
    id=1,
    start_date=datetime.datetime(2025, 5, 1),
    expiration_date=datetime.datetime(2025, 5, 10),
    shipping_days=10,

    port_from=port_shanghai.name,
    port_to=port_vladivostok.name,
    cost=3333,
    disable=False
)
rate_service.add_ship_rate(ship_rate_1)

train_rate_1 = TrainRate(
    id=1,
    start_date=datetime.datetime(2025, 5, 10),
    expiration_date=datetime.datetime(2025, 5, 20),
    shipping_days=3,

    port_from=port_vladivostok.name,
    station_to=station_krasnoyarsk.name,
    cost=4444,
    disable=False
)
rate_service.add_train_rate(train_rate_1)

train_rate_2 = TrainRate(
    id=2,
    start_date=datetime.datetime(2025, 5, 20),
    expiration_date=datetime.datetime(2025, 5, 30),
    shipping_days=3,

    port_from=port_vladivostok.name,
    station_to=station_krasnoyarsk.name,
    cost=4444,
    disable=False
)
rate_service.add_train_rate(train_rate_2)

auto_rate_1 = AutoRate(
    id=1,
    start_date=datetime.datetime(2025, 5, 10),
    expiration_date=datetime.datetime(2025, 5, 30),
    shipping_days=2,

    city_from=city_krasnoyarsk.name,
    city_to=city_kazan.name,
    cost=5555,
    disable=False
)
rate_service.add_auto_rate(auto_rate_1)
auto_rate_2 = AutoRate(
    id=2,
    start_date=datetime.datetime(2025, 5, 10),
    expiration_date=datetime.datetime(2025, 5, 30),
    shipping_days=2,

    city_from=city_vladivostok.name,
    city_to=city_kazan.name,
    cost=5554,
    disable=False
)
rate_service.add_auto_rate(auto_rate_2)

auto_rate_3 = AutoRate(
    id=3,
    start_date=datetime.datetime(2025, 5, 10),
    expiration_date=datetime.datetime(2025, 5, 30),
    shipping_days=2,

    city_from=city_vladivostok.name,
    city_to=city_disable.name,
    cost=5558,
    disable=False
)
rate_service.add_auto_rate(auto_rate_3)


def test_case_1():
    """ ship + train + auto """
    route = Route(
        date=datetime.date(2025, 5, 6),
        port_from=port_shanghai.name,
        city_to=city_kazan.name,
        ship_id=ship_rate_1.id,
        train_id=train_rate_1.id,
        auto_id=auto_rate_1.id
    )
    result = fsm_processor(route)
    assert result is not None
    assert result.cost == 10014
    assert result.transit_days == 17


def test_case_2():
    """ ship  + auto """
    route = Route(
        date=datetime.date(2025, 5, 5),
        port_from=port_shanghai.name,
        city_to=city_kazan.name,
        ship_id=ship_rate_1.id,
        auto_id=auto_rate_2.id
    )
    result = fsm_processor(route)

    assert result is not None
    assert result.cost == 8959
    assert result.transit_days == 12


def test_case_3():
    """ invalid auto """
    route = Route(
        date=datetime.date(2025, 5, 5),
        port_from=port_shanghai.name,
        city_to=city_kazan.name,
        ship_id=ship_rate_1.id,
        auto_id=auto_rate_1.id
    )
    result = fsm_processor(route)
    assert result is None


def test_case_4():
    """ invalid auto """
    route = Route(
        date=datetime.date(2025, 5, 5),
        port_from=port_shanghai.name,
        city_to=city_kazan.name,
        ship_id=ship_rate_1.id,
        train_id=train_rate_1.id,
        auto_id=auto_rate_2.id
    )
    result = fsm_processor(route)

    assert result is None


def test_case_5():
    """ disable_city auto """
    route = Route(
        date=datetime.date(2025, 5, 5),
        port_from=port_shanghai.name,
        city_to=city_disable.name,
        ship_id=ship_rate_1.id,
        train_id=train_rate_1.id,
        auto_id=auto_rate_3.id
    )
    result = fsm_processor(route)

    assert result is None


def test_case_6():
    """ invalid dates """
    route = Route(
        date=datetime.date(2025, 5, 5),
        port_from=port_shanghai.name,
        city_to=city_disable.name,
        ship_id=ship_rate_1.id,
        train_id=train_rate_2.id,
        auto_id=auto_rate_1.id
    )
    result = fsm_processor(route)

    assert result is None
