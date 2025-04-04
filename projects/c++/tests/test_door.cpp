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
    types_door_state_machine() = default;

    // : stater_state_machine(
        //     {
        //         // Transition<::states, door_fsm_context>("preOpen", states::CLOSE, states::AJAR,
        //         //                                        [](door_fsm_context *ctx) { return true; },
        //         //                                        [](door_fsm_context *ctx) { ctx->set_degree_of_opening(1); }),
        //         // Transition<::states, door_fsm_context>("preClose", states::OPEN, states::AJAR,
        //         //                                        [](door_fsm_context *ctx) { return true; },
        //         //                                        [](door_fsm_context *ctx) { ctx->set_degree_of_opening(99); }),
        //         // Transition<::states, door_fsm_context>("open", states::AJAR, states::OPEN,
        //         //                                        [](door_fsm_context *ctx) {
        //         //                                            return ctx->get_degree_of_opening() >= 99;
        //         //                                        },
        //         //                                        [](door_fsm_context *ctx) { ctx->set_degree_of_opening(100); }),
        //         // Transition<::states, door_fsm_context>("close", states::AJAR, states::CLOSE,
        //         //                                        [](door_fsm_context *ctx) {
        //         //                                            return ctx->get_degree_of_opening() <= 1;
        //         //                                        },
        //         //                                        [](door_fsm_context *ctx) { ctx->set_degree_of_opening(0); }),
        //         // Transition<::states, door_fsm_context>("ajarPlus", states::AJAR, states::AJAR,
        //         //                                        [](door_fsm_context *ctx) {
        //         //                                            return ctx->get_degree_of_opening() >= 1 && ctx->
        //         //                                                   get_degree_of_opening() <= 98;
        //         //                                        },
        //         //                                        [](door_fsm_context *ctx) {
        //         //                                            ctx->set_degree_of_opening(ctx->get_degree_of_opening() + 1);
        //         //                                        }),
        //         // Transition<::states, door_fsm_context>("ajarMinus", states::AJAR, states::AJAR,
        //         //                                        [](door_fsm_context *ctx) {
        //         //                                            return ctx->get_degree_of_opening() >= 2 && ctx->
        //         //                                                   get_degree_of_opening() <= 99;
        //         //                                        },
        //         //                                        [](door_fsm_context *ctx) {
        //         //                                            ctx->set_degree_of_opening(ctx->get_degree_of_opening() - 1);
        //         //                                        })
        //     },
        //     &door_fsm_context_,
        //     ::states::OPEN,
        //     {::states::OPEN, ::states::CLOSE, ::states::AJAR},
        //     {},
        //     {},
        //     {},
        //     {},
        //     {},
        //     {}
        // ) {

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

void test_door_schema(stater_state_machine<states, door_fsm_context> door) {
    // ASSERT_EQ(states::OPEN, door.get_state());
    // ASSERT_EQ(100, door.get_context().get_degree_of_opening());
    // door.transition("preClose");
    // ASSERT_EQ(states::AJAR, door.get_state());
    // ASSERT_EQ(99, door.get_context().get_degree_of_opening());
    // while (door.get_context().get_degree_of_opening() > 1) {
    //     door.transition("ajarMinus");
    //     ASSERT_EQ(states::AJAR, door.get_state());
    // }
    //
    // ASSERT_EQ(1, door.get_context().get_degree_of_opening());
    // door.transition("close");
    // ASSERT_EQ(0, door.get_context().get_degree_of_opening());
    // ASSERT_EQ(states::CLOSE, door.get_state());
    // door.transition("preOpen");
    // ASSERT_EQ(1, door.get_context().get_degree_of_opening());
    // ASSERT_EQ(states::AJAR, door.get_state());
    // door.transition("ajarPlus");
    // ASSERT_EQ(states::AJAR, door.get_state());
    // ASSERT_EQ(2, door.get_context().get_degree_of_opening());
    // while (door.get_context().get_degree_of_opening() < 99) {
    //     door.transition("ajarPlus");
    //     ASSERT_EQ(states::AJAR, door.get_state());
    // }
    //
    // door.transition("open");
    // ASSERT_EQ(states::OPEN, door.get_state());
    // ASSERT_EQ(100, door.get_context().get_degree_of_opening());
}

