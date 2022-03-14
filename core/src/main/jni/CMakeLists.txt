project(lspd)
cmake_minimum_required(VERSION 3.4.1)


find_program(CCACHE ccache)

if (CCACHE)
    set(CMAKE_CXX_COMPILER_LAUNCHER ${CCACHE})
    set(CMAKE_C_COMPILER_LAUNCHER ${CCACHE})
endif ()

find_package(cxx REQUIRED CONFIG)
link_libraries(cxx::cxx)

cmake_minimum_required(VERSION 3.4.1)

macro(SET_OPTION option value)
    set(${option} ${value} CACHE INTERNAL "" FORCE)
endmacro()

SET_OPTION(DOBBY_GENERATE_SHARED OFF)
if (NOT CMAKE_BUILD_TYPE STREQUAL "Debug")
    SET_OPTION(DOBBY_DEBUG OFF)
endif ()
add_subdirectory(${EXTERNAL_ROOT}/lsplant/lsplant/src/main/jni lsplant)
add_subdirectory(${EXTERNAL_ROOT}/dobby dobby)


configure_file(template/config.cpp src/config.cpp)

aux_source_directory(src SRC_LIST)
aux_source_directory(src/jni SRC_LIST)
if (${API} STREQUAL "riru")
    set(SRC_LIST ${SRC_LIST} api/riru_main.cpp)
elseif (${API} STREQUAL "zygisk")
    set(SRC_LIST ${SRC_LIST} api/zygisk_main.cpp)
endif()

add_library(${PROJECT_NAME} SHARED ${SRC_LIST} ${CMAKE_CURRENT_BINARY_DIR}/src/config.cpp)

target_include_directories(${PROJECT_NAME} PUBLIC include)
target_include_directories(${PROJECT_NAME} PRIVATE src)


find_package(riru REQUIRED CONFIG)
target_link_libraries(${PROJECT_NAME} riru::riru dobby dex_builder_static log lsplant_static)

if (NOT DEFINED DEBUG_SYMBOLS_PATH)
    set(DEBUG_SYMBOLS_PATH ${CMAKE_BINARY_DIR}/symbols)
endif()

add_custom_command(TARGET ${PROJECT_NAME} POST_BUILD
        COMMAND ${CMAKE_COMMAND} -E make_directory ${DEBUG_SYMBOLS_PATH}/${ANDROID_ABI}
        COMMAND ${CMAKE_OBJCOPY} --only-keep-debug $<TARGET_FILE:${PROJECT_NAME}>
        ${DEBUG_SYMBOLS_PATH}/${ANDROID_ABI}/${PROJECT_NAME}
        COMMAND ${CMAKE_STRIP} --strip-all $<TARGET_FILE:${PROJECT_NAME}>)
