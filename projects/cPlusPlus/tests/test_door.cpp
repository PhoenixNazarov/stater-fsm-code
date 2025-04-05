//
// Created by Владимир on 02.04.2025.
//

#include <stater_state_machine.h>
#include <gtest/gtest.h>

#include <utility>

enum class states {
    CLOSE,
    AJAR,
    OPEN
};

inline void to_json(nlohmann::json &j, const states &e) {
    switch (e) {
        case states::CLOSE: j = "CLOSE";
            break;
        case states::AJAR: j = "AJAR";
            break;
        case states::OPEN: j = "OPEN";
            break;
    }
}

inline void from_json(const nlohmann::json &j, states &e) {
    std::string state_str = j.get<std::string>();
    if (state_str == "CLOSE") e = states::CLOSE;
    else if (state_str == "AJAR") e = states::AJAR;
    else if (state_str == "OPEN") e = states::OPEN;
}

class door_fsm_context : context {
    int degree_of_opening;

public:
    door_fsm_context() : degree_of_opening(100) {
    }

    explicit door_fsm_context(int degree_of_opening) : degree_of_opening(degree_of_opening) {
    }

    [[nodiscard]] int get_degree_of_opening() const {
        return degree_of_opening;
    }

    void set_degree_of_opening(int degree_of_opening) {
        this->degree_of_opening = degree_of_opening;
    }
};

door_fsm_context door_fsm_context_ = door_fsm_context();

class types_door_state_machine : public stater_state_machine<states, door_fsm_context> {
public:
    types_door_state_machine() : stater_state_machine(
        {
            Transition<::states, door_fsm_context>("preOpen", states::CLOSE, states::AJAR,
                                                   [](door_fsm_context *ctx) { return true; },
                                                   [](door_fsm_context *ctx) { ctx->set_degree_of_opening(1); }),
            Transition<::states, door_fsm_context>("preClose", states::OPEN, states::AJAR,
                                                   [](door_fsm_context *ctx) { return true; },
                                                   [](door_fsm_context *ctx) { ctx->set_degree_of_opening(99); }),
            Transition<::states, door_fsm_context>("open", states::AJAR, states::OPEN,
                                                   [](door_fsm_context *ctx) {
                                                       return ctx->get_degree_of_opening() >= 99;
                                                   },
                                                   [](door_fsm_context *ctx) { ctx->set_degree_of_opening(100); }),
            Transition<::states, door_fsm_context>("close", states::AJAR, states::CLOSE,
                                                   [](door_fsm_context *ctx) {
                                                       return ctx->get_degree_of_opening() <= 1;
                                                   },
                                                   [](door_fsm_context *ctx) { ctx->set_degree_of_opening(0); }),
            Transition<::states, door_fsm_context>("ajarPlus", states::AJAR, states::AJAR,
                                                   [](door_fsm_context *ctx) {
                                                       return ctx->get_degree_of_opening() >= 1 && ctx->
                                                              get_degree_of_opening() <= 98;
                                                   },
                                                   [](door_fsm_context *ctx) {
                                                       ctx->set_degree_of_opening(ctx->get_degree_of_opening() + 1);
                                                   }),
            Transition<::states, door_fsm_context>("ajarMinus", states::AJAR, states::AJAR,
                                                   [](door_fsm_context *ctx) {
                                                       return ctx->get_degree_of_opening() >= 2 && ctx->
                                                              get_degree_of_opening() <= 99;
                                                   },
                                                   [](door_fsm_context *ctx) {
                                                       ctx->set_degree_of_opening(ctx->get_degree_of_opening() - 1);
                                                   })
        },
        &door_fsm_context_,
        states::OPEN,
        {states::OPEN, states::CLOSE, states::AJAR},
        {},
        {},
        {},
        {},
        {},
        {},
        nullptr
    ) {
    }

