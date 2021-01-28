cmake_minimum_required(VERSION 3.4.1)

SET(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -std=c11 -Wall -Wextra -Werror -fvisibility=hidden")

aux_source_directory(src SRC_LIST)
add_library(xhook STATIC ${SRC_LIST})

find_library(log-lib log)
target_link_libraries(xhook ${log-lib})
target_include_directories(xhook INTERFACE include)