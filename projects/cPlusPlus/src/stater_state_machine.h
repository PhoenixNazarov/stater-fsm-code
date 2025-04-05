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

class empty_context : public context {
};

template<typename C>
class context_json_adapter {
public:
    virtual ~context_json_adapter() = default;

    virtual std::string to_json(C *context) const = 0;

    virtual C* from_json(const std::string &json) const = 0;
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
    std::optional<::event<C> > event;
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
    std::unordered_map<T, std::vector<Transition<T, C> > > transitions_grouped_start;
    std::unordered_map<std::string, Transition<T, C> > transitions_by_name;
    std::unordered_map<std::string, std::vector<::transition_middleware<C> > > transition_middlewares;
    std::vector<transition_name_middleware<C> > transition_all_middlewares;
    std::unordered_map<std::string, std::vector<event<C> > > transition_callbacks;
    std::vector<name_event<C> > transition_all_callbacks;
    std::unordered_map<T, std::vector<event<C> > > state_callbacks;
    std::vector<state_event<T, C> > state_all_callbacks;
    T current_state;
    C *context;
    std::shared_ptr<context_json_adapter<C> > context_json_adapter_;
    bool enable_events_ = true;

public:
    virtual ~stater_state_machine() = default;

    stater_state_machine() = default;

    stater_state_machine(std::vector<Transition<T, C> > trans, C *ctx, T start_state): transitions(std::move(trans)),
        current_state(start_state), context(ctx) {
        for (const auto &t: transitions) {
            states.insert(t.start);
            states.insert(t.end);
            transitions_grouped_start[t.start].push_back(t);
            transitions_by_name[t.name] = t;
        }
    }

    stater_state_machine(
        const std::vector<Transition<T, C> > &transitions,
        C *context,
        T startState,
        const std::unordered_set<T> &states,
        const std::unordered_map<std::string, std::vector<transition_middleware<C> > > &transition_middlewares,
        const std::vector<transition_name_middleware<C> > &transition_all_middlewares,
        const std::unordered_map<std::string, std::vector<event<C> > > &transition_callbacks,
        const std::vector<name_event<C> > &transition_all_callbacks,
        const std::unordered_map<T, std::vector<event<C> > > &state_callbacks,
        const std::vector<state_event<T, C> > &state_all_callbacks,
        std::shared_ptr<::context_json_adapter<C> > context_json_adapter_
    )
        : transitions(transitions),
          transition_middlewares(transition_middlewares),
          transition_all_middlewares(transition_all_middlewares),
          transition_callbacks(transition_callbacks),
          transition_all_callbacks(transition_all_callbacks),
          state_callbacks(state_callbacks),
          state_all_callbacks(state_all_callbacks),
          context(context),
          context_json_adapter_(context_json_adapter_),
          current_state(startState),
          enable_events_(true) {
        if (!states.empty()) {
            this->states = states;
        } else {
            this->states = std::unordered_set<T>();
            for (const auto &el: transitions) {
                this->states.insert(el.start);
                this->states.insert(el.end);
            }
        }

        this->transitions_grouped_start = std::unordered_map<T, std::vector<Transition<T, C> > >();
        this->transitions_by_name = std::unordered_map<std::string, Transition<T, C> >();

        for (const auto &el: transitions) {
            this->transitions_by_name[el.name] = el;

            auto it = this->transitions_grouped_start.find(el.start);
            if (it != this->transitions_grouped_start.end()) {
                it->second.push_back(el);
            } else {
                this->transitions_grouped_start[el.start] = {el};
            }
        }
    }

    T get_state() const {
        return current_state;
    }

    C *get_context() const {
        return context;
    }

