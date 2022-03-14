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

add_custom_command(TARGET ${PROJECT_NAME} POST_BUILD
        COMMAND ${CMAKE_COMMAND} -E make_directory ${DEBUG_SYMBOLS_PATH}/${ANDROID_ABI}
        COMMAND ${CMAKE_OBJCOPY} --only-keep-debug $<TARGET_FILE:${PROJECT_NAME}>
        ${DEBUG_SYMBOLS_PATH}/${ANDROID_ABI}/${PROJECT_NAME}
        COMMAND ${CMAKE_STRIP} --strip-all $<TARGET_FILE:${PROJECT_NAME}>)
