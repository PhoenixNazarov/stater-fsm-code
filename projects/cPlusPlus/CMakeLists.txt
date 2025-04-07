cmake_minimum_required(VERSION 3.30)
project(stater_state_machine)
enable_testing()
set(CMAKE_CXX_STANDARD 20)

include(FetchContent)

FetchContent_Declare(
        json
        URL https://github.com/nlohmann/json/releases/download/v3.11.3/json.tar.xz
)
FetchContent_Declare(
        gtest
        GIT_REPOSITORY https://github.com/google/googletest.git
        GIT_TAG        v1.16.0
)

FetchContent_MakeAvailable(json)
FetchContent_MakeAvailable(gtest)

add_library(stater_state_machine
        src/stater_state_machine.h
        src/stater_state_machine.cpp
)
target_link_libraries(stater_state_machine PRIVATE nlohmann_json::nlohmann_json)

add_executable(fsm_tests tests/test_door.cpp)
target_include_directories(fsm_tests PRIVATE ${CMAKE_SOURCE_DIR}/src)
target_link_libraries(fsm_tests PRIVATE nlohmann_json::nlohmann_json gtest)
add_test(NAME fsm_tests COMMAND fsm_tests)
