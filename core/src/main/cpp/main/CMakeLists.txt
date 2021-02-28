##
## This file is part of LSPosed.
##
## LSPosed is free software: you can redistribute it and/or modify
## it under the terms of the GNU General Public License as published by
## the Free Software Foundation, either version 3 of the License, or
## (at your option) any later version.
##
## LSPosed is distributed in the hope that it will be useful,
## but WITHOUT ANY WARRANTY; without even the implied warranty of
## MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
## GNU General Public License for more details.
##
## You should have received a copy of the GNU General Public License
## along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
##
## Copyright (C) 2020 EdXposed Contributors
## Copyright (C) 2021 LSPosed Contributors
##

cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_ANDROID_STL_TYPE c++_static)

aux_source_directory(src SRC_LIST)
aux_source_directory(src/jni SRC_JNI_LIST)
include_directories(include src)
add_library(riru_lspd SHARED ${SRC_LIST} ${SRC_JNI_LIST})

find_package(riru REQUIRED CONFIG)
find_library(log-lib log)
target_link_libraries(riru_lspd yahfa riru::riru android dobby sandhook.lspd dex_builder ${log-lib})