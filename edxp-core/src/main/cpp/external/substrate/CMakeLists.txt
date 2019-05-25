cmake_minimum_required(VERSION 3.4.1)

aux_source_directory(src SRC_LIST)
add_library(substrate STATIC ${SRC_LIST})

target_include_directories(substrate INTERFACE src)