    types_door_state_machine(
        const std::vector<Transition<::states, door_fsm_context> > &transitions,
        door_fsm_context *context,
        ::states startState,
        const std::unordered_set<::states> &states,
        const std::unordered_map<std::string, std::vector<transition_middleware<door_fsm_context> > > &
        transition_middlewares,
        const std::vector<transition_name_middleware<door_fsm_context> > &transition_all_middlewares,
        const std::unordered_map<std::string, std::vector<event<door_fsm_context> > > &transition_callbacks,
        const std::vector<name_event<door_fsm_context> > &transition_all_callbacks,
        const std::unordered_map<::states, std::vector<event<door_fsm_context> > > &state_callbacks,
        const std::vector<state_event<::states, door_fsm_context> > &state_all_callbacks,
        std::shared_ptr<::context_json_adapter<door_fsm_context> > context_json_adapter_
    ): stater_state_machine(transitions, context, startState, states, transition_middlewares,
                            transition_all_middlewares, transition_callbacks, transition_all_callbacks, state_callbacks,
                            state_all_callbacks, std::move(context_json_adapter_)) {
    }

    void ajar_plus() {
        transition("ajarPlus");
    }

    void ajar_minus() {
        transition("ajarMinus");
    }

    void pre_open() {
        transition("preOpen");
    }

    void pre_close() {
        transition("preClose");
    }

    void open() {
        transition("open");
    }

    void close() {
        transition("close");
    }
};

void test_door_schema(stater_state_machine<states, door_fsm_context> &door) {
    ASSERT_EQ(states::OPEN, door.get_state());
    ASSERT_EQ(100, door.get_context()->get_degree_of_opening());
    door.transition("preClose");
    ASSERT_EQ(states::AJAR, door.get_state());
    ASSERT_EQ(99, door.get_context()->get_degree_of_opening());
    while (door.get_context()->get_degree_of_opening() > 1) {
        door.transition("ajarMinus");
        ASSERT_EQ(states::AJAR, door.get_state());
    }

    ASSERT_EQ(1, door.get_context()->get_degree_of_opening());
    door.transition("close");
    ASSERT_EQ(0, door.get_context()->get_degree_of_opening());
    ASSERT_EQ(states::CLOSE, door.get_state());
    door.transition("preOpen");
    ASSERT_EQ(1, door.get_context()->get_degree_of_opening());
    ASSERT_EQ(states::AJAR, door.get_state());
    door.transition("ajarPlus");
    ASSERT_EQ(states::AJAR, door.get_state());
    ASSERT_EQ(2, door.get_context()->get_degree_of_opening());
    while (door.get_context()->get_degree_of_opening() < 99) {
        door.transition("ajarPlus");
        ASSERT_EQ(states::AJAR, door.get_state());
    }

    door.transition("open");
    ASSERT_EQ(states::OPEN, door.get_state());
    ASSERT_EQ(100, door.get_context()->get_degree_of_opening());
}

void typed_test_schema(types_door_state_machine door) {
    ASSERT_EQ(states::OPEN, door.get_state());
    ASSERT_EQ(100, door.get_context()->get_degree_of_opening());
    door.pre_close();
    ASSERT_EQ(states::AJAR, door.get_state());
    ASSERT_EQ(99, door.get_context()->get_degree_of_opening());
    while (door.get_context()->get_degree_of_opening() > 1) {
        door.ajar_minus();
        ASSERT_EQ(states::AJAR, door.get_state());
    }

    ASSERT_EQ(1, door.get_context()->get_degree_of_opening());
    door.close();
    ASSERT_EQ(0, door.get_context()->get_degree_of_opening());
    ASSERT_EQ(states::CLOSE, door.get_state());
    door.pre_open();
    ASSERT_EQ(1, door.get_context()->get_degree_of_opening());
    ASSERT_EQ(states::AJAR, door.get_state());
    door.ajar_plus();
    ASSERT_EQ(states::AJAR, door.get_state());
    ASSERT_EQ(2, door.get_context()->get_degree_of_opening());
    while (door.get_context()->get_degree_of_opening() < 99) {
        door.ajar_plus();
        ASSERT_EQ(states::AJAR, door.get_state());
    }

    door.open();
    ASSERT_EQ(states::OPEN, door.get_state());
    ASSERT_EQ(100, door.get_context()->get_degree_of_opening());
}


