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

#include "rirud_socket.h"
#include <unistd.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <logging.h>
#include <cerrno>

template<>
void RirudSocket::Write<std::string>(const std::string &str) {
    auto count = str.size();
    auto *buf = str.data();
    Write(buf, count);
}

template<typename T>
void RirudSocket::Write(const T &obj) {
    auto len = sizeof(T);
    auto *buf = &obj;
    Write(reinterpret_cast<const char*>(buf), len);
}

template<>
void RirudSocket::Read<std::string>(std::string &str) {
    auto count = str.size();
    auto *buf = str.data();
    Read(buf, count);
}

template<typename T>
void RirudSocket::Read(T &obj) {
    auto len = sizeof(T);
    auto *buf = &obj;
    Read(reinterpret_cast<char*>(buf), len);
}

RirudSocket::RirudSocket() {
    if ((fd_ = socket(PF_UNIX, SOCK_STREAM | SOCK_CLOEXEC, 0)) < 0) {
        throw RirudSocketException(strerror(errno));
    }

    struct sockaddr_un addr{
            .sun_family = AF_UNIX,
            .sun_path={0}
    };
    strcpy(addr.sun_path + 1, "rirud");
    socklen_t socklen = sizeof(sa_family_t) + strlen(addr.sun_path + 1) + 1;

    if (connect(fd_, reinterpret_cast<struct sockaddr *>(&addr), socklen) == -1) {
        close(fd_);
        fd_ = -1;
        throw RirudSocketException(strerror(errno));
    }
}

RirudSocket::~RirudSocket() {
    if (fd_ != -1)
        close(fd_);
}

std::string RirudSocket::ReadFile(const std::string &path) {
    Write(ACTION_READ_FILE);
    Write(static_cast<uint32_t>(path.size()));
    Write(path);
    int32_t rirud_errno;
    Read(rirud_errno);
    if(rirud_errno != 0) {
        throw RirudSocketException(strerror(rirud_errno));
    }
    uint32_t file_size;
    Read(file_size);
    std::string content;
    content.resize(file_size);
    Read(content);
    return content;
}

void RirudSocket::Write(const char *buf, size_t len) const {
    auto count = len;
    while (count > 0) {
        ssize_t size = write(fd_, buf, count < SSIZE_MAX ? count : SSIZE_MAX);
        if (size == -1) {
            if (errno == EINTR) continue;
            else throw RirudSocketException(strerror(errno));
        }
        buf = buf + size;
        count -= size;
    }
}

void RirudSocket::Read(char *out, size_t len) const {
    while (len > 0) {
        ssize_t ret = read(fd_, out, len);
        if (ret <= 0) {
            if(errno == EINTR) continue;
            else throw RirudSocketException(strerror(errno));
        }
        out = out + ret;
        len -= ret;
    }
}

