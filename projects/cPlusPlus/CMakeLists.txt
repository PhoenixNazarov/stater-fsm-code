cmake_minimum_required(VERSION 3.30)
project(stater_state_machine)

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
)
target_link_libraries(stater_state_machine PRIVATE nlohmann_json::nlohmann_json)

add_executable(test_door tests/test_door.cpp)
target_include_directories(test_door PRIVATE ${CMAKE_SOURCE_DIR}/src)
target_link_libraries(test_door PRIVATE stater_state_machine nlohmann_json::nlohmann_json gtest)

target_compile_options(stater_state_machine PRIVATE --coverage)
target_link_options(stater_state_machine PRIVATE --coverage)

target_compile_options(test_door PRIVATE --coverage)
target_link_options(test_door PRIVATE --coverage)