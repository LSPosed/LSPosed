cmake_minimum_required(VERSION 3.4.1)

add_subdirectory(xhook)
add_subdirectory(yahfa)
add_subdirectory(android)

macro(SET_OPTION option value)
    set(${option} ${value} CACHE INTERNAL "" FORCE)
endmacro()

SET_OPTION(DOBBY_GENERATE_SHARED OFF)
SET_OPTION(Plugin.Android.BionicLinkerRestriction ON)
if (NOT CMAKE_BUILD_TYPE STREQUAL "Debug")
    SET_OPTION(DOBBY_DEBUG OFF)
endif (NOT CMAKE_BUILD_TYPE STREQUAL "Debug")
add_subdirectory(Dobby)
target_include_directories(dobby PUBLIC Dobby/include)
target_include_directories(dobby PUBLIC Dobby/builtin-plugin/BionicLinkerRestriction)