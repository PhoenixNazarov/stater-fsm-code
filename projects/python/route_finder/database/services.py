from route_finder.database.dto import ShipRate, TrainRate, AutoRate, Country, City, Port, Station


class RateService:
    def __init__(self):
        self.ships = {}
        self.trains = {}
        self.autos = {}

    def add_ship_rate(self, rate: ShipRate):
        self.ships[rate.id] = rate

    def add_train_rate(self, rate: TrainRate):
        self.trains[rate.id] = rate

    def add_auto_rate(self, rate: AutoRate):
        self.autos[rate.id] = rate

    def get_ship_rate(self, id: int) -> ShipRate | None:
        return self.ships.get(id)

    def get_train_rate(self, id: int) -> TrainRate | None:
        return self.trains.get(id)

    def get_auto_rate(self, id: int) -> AutoRate | None:
        return self.autos.get(id)


class MapService:
    def __init__(self):
        self.countries = {}
        self.cities = {}
        self.ports = {}
        self.stations = {}

    def add_country(self, country: Country):
        self.countries[country.name] = country

    def add_city(self, city: City):
        self.cities[city.name] = city

    def add_port(self, port: Port):
        self.ports[port.name] = port

    def add_station(self, station: Station):
        self.stations[station.name] = station

    def get_country(self, name: str) -> Country | None:
        return self.countries.get(name)

    def get_city(self, name: str) -> City | None:
        return self.cities.get(name)

    def get_port(self, name: str) -> Port | None:
        return self.ports.get(name)

    def get_station(self, name: str) -> Station | None:
        return self.stations.get(name)


rate_service = RateService()
map_service = MapService()
