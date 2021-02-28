/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2020 EdXposed Contributors
 * Copyright (C) 2021 LSPosed Contributors
 */

#pragma once

#include <string>
#include <filesystem>


class RirudSocket {
public:
//    class DirIter {};

    class RirudSocketException : public std::runtime_error {
    public:
        RirudSocketException(const std::string &what) : std::runtime_error(what) {}
    };

    RirudSocket();

    std::string ReadFile(const std::filesystem::path &path);

//    DirIter ReadDir(const std::filesystem::path &path);
//    DirIter RecursiveReadDir(const std::filesystem::path &path);

    ~RirudSocket();

private:
    RirudSocket(const RirudSocket &) = delete;

    RirudSocket operator=(const RirudSocket &) = delete;


    inline static const uint32_t ACTION_READ_FILE = 4;

    template<typename T>
    void Write(const T &);

    template<typename T>
    void Read(T &);

    void Write(const char *buf, size_t len) const;

    void Read(char *buf, size_t len) const;

    int fd_ = -1;
};