    void transition(const std::string &name) {
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

    void auto_transition() {
        for (const auto &t: transitions_grouped_start[current_state]) {
            try {
                transition(t.name);
                return;
            } catch (...) {
            }
        }
    }

    std::string to_json_schema() {
        json j;
        std::vector<T> states_v(states.begin(), states.end());

        std::sort(states_v.begin(), states_v.end(), [](T a, T b) {
            json state_a_json = a;
            json state_b_json = b;
            return state_a_json.dump().compare(state_b_json.dump()) < 0;
        });
        j["states"] = states_v;
        j["startState"] = current_state;
        json transitions_json = json::array();
        for (const auto &t: transitions) {
            transitions_json.push_back({
                {"name", t.name},
                {"start", t.start},
                {"end", t.end}
            });
        }
        j["transitions"] = transitions_json;
        return j.dump();
    }

    std::string to_json() {
        json j;
        j["state"] = current_state;
        j["context"] = context_json_adapter_->to_json(context);
        return j.dump();
    }

    void from_json(std::string json_) {
        auto data = json::parse(json_);
        this->current_state = data["state"];
        this->context = context_json_adapter_->from_json(data["context"]);
    }

    void disable_events() {
        enable_events_ = false;
    }

    void enable_events() {
        enable_events_ = true;
    }
};

template<typename T, typename C>
using state_machine_factory = std::function<std::unique_ptr<stater_state_machine<T, C> >(
    std::vector<Transition<T, C> >,
    C *,
    T,
    std::unordered_set<T>,
    std::unordered_map<std::string, std::vector<transition_middleware<C> > >,
    std::vector<transition_name_middleware<C> >,
    std::unordered_map<std::string, std::vector<event<C> > >,
    std::vector<name_event<C> >,
    std::unordered_map<T, std::vector<event<C> > >,
    std::vector<state_event<T, C> >,
    std::shared_ptr<context_json_adapter<C> >)>;


template<typename T, typename C>
class base_fsm : public stater_state_machine<T, C> {
public:
    base_fsm(
        const std::vector<Transition<T, C> > &transitions,
        C *context,
        T startState,
        const std::unordered_set<T> &states,
        const std::unordered_map<std::string, std::vector<transition_middleware<C> > > &transition_middlewares,
        const std::vector<transition_name_middleware<C> > &transition_all_middlewares,
        const std::unordered_map<std::string, std::vector<event<C> > > &transition_callbacks,
        const std::vector<name_event<C> > &transition_all_callbacks,
        const std::unordered_map<T, std::vector<event<C> > > &state_callbacks,
        const std::vector<state_event<T, C> > &state_all_callbacks,
        std::shared_ptr<context_json_adapter<C> > context_json_adapter_
    )
        : stater_state_machine<T, C>(
            transitions, context, startState, states,
            transition_middlewares, transition_all_middlewares,
            transition_callbacks, transition_all_callbacks,
            state_callbacks, state_all_callbacks, context_json_adapter_
        ) {
    }
};


template<typename T, typename C>
class stater_state_machine_builder {
    std::map<std::string, Transition<T, C> > transitions;
    std::unordered_set<T> states;
    T startState;
    C *context;
    std::unordered_map<std::string, std::vector<::transition_middleware<C> > > transition_middlewares;
    std::vector<transition_name_middleware<C> > transition_all_middlewares;
    std::unordered_map<std::string, std::vector<event<C> > > transition_callbacks;
    std::vector<name_event<C> > transition_all_callbacks;
    std::unordered_map<T, std::vector<event<C> > > state_callbacks;
    std::vector<state_event<T, C> > state_all_callbacks;

    state_machine_factory<T, C> factory = [](
        const std::vector<Transition<T, C> > &transitions,
        C *context,
        T startState,
        const std::unordered_set<T> &states,
        const std::unordered_map<std::string, std::vector<::transition_middleware<
            C> > > &transitionMiddleware,
        const std::vector<transition_name_middleware<C> > &
        transitionAllMiddlewares,
        const std::unordered_map<std::string, std::vector<event<C> > > &
        transitionCallbacks,
        const std::vector<name_event<C> > &transition_all_callbacks,
        const std::unordered_map<T, std::vector<event<C> > > &state_callbacks,
        const std::vector<state_event<T, C> > &state_all_callbacks,
        std::shared_ptr<context_json_adapter<C> > context_json_adapter_) ->
        std::unique_ptr<base_fsm<T, C> > {
        return std::make_unique<base_fsm<T, C> >(
            transitions, context, startState, states,
            transitionMiddleware, transitionAllMiddlewares,
            transitionCallbacks, transition_all_callbacks,
            state_callbacks, state_all_callbacks, context_json_adapter_);
    };
    std::shared_ptr<context_json_adapter<C> > context_json_adapter_ = nullptr;

public:
    stater_state_machine_builder() = default;

