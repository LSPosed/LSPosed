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

find_program(CCACHE ccache)

if (CCACHE)
    set(CMAKE_CXX_COMPILER_LAUNCHER ${CCACHE})
    set(CMAKE_C_COMPILER_LAUNCHER ${CCACHE})
endif ()

find_package(cxx REQUIRED CONFIG)
link_libraries(cxx::cxx)

add_subdirectory(main)
add_subdirectory(external)
