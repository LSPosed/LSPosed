cmake_minimum_required(VERSION 3.4.1)

aux_source_directory(src SRC_LIST)
add_library(riru STATIC ${SRC_LIST})

target_include_directories(riru INTERFACE src)