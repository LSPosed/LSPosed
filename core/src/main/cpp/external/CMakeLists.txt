cmake_minimum_required(VERSION 3.4.1)

add_subdirectory(yahfa)

add_subdirectory(DexBuilder)
target_include_directories(dex_builder PUBLIC DexBuilder)
