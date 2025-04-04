//
// Created by Владимир on 02.04.2025.
//

#include "stater_state_machine.h"

#include <vector>
#include <unordered_set>
#include <functional>
#include <optional>
#include <nlohmann/json.hpp>

using json = nlohmann::json;


class EmptyContext : public context {
};


template<typename TC>
context_json_adapter<TC>::~context_json_adapter() {
}

template<typename T, typename C>
stater_state_machine<T, C>::stater_state_machine(std::vector<Transition<T, C> > trans, C *ctx, T start_state)
    : transitions(std::move(trans)), context(ctx), current_state(start_state) {
    for (const auto &t: transitions) {
        states.insert(t.start);
        states.insert(t.end);
        transitions_by_start[t.start].push_back(t);
        transitions_by_name[t.name] = t;
    }
}

template<typename T, typename C>
stater_state_machine<T, C>::stater_state_machine(
    const std::vector<Transition<T, C> > &transitions, C *context,
    T startState, const std::unordered_set<T> &states,
    const std::unordered_map<std::string, std::vector<transition_middleware<C> > > &transition_middlewares,
    const std::vector<transition_name_middleware<C> > &transition_all_middlewares,
    const std::unordered_map<std::string, std::vector<event<C> > > &transition_callbacks,
    const std::vector<name_event<C> > &transition_all_callbacks,
    const std::unordered_map<T, std::vector<event<C> > > &state_callbacks,
    const std::vector<state_event<T, C> > &state_all_callbacks
    // std::shared_ptr<::context_json_adapter<C> > context_json_adapter
) : transitions(transitions),
    context(context),
    current_state(startState),
    states(states),
    transition_middlewares(transition_middlewares),
    transition_all_middlewares(transition_all_middlewares),
    transition_callbacks(transition_callbacks),
    transition_all_callbacks(transition_all_callbacks),
    state_callbacks(state_callbacks),
    state_all_callbacks(state_all_callbacks)
    // context_json_adapter(context_json_adapter)
{
}

template<typename T, typename C>
stater_state_machine<T, C>::stater_state_machine() = default;

template<typename T, typename C>
T stater_state_machine<T, C>::get_state() const {
    return current_state;
}

template<typename T, typename C>
C stater_state_machine<T, C>::get_context() const {
    return context;
}

template<typename T, typename C>
void stater_state_machine<T, C>::transition(const std::string &name) {
    auto it = transitions_by_name.find(name);
    if (it == transitions_by_name.end()) {
        throw std::runtime_error("Transition not found: " + name);
    }
    const Transition<T, C> &t = it->second;

    if (current_state != t.start) {
        throw std::runtime_error("Invalid transition: incorrect start state");
    }

    if (t.condition && !t.condition.value()(context)) {
        throw std::runtime_error("Condition failed for transition: " + name);
    }

    int index = 0, index2 = 0;
    auto transitionMiddleware = transition_middlewares.find(name);

    auto conditionHandler = [&]() {
        if (t.condition && !t.condition.value()(context)) {
            throw std::runtime_error("Condition returned false for transition " + name);
        }
    };

    std::function<void(C *)> internalNext = [&](C *ctx) {
        if (transitionMiddleware != transition_middlewares.end() && index2 < transitionMiddleware->second.size()) {
            transitionMiddleware->second[index2++](ctx, internalNext);
        } else {
            conditionHandler();
        }
    };

    std::function<void(const std::string &, C *)> next = [&](const std::string &n, C *ctx) {
        if (index < transition_all_middlewares.size()) {
            transition_all_middlewares[index++](n, ctx, next);
        } else {
            internalNext(ctx);
        }
    };

    if (enable_events_) {
        next(name, context);
    }

    current_state = t.end;

    if (!enable_events_) {
        return;
    }

    if (t.event) {
        t.event.value()(context);
    }

    for (auto &cb: transition_all_callbacks) {
        cb(name, context);
    }
    for (auto &cb: transition_callbacks[name]) {
        cb(context);
    }
    for (auto &cb: state_all_callbacks) {
        cb(current_state, context);
    }
    for (auto &cb: state_callbacks[current_state]) {
        cb(context);
    }
}

template<typename T, typename C>
void stater_state_machine<T, C>::auto_transition() {
    for (const auto &t: transitions_by_start[current_state]) {
        try {
            transition(t.name);
            return;
        } catch (...) {
        }
    }
}


template<typename T, typename C>
std::string stater_state_machine<T, C>::to_json_schema() {
    return json{{"states", states}, {"startState", current_state}, {"transitions", transitions}};
}

template<typename T, typename C>
std::string stater_state_machine<T, C>::to_json() {
    return json{{"state", current_state}};
}

template<typename T, typename C>
void stater_state_machine<T, C>::from_json(const json &j) {
    current_state = j.at("state").get<T>();
}

