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
add_executable(
    generated_test
    tests/test_door.cpp
    tests/test_blizmhadwlqsljgkznyv.cpp
    tests/test_fcihdbdynyztpojyufoa.cpp
    tests/test_fzcjmdahoofbhzraiocx.cpp
    tests/test_jyzsfvpllxofscqzpios.cpp
    tests/test_ltumlfmckzvdbblnwgbk.cpp
    tests/test_lxtznplgcahqqtqaonqx.cpp
    tests/test_maomuzrpeiszqbyexyud.cpp
    tests/test_nerxytuqrhkgmwksgtoc.cpp
    tests/test_nyaduqfogdcrostqoows.cpp
    tests/test_qaeagmftbzatibxlbiaq.cpp
    tests/test_ufggekwfxmecdrnmzrfr.cpp
    tests/test_umkhaybuedccvdcsutwo.cpp
    tests/test_vebuliigbbwcngbzaudc.cpp
    tests/test_vxfagxrhfmvzuastbecu.cpp
    tests/test_zkjfhrnkspkehmoocmep.cpp
    tests/test_ztjjgxfzhysremhbjdka.cpp
)
target_include_directories(generated_test PRIVATE ${CMAKE_SOURCE_DIR}/fsm/blizmhadwlqsljgkznyv)
target_include_directories(generated_test PRIVATE ${CMAKE_SOURCE_DIR}/fsm/fcihdbdynyztpojyufoa)
target_include_directories(generated_test PRIVATE ${CMAKE_SOURCE_DIR}/fsm/fzcjmdahoofbhzraiocx)
target_include_directories(generated_test PRIVATE ${CMAKE_SOURCE_DIR}/fsm/jyzsfvpllxofscqzpios)
target_include_directories(generated_test PRIVATE ${CMAKE_SOURCE_DIR}/fsm/ltumlfmckzvdbblnwgbk)
target_include_directories(generated_test PRIVATE ${CMAKE_SOURCE_DIR}/fsm/lxtznplgcahqqtqaonqx)
target_include_directories(generated_test PRIVATE ${CMAKE_SOURCE_DIR}/fsm/maomuzrpeiszqbyexyud)
target_include_directories(generated_test PRIVATE ${CMAKE_SOURCE_DIR}/fsm/nerxytuqrhkgmwksgtoc)
target_include_directories(generated_test PRIVATE ${CMAKE_SOURCE_DIR}/fsm/nyaduqfogdcrostqoows)
target_include_directories(generated_test PRIVATE ${CMAKE_SOURCE_DIR}/fsm/qaeagmftbzatibxlbiaq)
target_include_directories(generated_test PRIVATE ${CMAKE_SOURCE_DIR}/fsm/ufggekwfxmecdrnmzrfr)
target_include_directories(generated_test PRIVATE ${CMAKE_SOURCE_DIR}/fsm/umkhaybuedccvdcsutwo)
target_include_directories(generated_test PRIVATE ${CMAKE_SOURCE_DIR}/fsm/vebuliigbbwcngbzaudc)
target_include_directories(generated_test PRIVATE ${CMAKE_SOURCE_DIR}/fsm/vxfagxrhfmvzuastbecu)
target_include_directories(generated_test PRIVATE ${CMAKE_SOURCE_DIR}/fsm/zkjfhrnkspkehmoocmep)
target_include_directories(generated_test PRIVATE ${CMAKE_SOURCE_DIR}/fsm/ztjjgxfzhysremhbjdka)
target_include_directories(generated_test PRIVATE ${CMAKE_SOURCE_DIR}/src)
target_link_libraries(generated_test PRIVATE nlohmann_json::nlohmann_json gtest)
add_test(NAME generated_test COMMAND generated_test)
