#!/bin/bash

INPUT_PATH="$1"

echo "add_executable(
    generated_test
    tests/test_door.cpp" >> "CMakeLists.txt"

for dir in "$INPUT_PATH"/*/; do
  dirname=$(basename "$dir")
  echo "    tests/test_${dirname}.cpp" >> "CMakeLists.txt"
done

echo ")" >> "CMakeLists.txt"

for dir in "$INPUT_PATH"/*/; do
  dirname=$(basename "$dir")
  echo "target_include_directories(generated_test PRIVATE \${CMAKE_SOURCE_DIR}/fsm/${dirname})" >> "CMakeLists.txt"
done

echo "target_include_directories(generated_test PRIVATE \${CMAKE_SOURCE_DIR}/src)" >> "CMakeLists.txt"
echo "target_link_libraries(generated_test PRIVATE nlohmann_json::nlohmann_json gtest)" >> "CMakeLists.txt"
echo "add_test(NAME generated_test COMMAND generated_test)" >> "CMakeLists.txt"