TEST(state_machine_test, test_simple_build) {
    auto door_fsm = types_door_state_machine();
    test_door_schema(door_fsm);
    typed_test_schema(door_fsm);
}

TEST(state_machine_test, test_builder) {
    door_fsm_context context;
    auto door_fsm = stater_state_machine_builder<states, door_fsm_context>()
            .add_transition("preOpen", states::CLOSE, states::AJAR,
                            [](door_fsm_context *ctx) { return true; },
                            [](door_fsm_context *ctx) { ctx->set_degree_of_opening(1); })
            .add_transition("preClose", states::OPEN, states::AJAR,
                            [](door_fsm_context *ctx) { return true; },
                            [](door_fsm_context *ctx) { ctx->set_degree_of_opening(99); })
            .add_transition("open", states::AJAR, states::OPEN,
                            [](door_fsm_context *ctx) { return ctx->get_degree_of_opening() >= 99; },
                            [](door_fsm_context *ctx) { ctx->set_degree_of_opening(100); })
            .add_transition("close", states::AJAR, states::CLOSE,
                            [](door_fsm_context *ctx) { return ctx->get_degree_of_opening() <= 1; },
                            [](door_fsm_context *ctx) { ctx->set_degree_of_opening(0); })
            .add_transition("ajarPlus", states::AJAR, states::AJAR,
                            [](door_fsm_context *ctx) {
                                return 1 <= ctx->get_degree_of_opening() && ctx->get_degree_of_opening() <= 98;
                            },
                            [](door_fsm_context *ctx) { ctx->set_degree_of_opening(ctx->get_degree_of_opening() + 1); })
            .add_transition("ajarMinus", states::AJAR, states::AJAR,
                            [](door_fsm_context *ctx) {
                                return 2 <= ctx->get_degree_of_opening() && ctx->get_degree_of_opening() <= 99;
                            },
                            [](door_fsm_context *ctx) { ctx->set_degree_of_opening(ctx->get_degree_of_opening() - 1); })
            .set_context(&context)
            .set_start_state(states::OPEN)
            .build();

    test_door_schema(*door_fsm);
}

stater_state_machine_builder<states, door_fsm_context> structure_build() {
    return stater_state_machine_builder<states, door_fsm_context>()
            .add_transition("preOpen", states::CLOSE, states::AJAR)
            .add_transition("preClose", states::OPEN, states::AJAR)
            .add_transition("open", states::AJAR, states::OPEN)
            .add_transition("close", states::AJAR, states::CLOSE)
            .add_transition("ajarPlus", states::AJAR, states::AJAR)
            .add_transition("ajarMinus", states::AJAR, states::AJAR);
}

// Строим события и условия для FSM
stater_state_machine_builder<states, door_fsm_context> events_build(
    stater_state_machine_builder<states, door_fsm_context> &builder) {
    return builder
            .set_transition_event("preOpen", [](door_fsm_context *context) {
                context->set_degree_of_opening(1); // Используем указатель на объект
            })
            .set_transition_event("preClose", [](door_fsm_context *context) {
                context->set_degree_of_opening(99);
            })
            .set_transition_condition("open", [](door_fsm_context *context) {
                return context->get_degree_of_opening() >= 99;
            })
            .set_transition_event("open", [](door_fsm_context *context) {
                context->set_degree_of_opening(100);
            })
            .set_transition_condition("close", [](door_fsm_context *context) {
                return context->get_degree_of_opening() <= 1;
            })
            .set_transition_event("close", [](door_fsm_context *context) {
                context->set_degree_of_opening(0);
            })
            .set_transition_condition("ajarPlus", [](door_fsm_context *context) {
                return context->get_degree_of_opening() >= 1 && context->get_degree_of_opening() <= 98;
            })
            .set_transition_event("ajarPlus", [](door_fsm_context *context) {
                context->set_degree_of_opening(context->get_degree_of_opening() + 1);
            })
            .set_transition_condition("ajarMinus", [](door_fsm_context *context) {
                return context->get_degree_of_opening() >= 2 && context->get_degree_of_opening() <= 99;
            })
            .set_transition_event("ajarMinus", [](door_fsm_context *context) {
                context->set_degree_of_opening(context->get_degree_of_opening() - 1);
            });
}

