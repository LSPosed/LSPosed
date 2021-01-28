cmake_minimum_required(VERSION 3.4.1)

set(SRC_LIST
        android-base/logging.cpp
        android-base/file.cpp
        android-base/threads.cpp
        android-base/strings.cpp
        )
add_library(android STATIC ${SRC_LIST})

target_include_directories(android PUBLIC .)