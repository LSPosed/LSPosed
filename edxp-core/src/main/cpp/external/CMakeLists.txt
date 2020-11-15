cmake_minimum_required(VERSION 3.4.1)

add_subdirectory(xhook)
add_subdirectory(riru)
add_subdirectory(yahfa)
add_subdirectory(substrate)
add_subdirectory(android)

macro(SET_OPTION option value)
    set(${option} ${value} CACHE INTERNAL "" FORCE)
endmacro()

SET_OPTION(DOBBY_GENERATE_SHARED OFF)
add_subdirectory(Dobby)
target_include_directories(dobby PUBLIC Dobby/include)
target_include_directories(dobby PUBLIC Dobby/builtin-plugin/AndroidRestriction)