cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_ANDROID_STL_TYPE c++_static)

set(SRC_LIST
        main.cpp
        native_hook/native_hook.cpp
        native_hook/resource_hook.cpp
        native_hook/riru_hook.cpp
        java_hook/java_hook.cpp
        inject/framework_hook.cpp
        inject/config_manager.cpp
        )
include_directories(include .)
add_library(riru_edxp SHARED ${SRC_LIST})

find_library(log-lib log)
target_link_libraries(riru_edxp yahfa riru xhook substrate ${log-lib})