void typed_test_schema(types_door_state_machine door) {
    // ASSERT_EQ(states::OPEN, door.get_state());
    // ASSERT_EQ(100, door.get_context().get_degree_of_opening());
    // door.pre_close();
    // ASSERT_EQ(states::AJAR, door.get_state());
    // ASSERT_EQ(99, door.get_context().get_degree_of_opening());
    // while (door.get_context().get_degree_of_opening() > 1) {
    //     door.ajar_minus();
    //     ASSERT_EQ(states::AJAR, door.get_state());
    // }
    //
    // ASSERT_EQ(1, door.get_context().get_degree_of_opening());
    // door.close();
    // ASSERT_EQ(0, door.get_context().get_degree_of_opening());
    // ASSERT_EQ(states::CLOSE, door.get_state());
    // door.pre_open();
    // ASSERT_EQ(1, door.get_context().get_degree_of_opening());
    // ASSERT_EQ(states::AJAR, door.get_state());
    // door.ajar_plus();
    // ASSERT_EQ(states::AJAR, door.get_state());
    // ASSERT_EQ(2, door.get_context().get_degree_of_opening());
    // while (door.get_context().get_degree_of_opening() < 99) {
    //     door.ajar_plus();
    //     ASSERT_EQ(states::AJAR, door.get_state());
    // }
    //
    // door.open();
    // ASSERT_EQ(states::OPEN, door.get_state());
    // ASSERT_EQ(100, door.get_context().get_degree_of_opening());
}


TEST(state_machine_test, test_simple_build) {
    auto door_fsm = types_door_state_machine();
}

TEST(state_machine_test, test_builder) {
    // door_fsm_context context;
    //
    // auto door_fsm = stater_state_machine_builder<states, door_fsm_context>()
    //         .add_transition("preOpen", states::CLOSE, states::AJAR,
    //                         [](door_fsm_context *ctx) { return true; },
    //                         [](door_fsm_context *ctx) { ctx->set_degree_of_opening(1); })
    //         .add_transition("preClose", states::OPEN, states::AJAR,
    //                         [](door_fsm_context *ctx) { return true; },
    //                         [](door_fsm_context *ctx) { ctx->set_degree_of_opening(99); })
    //         .add_transition("open", states::AJAR, states::OPEN,
    //                         [](door_fsm_context *ctx) { return ctx->get_degree_of_opening() >= 99; },
    //                         [](door_fsm_context *ctx) { ctx->set_degree_of_opening(100); })
    //         .add_transition("close", states::AJAR, states::CLOSE,
    //                         [](door_fsm_context *ctx) { return ctx->get_degree_of_opening() <= 1; },
    //                         [](door_fsm_context *ctx) { ctx->set_degree_of_opening(0); })
    //         .add_transition("ajarPlus", states::AJAR, states::AJAR,
    //                         [](door_fsm_context *ctx) {
    //                             return 1 <= ctx->get_degree_of_opening() && ctx->get_degree_of_opening() <= 98;
    //                         },
    //                         [](door_fsm_context *ctx) { ctx->set_degree_of_opening(ctx->get_degree_of_opening() + 1); })
    //         .add_transition("ajarMinus", states::AJAR, states::AJAR,
    //                         [](door_fsm_context *ctx) {
    //                             return 2 <= ctx->get_degree_of_opening() && ctx->get_degree_of_opening() <= 99;
    //                         },
    //                         [](door_fsm_context *ctx) { ctx->set_degree_of_opening(ctx->get_degree_of_opening() - 1); })
    //         .set_context(&context)
    //         .set_start_state(states::OPEN)
    //         .build();
    //
    // test_door_schema(door_fsm);
}

TEST(state_machine_test, TestBuilder2) {
}

TEST(state_machine_test, TestAutoTransition) {
}

TEST(state_machine_test, TestBuilderFactory) {
}

TEST(state_machine_test, TestJsonSchema) {
}

TEST(state_machine_test, TestStringGeneric) {
}

TEST(state_machine_test, TestJsonDump) {
}

TEST(state_machine_test, TestMiddlewareAndCallbacks) {
}

int main(int argc, char **argv) {
    ::testing::InitGoogleTest(&argc, argv);
    return RUN_ALL_TESTS();
}
