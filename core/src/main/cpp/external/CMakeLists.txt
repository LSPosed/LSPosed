cmake_minimum_required(VERSION 3.4.1)

add_subdirectory(yahfa)

add_subdirectory(DexBuilder)
target_include_directories(dex_builder PUBLIC DexBuilder)

macro(SET_OPTION option value)
    set(${option} ${value} CACHE INTERNAL "" FORCE)
endmacro()

SET_OPTION(DOBBY_GENERATE_SHARED OFF)
if (NOT CMAKE_BUILD_TYPE STREQUAL "Debug")
    SET_OPTION(DOBBY_DEBUG OFF)
endif (NOT CMAKE_BUILD_TYPE STREQUAL "Debug")
add_subdirectory(Dobby)
target_include_directories(dobby PUBLIC Dobby/include)

add_library(libcxx STATIC IMPORTED GLOBAL)
set_property(TARGET libcxx PROPERTY IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/../../../../build/intermediates/ndk/obj/local/${CMAKE_ANDROID_ARCH_ABI}/libcxx.a)
