# mypy: ignore-errors
import datetime

from route_finder.database.services import map_service, rate_service
from route_finder.exceptions import RouteException
from route_finder.route_dto import Route


def check_ship(route: Route):
    ship_rate = rate_service.get_ship_rate(route.ship_id)
    port_from = map_service.get_port(route.port_from)

    if ship_rate is None or port_from is None or ship_rate.disable:
        raise RouteException()

    if ship_rate.start_date > route.date or ship_rate.expiration_date < route.date:
        raise RouteException()

    ship_rate_port_from = map_service.get_port(ship_rate.port_from)
    ship_rate_port_to = map_service.get_port(ship_rate.port_to)

    if ship_rate_port_from is None or ship_rate_port_to is None or port_from.name != ship_rate_port_from.name:
        raise RouteException()

    if ship_rate_port_from.disable or ship_rate_port_to.disable:
        raise RouteException()

    city_from = map_service.get_city(ship_rate_port_from.city_name)
    city_to = map_service.get_city(ship_rate_port_from.city_name)

    if city_from is None or city_to is None or city_from.disable or city_to.disable:
        raise RouteException()

    country_from = map_service.get_country(city_from.country_name)
    country_to = map_service.get_country(city_to.country_name)

    if country_from is None or country_to is None or country_from.disable or country_to.disable:
        raise RouteException()

    route.ship_transit_days = (
            ship_rate.shipping_days
            + ship_rate_port_from.transit_delay
            + ship_rate_port_to.transit_delay
            + city_from.transit_delay
            + city_to.transit_delay
            + country_from.transit_delay
            + country_to.transit_delay
    )
    route.ship_cost = (
            ship_rate.cost
            + ship_rate_port_from.commission
            + ship_rate_port_to.commission
            + city_from.commission
            + city_to.commission
            + country_from.commission
            + country_to.commission
    )
    return True


def check_train(route: Route):
    ship_rate = rate_service.get_ship_rate(route.ship_id)
    train_rate = rate_service.get_train_rate(route.train_id)

    if ship_rate is None or train_rate is None or train_rate.disable:
        raise RouteException()

    date_eta = route.date + datetime.timedelta(days=route.ship_transit_days)
    if train_rate.start_date > date_eta or train_rate.expiration_date < date_eta:
        raise RouteException()

    if ship_rate.port_to != train_rate.port_from:
        raise RouteException()

    station_to = map_service.get_station(train_rate.station_to)
    if station_to is None or station_to.disable:
        raise RouteException()
    city_to = map_service.get_city(station_to.city_name)
    if city_to is None or city_to.disable:
        raise RouteException()

    route.ship_transit_days = (
            train_rate.shipping_days
            + station_to.commission
            + city_to.commission
    )
    route.ship_cost = (
            train_rate.cost
            + station_to.commission
            + city_to.commission
    )
    return True


def check_train_auto(route: Route):
    train_rate = rate_service.get_train_rate(route.train_id)
    auto_rate = rate_service.get_auto_rate(route.auto_id)

    if train_rate is None or auto_rate is None or auto_rate.disable:
        raise RouteException()

    date_eta = route.date + datetime.timedelta(days=route.ship_transit_days + route.train_transit_days)

    if auto_rate.start_date > date_eta or auto_rate.expiration_date < date_eta:
        raise RouteException()

    station_to = map_service.get_station(train_rate.station_to)
    if station_to is None or station_to.disable:
        raise RouteException()

    if auto_rate.city_from != station_to.city_name:
        raise RouteException()

    city_to = map_service.get_city(auto_rate.city_to)

    if city_to is None or route.city_to != city_to.name or city_to.disable:
        raise RouteException()

    route.auto_transit_days = (
            auto_rate.shipping_days
            + city_to.transit_delay
    )
    route.auto_cost = (
            auto_rate.cost
            + city_to.commission
    )
    return True


def check_ship_auto(route: Route):
    ship_rate = rate_service.get_ship_rate(route.ship_id)
    auto_rate = rate_service.get_auto_rate(route.auto_id)

    if ship_rate is None or auto_rate is None or auto_rate.disable:
        raise RouteException()

    date_eta = route.date + datetime.timedelta(days=route.ship_transit_days)

    if auto_rate.start_date > date_eta or auto_rate.expiration_date < date_eta:
        raise RouteException()

    port_to = map_service.get_port(ship_rate.port_to)
    if port_to is None or port_to.disable:
        raise RouteException()

    if auto_rate.city_from != port_to.city_name:
        raise RouteException()

    city_to = map_service.get_city(auto_rate.city_to)

    if city_to is None or route.city_to != city_to.name or city_to.disable:
        raise RouteException()

    route.auto_transit_days = (
            auto_rate.shipping_days
            + city_to.transit_delay
    )
    route.auto_cost = (
            auto_rate.cost
            + city_to.commission
    )
    return True


def collect(route: Route):
    route.cost = route.ship_cost + route.train_cost + route.auto_cost
    route.transit_days = route.ship_transit_days + route.train_transit_days + route.auto_transit_days
    return True