TEST(state_machine_test, TestBuilder2) {
    door_fsm_context context;
    auto fsm = structure_build();
    auto door_fsm = events_build(fsm)
            .set_context(&context)
            .set_start_state(states::OPEN)
            .build();

    test_door_schema(*door_fsm);
}

state_machine_factory<states, door_fsm_context> typed_door_factory = [](
    const std::vector<Transition<states, door_fsm_context> > &transitions,
    door_fsm_context *context,
    states startState,
    const std::unordered_set<states> &states,
    const std::unordered_map<std::string, std::vector<::transition_middleware<door_fsm_context> > > &
    transitionMiddleware,
    const std::vector<transition_name_middleware<door_fsm_context> > &
    transitionAllMiddlewares,
    const std::unordered_map<std::string, std::vector<event<door_fsm_context> > > &
    transitionCallbacks,
    const std::vector<name_event<door_fsm_context> > &transition_all_callbacks,
    const std::unordered_map<::states, std::vector<event<door_fsm_context> > > &state_callbacks,
    const std::vector<state_event<::states, door_fsm_context> > &state_all_callbacks,
    std::shared_ptr<context_json_adapter<door_fsm_context> > context_json_adapter_) ->
    std::unique_ptr<types_door_state_machine> {
    return std::make_unique<types_door_state_machine>(
        transitions, context, startState, states,
        transitionMiddleware, transitionAllMiddlewares,
        transitionCallbacks, transition_all_callbacks,
        state_callbacks, state_all_callbacks, context_json_adapter_);
};

TEST(state_machine_test, TestAutoTransition) {
    door_fsm_context context;
    auto fsm = structure_build();
    const auto door_fsm = events_build(fsm)
            .set_context(&context)
            .set_start_state(states::OPEN)
            .build();

    door_fsm->auto_transition();
    ASSERT_EQ(states::AJAR, door_fsm->get_state());
}

TEST(state_machine_test, TestBuilderFactory) {
    door_fsm_context context;
    auto fsm = structure_build();
    const auto door_fsm = events_build(fsm)
            .set_context(&context)
            .set_start_state(states::OPEN)
            .set_factory(typed_door_factory)
            .build();
    test_door_schema(*door_fsm);

    auto typed_door_fsm = dynamic_cast<types_door_state_machine *>(door_fsm.get());
    ASSERT_NE(typed_door_fsm, nullptr);
    typed_test_schema(*typed_door_fsm);
}

TEST(state_machine_test, TestJsonSchema) {
    door_fsm_context context;
    auto fsm = structure_build();
    const auto door_fsm = events_build(fsm)
            .set_context(&context)
            .set_start_state(states::OPEN)
            .build();

    const auto json_schema = door_fsm->to_json_schema();

    auto builder = stater_state_machine_builder<states, door_fsm_context>()
            .from_json_schema(json_schema);
    auto events_builder = events_build(builder)
            .set_context(&context)
            .set_factory(typed_door_factory)
            .build();

    ASSERT_EQ(json_schema, events_builder->to_json_schema());
    test_door_schema(*events_builder);
    auto typed_door_fsm = dynamic_cast<types_door_state_machine *>(events_builder.get());
    ASSERT_NE(typed_door_fsm, nullptr);
    typed_test_schema(*typed_door_fsm);
}