template<typename T, typename C>
void stater_state_machine<T, C>::disable_events() {
    enable_events_ = false;
}

template<typename T, typename C>
void stater_state_machine<T, C>::enable_events() {
    enable_events_ = true;
}

template<typename T, typename C>
class base_fsm : public state_event<T, C> {
};


template<typename T, typename C>
stater_state_machine_builder<T, C>::stater_state_machine_builder() = default;

template<typename T, typename C>
stater_state_machine_builder<T, C> &stater_state_machine_builder<T, C>::add_transition(
    const std::string &name, T start, T end,
    std::optional<std::function<bool(C *)> > condition,
    std::optional<std::function<void(C *)> > event
) {
    add_state(start);
    add_state(end);
    transitions[name] = Transition<T, C>(name, start, end, condition, event);
    return *this;
}

template<typename T, typename C>
stater_state_machine_builder<T, C> &stater_state_machine_builder<T, C>::add_state(T state) {
    states.insert(state);
    return *this;
}

template<typename T, typename C>
stater_state_machine_builder<T, C> &stater_state_machine_builder<T, C>::set_transition_condition(
    const std::string &name, std::function<bool(C *)> condition) {
    if (transitions.find(name) == transitions.end()) {
        throw std::invalid_argument("Transition not found: " + name);
    }
    transitions[name].condition = condition;
    return *this;
}

template<typename T, typename C>
stater_state_machine_builder<T, C> &stater_state_machine_builder<T, C>::set_transition_event(
    const std::string &name, const event<C> &event) {
    if (transitions.find(name) == transitions.end()) {
        throw std::invalid_argument("Transition not found: " + name);
    }
    transitions[name].event = event;
    return *this;
}

template<typename T, typename C>
stater_state_machine_builder<T, C> &stater_state_machine_builder<T, C>::transition_middleware(const std::string &name,
    const ::transition_middleware<C> &middleware) {
    transition_middlewares[name].push_back(middleware);
    return *this;
}

template<typename T, typename C>
stater_state_machine_builder<T, C> &stater_state_machine_builder<T, C>::transition_all_middleware(
    const transition_name_middleware<C> &middleware) {
    transition_all_middlewares.push_back(middleware);
    return *this;
}

template<typename T, typename C>
stater_state_machine_builder<T, C> &stater_state_machine_builder<T, C>::subscribe_on_transition(
    const std::string &name, const event<C> &event) {
    transition_callbacks[name].push_back(event);
    return *this;
}

template<typename T, typename C>
stater_state_machine_builder<T, C> &stater_state_machine_builder<T, C>::subscribe_on_all_transition(
    const name_event<C> &event) {
    transition_all_callbacks.push_back(event);
    return *this;
}

template<typename T, typename C>
stater_state_machine_builder<T, C> &stater_state_machine_builder<T, C>::subscribe_on_state(
    T state, const event<C> &event) {
    state_callbacks[state].push_back(event);
    return *this;
}

template<typename T, typename C>
stater_state_machine_builder<T, C> &stater_state_machine_builder<T, C>::subscribe_on_all_state(
    const state_event<T, C> &event) {
    state_all_callbacks.push_back(event);
    return *this;
}


template<typename T, typename C>
stater_state_machine_builder<T, C> &stater_state_machine_builder<T, C>::set_start_state(T state) {
    startState = state;
    return *this;
}

template<typename T, typename C>
stater_state_machine_builder<T, C> &stater_state_machine_builder<T, C>::set_context(C *ctx) {
    context = ctx;
    return *this;
}

template<typename T, typename C>
stater_state_machine_builder<T, C> &stater_state_machine_builder<T, C>::from_json_schema(const std::string &schema,
    std::function<T(const std::string &)> stateConverter) {
    auto data = json::parse(schema);
    for (const auto &state: data["states"]) {
        addState(stateConverter(state));
    }
    for (const auto &transition: data["transitions"]) {
        addTransition(transition["name"], stateConverter(transition["start"]), stateConverter(transition["end"]));
    }
    setStartState(stateConverter(data["startState"]));
    return *this;
}

template<typename T, typename C>
stater_state_machine_builder<T, C> &stater_state_machine_builder<T, C>::set_factory(
    state_machine_factory<T, C> factoryFunc) {
    factory = factoryFunc;
    return *this;
}

template<typename T, typename C>
stater_state_machine_builder<T, C> &stater_state_machine_builder<T, C>::set_context_json_adapter(
    std::optional<std::function<void(C *, json &)> > adapter) {
    // contextJsonAdapter = adapter;
    return *this;
}

template<typename T, typename C>
stater_state_machine<T, C> stater_state_machine_builder<T, C>::build() {
    return factory(
        std::vector<Transition<T, C> >(transitions.begin(), transitions.end()),
        context,
        startState
    );
}
