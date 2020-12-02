cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_ANDROID_STL_TYPE c++_static)

aux_source_directory(src SRC_LIST)
aux_source_directory(src/jni SRC_JNI_LIST)
include_directories(include src)
add_library(riru_edxp SHARED ${SRC_LIST} ${SRC_JNI_LIST})

find_package(riru REQUIRED CONFIG)
find_library(log-lib log)
target_link_libraries(riru_edxp yahfa riru::riru xhook android dobby ${log-lib})