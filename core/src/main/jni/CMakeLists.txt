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

add_library(lspd SHARED ${SRC_LIST} ${CMAKE_CURRENT_BINARY_DIR}/src/config.cpp)

target_include_directories(lspd PUBLIC include)
target_include_directories(lspd PRIVATE src)


find_package(riru REQUIRED CONFIG)
target_link_libraries(lspd riru::riru android dobby dex_builder log lsplant_static)