TEST(state_machine_test, TestStringGeneric) {
    door_fsm_context context;
    auto fsm = structure_build();
    const auto door_fsm = events_build(fsm)
            .set_context(&context)
            .set_start_state(states::OPEN)
            .build();

    const auto json_schema = door_fsm->to_json_schema();

    auto builder = stater_state_machine_builder<std::string, door_fsm_context>()
            .from_json_schema(json_schema)
            .set_context(&context)
            .build();
    ASSERT_EQ(builder->get_state(), "OPEN");
}


class json_converter : public context_json_adapter<door_fsm_context> {
public:
    json_converter() = default;

    std::string to_json(door_fsm_context *context) const override {
        return std::to_string(context->get_degree_of_opening());
    }

    door_fsm_context *from_json(const std::string &json) const override {
        return new door_fsm_context(std::stoi(json));
    }
};

TEST(state_machine_test, TestJsonDump) {
    door_fsm_context context;
    const std::shared_ptr<json_converter> adapter = std::make_unique<json_converter>();
    auto fsm = structure_build();
    const auto door_fsm = events_build(fsm)
            .set_context(&context)
            .set_start_state(states::OPEN)
            .set_context_json_adapter(adapter)
            .build();

    ASSERT_EQ(door_fsm->get_state(), states::OPEN);
    ASSERT_EQ(door_fsm->get_context()->get_degree_of_opening(), 100);

    const auto dump = door_fsm->to_json();
    door_fsm->transition("preClose");

    ASSERT_EQ(door_fsm->get_state(), states::AJAR);
    ASSERT_EQ(door_fsm->get_context()->get_degree_of_opening(), 99);

    door_fsm->from_json(dump);
    ASSERT_EQ(door_fsm->get_state(), states::OPEN);
    ASSERT_EQ(door_fsm->get_context()->get_degree_of_opening(), 100);
}

TEST(state_machine_test, TestMiddlewareAndCallbacks) {
    door_fsm_context context;
    auto fsm = structure_build();


    int transition_middleware_count = 0;
    int transition_all_middleware_count = 0;
    int subscribe_on_transition_count = 0;
    int subscribe_on_all_transition_count = 0;
    int subscribe_on_state_count = 0;
    int subscribe_on_all_state_count = 0;

    const auto door_fsm = events_build(fsm)
            .set_context(&context)
            .set_start_state(states::OPEN)
            .transition_middleware(
                "open", [&transition_middleware_count](door_fsm_context *ctx, const event<door_fsm_context> &next) {
                    transition_middleware_count++;
                    next(ctx);
                })
            .transition_all_middleware(
                [&transition_all_middleware_count](std::string name, door_fsm_context *ctx,
                                                const name_event<door_fsm_context> &next) {
                    transition_all_middleware_count++;
                    next(name, ctx);
                })
            .subscribe_on_transition("open", [&subscribe_on_transition_count](door_fsm_context *ctx) {
                subscribe_on_transition_count++;
            })
            .subscribe_on_all_transition([&subscribe_on_all_transition_count](std::string name, door_fsm_context *ctx) {
                subscribe_on_all_transition_count++;
            })
            .subscribe_on_state(states::AJAR, [&subscribe_on_state_count](door_fsm_context *ctx) {
                subscribe_on_state_count++;
            })
            .subscribe_on_all_state([&subscribe_on_all_state_count](states name, door_fsm_context *ctx) {
                subscribe_on_all_state_count++;
            })
            .build();

    test_door_schema(*door_fsm);

    ASSERT_EQ(transition_middleware_count, 1);
    ASSERT_EQ(transition_all_middleware_count, 200);
    ASSERT_EQ(subscribe_on_transition_count, 1);
    ASSERT_EQ(subscribe_on_all_transition_count, 200);
    ASSERT_EQ(subscribe_on_state_count, 198);
    ASSERT_EQ(subscribe_on_all_state_count, 200);
}

int main(int argc, char **argv) {
    ::testing::InitGoogleTest(&argc, argv);
    return RUN_ALL_TESTS();
}
