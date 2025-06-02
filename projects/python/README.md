# RouteFinder FSM Processor

Этот проект демонстрирует пример использования библиотеки [`stater_state_machine`](https://github.com/PhoenixNazarov/stater-fsm-code) для построения конечного автомата, предназначенного для проверки маршрутов доставки.

## 📦 Описание

Система рассчитывает стоимость и срок доставки маршрута, включающего последовательность сегментов:

- 🛳 **Морской транспорт (ship)**
- 🚂 **Железнодорожный транспорт (train)**
- 🚚 **Автомобильный транспорт (auto)**

Каждый сегмент проверяется на валидность с учётом дат действия тарифов, соответствия точек маршрута, статусов городов, портов и стран.

Если маршрут валиден — рассчитываются стоимость и общее время в пути.

## 🧠 Основной алгоритм

Главная логика реализована в файле [`fsm_processor`](route_finder/generated_fsm.py) — автоматически сгенерированном на основе схемы [`schema.json`](route_finder/schema.json) при помощи библиотеки `stater_state_machine`.

Вся проверка маршрута разбита на функции:

- `check_ship(route)`
- `check_train(route)`
- `check_train_auto(route)`
- `check_ship_auto(route)`
- `collect(route)`

Каждая из них отвечает за определённый сегмент маршрута.

## 🧪 Тесты

Файл [`generated_test.py`](tests/generated_test.py) содержит автосгенерированные тест-кейсы. Также в проекте есть набор ручных тестов:

```python
def test_case_1():  # ship + train + auto
def test_case_2():  # ship + auto
def test_case_3():  # invalid auto
def test_case_4():  # invalid train-auto combo
def test_case_5():  # auto в отключённый город
def test_case_6():  # несовпадающие даты
```
