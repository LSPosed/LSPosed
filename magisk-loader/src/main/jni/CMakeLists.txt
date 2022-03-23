project(lspd)
cmake_minimum_required(VERSION 3.4.1)

add_subdirectory(${CORE_ROOT} core)

configure_file(template/loader.cpp src/loader.cpp)

aux_source_directory(src SRC_LIST)
if (${API} STREQUAL "riru")
    set(SRC_LIST ${SRC_LIST} api/riru_main.cpp)
elseif (${API} STREQUAL "zygisk")
    set(SRC_LIST ${SRC_LIST} api/zygisk_main.cpp)
endif()

add_library(${PROJECT_NAME} SHARED ${SRC_LIST} ${CMAKE_CURRENT_BINARY_DIR}/src/loader.cpp)

target_include_directories(${PROJECT_NAME} PUBLIC include)
target_include_directories(${PROJECT_NAME} PRIVATE src)

target_link_libraries(${PROJECT_NAME} core log)

if (DEFINED DEBUG_SYMBOLS_PATH)
    set(DEBUG_SYMBOLS_PATH ${DEBUG_SYMBOLS_PATH}/${API})
    message(STATUS "Debug symbols will be placed at ${DEBUG_SYMBOLS_PATH}")
    add_custom_command(TARGET ${PROJECT_NAME} POST_BUILD
            COMMAND ${CMAKE_COMMAND} -E make_directory ${DEBUG_SYMBOLS_PATH}/${ANDROID_ABI}
            COMMAND ${CMAKE_OBJCOPY} --only-keep-debug $<TARGET_FILE:${PROJECT_NAME}>
            ${DEBUG_SYMBOLS_PATH}/${ANDROID_ABI}/${PROJECT_NAME}.debug
            COMMAND ${CMAKE_STRIP} --strip-all $<TARGET_FILE:${PROJECT_NAME}>
            COMMAND ${CMAKE_OBJCOPY} --add-gnu-debuglink ${DEBUG_SYMBOLS_PATH}/${ANDROID_ABI}/${PROJECT_NAME}.debug
            $<TARGET_FILE:${PROJECT_NAME}>)
endif()
