from pydantic import BaseModel
import datetime


class Route(BaseModel):
    date: datetime.date | None = None
    port_from: str | None = None
    city_to: str | None = None

    ship_id: int | None = None
    ship_cost: int = 0
    ship_transit_days: int = 0

    train_id: int | None = None
    train_cost: int = 0
    train_transit_days: int = 0

    auto_id: int | None = None
    auto_cost: int = 0
    auto_transit_days: int = 0

    cost: int | None = None
    transit_days: int | None = None
