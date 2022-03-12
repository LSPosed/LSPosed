cmake_minimum_required(VERSION 3.4.1)
project(daemon)

add_definitions(-std=c++20)

find_program(CCACHE ccache)

if (CCACHE)
    set(CMAKE_CXX_COMPILER_LAUNCHER ${CCACHE})
    set(CMAKE_C_COMPILER_LAUNCHER ${CCACHE})
endif ()

find_package(cxx REQUIRED CONFIG)
link_libraries(cxx::cxx)

set(SOURCES
        logcat.cpp
        obfuscation.cpp
        )

add_library(${PROJECT_NAME} SHARED ${SOURCES})

target_include_directories(${PROJECT_NAME} PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})

add_subdirectory(${EXTERNAL_ROOT}/lsplant/lsplant/src/main/jni lsplant)

target_link_libraries(${PROJECT_NAME} PRIVATE lsplant_static dex_builder_static android log)
