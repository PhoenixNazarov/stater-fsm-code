//
// Created by Владимир on 02.04.2025.
//

#pragma once

#include <iostream>
#include <vector>
#include <unordered_map>
#include <unordered_set>
#include <functional>
#include <optional>
#include <nlohmann/json.hpp>

using json = nlohmann::json;

class context {
public:
    virtual ~context() = default;
};

template<typename C>
class context_json_adapter {
public:
    virtual ~context_json_adapter();
    virtual std::string to_json(C *context) const = 0;
    virtual C from_json(const std::string &json) const = 0;
};


template<typename C>
using event = std::function<void(C *)>;

template<typename C>
using name_event = std::function<void(const std::string &, C *)>;

template<typename T, typename C>
using state_event = std::function<void(T, C *)>;

template<typename C>
using transition_middleware = std::function<void(C *, event<C>)>;

template<typename C>
using transition_name_middleware = std::function<void(const std::string &, C *, name_event<C>)>;

template<typename T, typename C>
struct Transition {
    std::string name;
    T start;
    T end;
    std::optional<std::function<bool(C *)> > condition;
    std::optional<event<C> > event;
};

template<typename T>
struct json_schema {
    std::vector<T> states;
    T start_state;
    std::vector<Transition<T, context> > transitions;
};

template<typename T>
struct json_state {
    T state;
    std::string context;
};


template<typename T, typename C>
class stater_state_machine {
    std::vector<Transition<T, C> > transitions;
    std::unordered_set<T> states;
    std::unordered_map<T, std::vector<Transition<T, C> > > transitions_by_start;
    std::unordered_map<std::string, Transition<T, C> > transitions_by_name;
    std::unordered_map<std::string, std::vector<transition_middleware<C> > > transition_middlewares;
    std::vector<std::function<void(const std::string &, C *)> > transition_all_middlewares;
    std::unordered_map<std::string, std::vector<std::function<void(C *)> > > transition_callbacks;
    std::vector<std::function<void(const std::string &, C *)> > transition_all_callbacks;
    std::unordered_map<T, std::vector<std::function<void(C *)> > > state_callbacks;
    std::vector<std::function<void(T, C *)> > state_all_callbacks;
    T current_state;
    C *context;
    std::shared_ptr<context_json_adapter<C>> context_json_adapter;
    bool enable_events_ = true;

public:
    stater_state_machine();

    stater_state_machine(std::vector<Transition<T, C> > trans, C *ctx, T start_state);
    stater_state_machine(
        const std::vector<Transition<T, C>>& transitions,
        C* context,
        T startState,
        const std::unordered_set<T>& states,
        const std::unordered_map<std::string, std::vector<transition_middleware<C>>>& transition_middlewares,
        const std::vector<transition_name_middleware<C>>& transition_all_middlewares,
        const std::unordered_map<std::string, std::vector<event<C>>>& transition_callbacks,
        const std::vector<name_event<C>>& transition_all_callbacks,
        const std::unordered_map<T, std::vector<event<C>>>& state_callbacks,
        const std::vector<state_event<T, C>>& state_all_callbacks
        // std::shared_ptr<::context_json_adapter<C>> context_json_adapter
    );

    T get_state() const;
    C get_context() const;
    void transition(const std::string &name);
    void auto_transition();
    std::string to_json_schema();
    std::string to_json();
    void from_json(const json &j);
    void disable_events();
    void enable_events();
};


template<typename T, typename TC>
using state_machine_factory = std::function<stater_state_machine<T, TC>(
    std::vector<Transition<T, TC> >,
    TC *,
    T,
    std::unordered_set<T>,
    std::unordered_map<std::string, std::vector<transition_middleware<TC> > >,
    std::vector<transition_name_middleware<TC> >,
    std::unordered_map<std::string, std::vector<event<TC> > >,
    std::vector<name_event<TC> >,
    std::unordered_map<T, std::vector<event<TC> > >,
    std::vector<state_event<T, TC> >,
    std::optional<std::shared_ptr<context_json_adapter<TC> > >)>;

template<typename T, typename C>
class stater_state_machine_builder {
    std::unordered_map<std::string, Transition<T, C> > transitions;
    std::unordered_set<T> states;
    T startState;
    C *context;
    std::unordered_map<std::string, std::vector<transition_middleware<C> > > transition_middlewares;
    std::vector<transition_name_middleware<C> > transition_all_middlewares;
    std::unordered_map<std::string, std::vector<event<C> > > transition_callbacks;
    std::vector<name_event<C> > transition_all_callbacks;
    std::unordered_map<T, std::vector<event<C> > > state_callbacks;
    std::vector<state_event<T, C> > state_all_callbacks;
    state_machine_factory<T, C> factory = {};

public:
    stater_state_machine_builder();
    stater_state_machine_builder &add_transition(
        const std::string &name, T start, T end,
        std::optional<std::function<bool(C *)> > condition = {},
        std::optional<std::function<void(C *)> > event = {}
    );
    stater_state_machine_builder &add_state(T state);
    stater_state_machine_builder &set_transition_condition(const std::string &name, std::function<bool(C *)> condition);
    stater_state_machine_builder &set_transition_event(const std::string &name, const event<C> &event);
    stater_state_machine_builder &transition_middleware(const std::string &name, const transition_middleware<C> &middleware);
    stater_state_machine_builder &transition_all_middleware(const transition_name_middleware<C> &middleware);
    stater_state_machine_builder &subscribe_on_transition(const std::string &name, const event<C> &event);
    stater_state_machine_builder &subscribe_on_all_transition(const name_event<C> &event);
    stater_state_machine_builder &subscribe_on_state(T state, const event<C> &event);
    stater_state_machine_builder &subscribe_on_all_state(const state_event<T, C> &event);
    stater_state_machine_builder &set_start_state(T state);
    stater_state_machine_builder &set_context(C *ctx);
    stater_state_machine_builder &from_json_schema(const std::string &schema, std::function<T(const std::string &)> stateConverter);
    stater_state_machine_builder &set_factory(state_machine_factory<T, C> factoryFunc);
    stater_state_machine_builder &set_context_json_adapter(std::optional<std::function<void(C *, json &)> > adapter);
    stater_state_machine<T, C> build();
};
