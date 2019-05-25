cmake_minimum_required(VERSION 3.4.1)

SET(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -std=c11 -Wall -Wextra -fvisibility=hidden")

aux_source_directory(src SRC_LIST)
add_library(yahfa STATIC ${SRC_LIST})

find_library(log-lib log)
target_link_libraries(yahfa ${log-lib})
target_include_directories(yahfa PUBLIC include)