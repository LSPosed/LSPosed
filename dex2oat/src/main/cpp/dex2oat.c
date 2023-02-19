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
 * Copyright (C) 2022 LSPosed Contributors
 */

//
// Created by Nullptr on 2022/4/1.
//

#include <errno.h>
#include <stdio.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <unistd.h>

#include "logging.h"

#if defined(__LP64__)
# define LP_SELECT(lp32, lp64) lp64
#else
# define LP_SELECT(lp32, lp64) lp32
#endif

#define ID_VEC(is64, is_debug) (((is64) << 1) | (is_debug))

const char kSockName[] = "5291374ceda0aef7c5d86cd2a4f6a3ac\0";

static ssize_t xrecvmsg(int sockfd, struct msghdr *msg, int flags) {
    int rec = recvmsg(sockfd, msg, flags);
    if (rec < 0) {
        PLOGE("recvmsg");
    }
    return rec;
}

static void *recv_fds(int sockfd, char *cmsgbuf, size_t bufsz, int cnt) {
    struct iovec iov = {
            .iov_base = &cnt,
            .iov_len  = sizeof(cnt),
    };
    struct msghdr msg = {
            .msg_iov        = &iov,
            .msg_iovlen     = 1,
            .msg_control    = cmsgbuf,
            .msg_controllen = bufsz
    };

    xrecvmsg(sockfd, &msg, MSG_WAITALL);
    struct cmsghdr *cmsg = CMSG_FIRSTHDR(&msg);

    if (msg.msg_controllen != bufsz ||
        cmsg == NULL ||
        cmsg->cmsg_len != CMSG_LEN(sizeof(int) * cnt) ||
        cmsg->cmsg_level != SOL_SOCKET ||
        cmsg->cmsg_type != SCM_RIGHTS) {
        return NULL;
    }

    return CMSG_DATA(cmsg);
}

static int recv_fd(int sockfd) {
    char cmsgbuf[CMSG_SPACE(sizeof(int))];

    void *data = recv_fds(sockfd, cmsgbuf, sizeof(cmsgbuf), 1);
    if (data == NULL)
        return -1;

    int result;
    memcpy(&result, data, sizeof(int));
    return result;
}

static int read_int(int fd) {
    int val;
    if (read(fd, &val, sizeof(val)) != sizeof(val))
        return -1;
    return val;
}

static void write_int(int fd, int val) {
    if (fd < 0) return;
    write(fd, &val, sizeof(val));
}

int main(int argc, char **argv) {
    LOGD("dex2oat wrapper ppid=%d", getppid());
    struct sockaddr_un sock = {};
    sock.sun_family = AF_UNIX;
    strlcpy(sock.sun_path + 1, kSockName, sizeof(sock.sun_path) - 1);
    int sock_fd = socket(AF_UNIX, SOCK_STREAM, 0);
    size_t len = sizeof(sa_family_t) + strlen(sock.sun_path + 1) + 1;
    if (connect(sock_fd, (struct sockaddr *) &sock, len)) {
        PLOGE("failed to connect to %s", sock.sun_path + 1);
        return 1;
    }
    write_int(sock_fd, ID_VEC(LP_SELECT(0, 1), strstr(argv[0], "dex2oatd") != NULL));
    int stock_fd = recv_fd(sock_fd);
    read_int(sock_fd);
    close(sock_fd);
    LOGD("sock: %s %d", sock.sun_path + 1, stock_fd);

    const char *new_argv[argc + 2];
    for (int i = 0; i < argc; i++) new_argv[i] = argv[i];
    new_argv[argc] = "--inline-max-code-units=0";
    new_argv[argc + 1] = NULL;
    fexecve(stock_fd, (char **) new_argv, environ);
    PLOGE("fexecve failed");
    return 2;
}