    stater_state_machine_builder &add_transition(
        const std::string &name, T start, T end,
        std::optional<std::function<bool(C *)> > condition = {},
        std::optional<std::function<void(C *)> > event = {}
    ) {
        add_state(start);
        add_state(end);
        transitions[name] = Transition<T, C>(name, start, end, condition, event);
        return *this;
    }

    stater_state_machine_builder &add_state(T state) {
        states.insert(state);
        return *this;
    }

    stater_state_machine_builder &
    set_transition_condition(const std::string &name, std::function<bool(C *)> condition) {
        if (transitions.find(name) == transitions.end()) {
            throw std::invalid_argument("Transition not found: " + name);
        }
        transitions[name].condition = condition;
        return *this;
    }

    stater_state_machine_builder &set_transition_event(const std::string &name, const event<C> &event) {
        if (transitions.find(name) == transitions.end()) {
            throw std::invalid_argument("Transition not found: " + name);
        }
        transitions[name].event = event;
        return *this;
    }

    stater_state_machine_builder &transition_middleware(const std::string &name,
                                                        const ::transition_middleware<C> &middleware) {
        transition_middlewares[name].push_back(middleware);
        return *this;
    }

    stater_state_machine_builder &transition_all_middleware(const transition_name_middleware<C> &middleware) {
        transition_all_middlewares.push_back(middleware);
        return *this;
    }

    stater_state_machine_builder &subscribe_on_transition(const std::string &name, const event<C> &event) {
        transition_callbacks[name].push_back(event);
        return *this;
    }

    stater_state_machine_builder &subscribe_on_all_transition(const name_event<C> &event) {
        transition_all_callbacks.push_back(event);
        return *this;
    }

    stater_state_machine_builder &subscribe_on_state(T state, const event<C> &event) {
        state_callbacks[state].push_back(event);
        return *this;
    }

    stater_state_machine_builder &subscribe_on_all_state(const state_event<T, C> &event) {
        state_all_callbacks.push_back(event);
        return *this;
    }

    stater_state_machine_builder &set_start_state(T state) {
        startState = state;
        return *this;
    }

    stater_state_machine_builder &set_context(C *ctx) {
        context = ctx;
        return *this;
    }

    stater_state_machine_builder &from_json_schema(
        const std::string &schema
    ) {
        auto data = json::parse(schema);
        for (const auto &state: data["states"]) {
            add_state(state);
        }
        for (const auto &transition: data["transitions"]) {
            add_transition(transition["name"], transition["start"], transition["end"]);
        }
        set_start_state(data["startState"]);
        return *this;
    }

    stater_state_machine_builder &set_factory(state_machine_factory<T, C> factory_func) {
        factory = factory_func;
        return *this;
    }

    stater_state_machine_builder &set_context_json_adapter(std::shared_ptr<::context_json_adapter<C> > adapter) {
        context_json_adapter_ = adapter;
        return *this;
    }

    std::unique_ptr<stater_state_machine<T, C> > build() {
        std::vector<Transition<T, C> > values;
        for (const auto &pair: transitions) {
            values.push_back(pair.second);
        }
        return factory(
            values,
            context,
            startState,
            states,
            transition_middlewares,
            transition_all_middlewares,
            transition_callbacks,
            transition_all_callbacks,
            state_callbacks,
            state_all_callbacks,
            context_json_adapter_
        );
    }
};
