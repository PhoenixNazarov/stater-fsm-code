from __future__ import annotations

import datetime

from pydantic import BaseModel


class Country(BaseModel):
    name: str
    commission: int
    disable: bool = False
    dangerous: bool = False
    transit_delay: int = 0


class City(BaseModel):
    name: str
    commission: int
    disable: bool = False

    country_name: str
    transit_delay: int = 0


class Port(BaseModel):
    name: str
    commission: int
    disable: bool = False

    city_name: str
    transit_delay: int = 0


class Station(BaseModel):
    name: str
    commission: int
    disable: bool = False

    city_name: str
    transit_delay: int = 0


class ShipRate(BaseModel):
    id: int
    start_date: datetime.date
    expiration_date: datetime.date
    shipping_days: int

    port_from: str
    port_to: str
    cost: int
    disable: bool = False


class TrainRate(BaseModel):
    id: int
    start_date: datetime.date
    expiration_date: datetime.date
    shipping_days: int

    port_from: str
    station_to: str
    cost: int
    disable: bool = False


class AutoRate(BaseModel):
    id: int
    start_date: datetime.date
    expiration_date: datetime.date
    shipping_days: int

    city_from: str
    city_to: str
    cost: int
    disable: bool = False
