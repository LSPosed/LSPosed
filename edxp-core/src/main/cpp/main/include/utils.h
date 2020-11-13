//
// Created by loves on 11/13/2020.
//

#ifndef EDXPOSED_UTILS_H
#define EDXPOSED_UTILS_H

#include <string>

namespace edxp{
inline const std::string operator ""_str(const char *str, std::size_t size) {
    return {str, size};
}
}
#endif //EDXPOSED_UTILS_H
