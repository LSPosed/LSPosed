cmake_minimum_required(VERSION 3.4.1)

add_library(yahfa STATIC src/HookMain.cpp src/trampoline.c)

find_library(log-lib log)
target_link_libraries(yahfa ${log-lib})
target_include_directories(yahfa PUBLIC include)
