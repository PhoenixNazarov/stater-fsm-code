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
