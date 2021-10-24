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
 * Copyright (C) 2021 LSPosed Contributors
 */

#include <sys/socket.h>
#include <fcntl.h>
#include <dlfcn.h>

#include "jni/zygisk.h"
#include "logging.h"
#include "context.h"
#include "config.h"

namespace lspd {
    namespace {
        ssize_t xsendmsg(int sockfd, const struct msghdr *msg, int flags) {
            int sent = sendmsg(sockfd, msg, flags);
            if (sent < 0) {
                PLOGE("sendmsg");
            }
            return sent;
        }

        ssize_t xrecvmsg(int sockfd, struct msghdr *msg, int flags) {
            int rec = recvmsg(sockfd, msg, flags);
            if (rec < 0) {
                PLOGE("recvmsg");
            }
            return rec;
        }

        // Read exact same size as count
        ssize_t xxread(int fd, void *buf, size_t count) {
            size_t read_sz = 0;
            ssize_t ret;
            do {
                ret = read(fd, (std::byte *) buf + read_sz, count - read_sz);
                if (ret < 0) {
                    if (errno == EINTR)
                        continue;
                    PLOGE("read");
                    return ret;
                }
                read_sz += ret;
            } while (read_sz != count && ret != 0);
            if (read_sz != count) {
                PLOGE("read (%zu != %zu)", count, read_sz);
            }
            return read_sz;
        }

        // Write exact same size as count
        ssize_t xwrite(int fd, const void *buf, size_t count) {
            size_t write_sz = 0;
            ssize_t ret;
            do {
                ret = write(fd, (std::byte *) buf + write_sz, count - write_sz);
                if (ret < 0) {
                    if (errno == EINTR)
                        continue;
                    PLOGE("write");
                    return ret;
                }
                write_sz += ret;
            } while (write_sz != count && ret != 0);
            if (write_sz != count) {
                PLOGE("write (%zu != %zu)", count, write_sz);
            }
            return write_sz;
        }

        int send_fds(int sockfd, void *cmsgbuf, size_t bufsz, const int *fds, int cnt) {
            iovec iov = {
                    .iov_base = &cnt,
                    .iov_len  = sizeof(cnt),
            };
            msghdr msg = {
                    .msg_iov        = &iov,
                    .msg_iovlen     = 1,
            };

            if (cnt) {
                msg.msg_control = cmsgbuf;
                msg.msg_controllen = bufsz;
                cmsghdr *cmsg = CMSG_FIRSTHDR(&msg);
                cmsg->cmsg_len = CMSG_LEN(sizeof(int) * cnt);
                cmsg->cmsg_level = SOL_SOCKET;
                cmsg->cmsg_type = SCM_RIGHTS;

                memcpy(CMSG_DATA(cmsg), fds, sizeof(int) * cnt);
            }

            return xsendmsg(sockfd, &msg, 0);
        }

        int send_fd(int sockfd, int fd) {
            if (fd < 0) {
                return send_fds(sockfd, nullptr, 0, nullptr, 0);
            }
            char cmsgbuf[CMSG_SPACE(sizeof(int))];
            return send_fds(sockfd, cmsgbuf, sizeof(cmsgbuf), &fd, 1);
        }

        void *recv_fds(int sockfd, char *cmsgbuf, size_t bufsz, int cnt) {
            iovec iov = {
                    .iov_base = &cnt,
                    .iov_len  = sizeof(cnt),
            };
            msghdr msg = {
                    .msg_iov        = &iov,
                    .msg_iovlen     = 1,
                    .msg_control    = cmsgbuf,
                    .msg_controllen = bufsz
            };

            xrecvmsg(sockfd, &msg, MSG_WAITALL);
            cmsghdr *cmsg = CMSG_FIRSTHDR(&msg);

            if (msg.msg_controllen != bufsz ||
                cmsg == nullptr ||
                cmsg->cmsg_len != CMSG_LEN(sizeof(int) * cnt) ||
                cmsg->cmsg_level != SOL_SOCKET ||
                cmsg->cmsg_type != SCM_RIGHTS) {
                return nullptr;
            }

            return CMSG_DATA(cmsg);
        }

        int recv_fd(int sockfd) {
            char cmsgbuf[CMSG_SPACE(sizeof(int))];

            void *data = recv_fds(sockfd, cmsgbuf, sizeof(cmsgbuf), 1);
            if (data == nullptr)
                return -1;

            int result;
            memcpy(&result, data, sizeof(int));
            return result;
        }

        int read_int(int fd) {
            int val;
            if (xxread(fd, &val, sizeof(val)) != sizeof(val))
                return -1;
            return val;
        }

        void write_int(int fd, int val) {
            if (fd < 0) return;
            xwrite(fd, &val, sizeof(val));
        }

        int allow_unload = 0;
    }

    int *allowUnload = &allow_unload;

    class ZygiskModule : public zygisk::ModuleBase {
        JNIEnv *env_;
        zygisk::Api *api_;

        void onLoad(zygisk::Api *api, JNIEnv *env) override {
            env_ = env;
            api_ = api;
            Context::GetInstance()->Init();

            auto companion = api->connectCompanion();
            if (companion == -1) {
                LOGE("Failed to connect to companion");
                return;
            }

            if (int fd = -1, size = 0; (size = read_int(companion)) > 0 &&
                                       (fd = recv_fd(companion)) != -1) {
                Context::GetInstance()->PreLoadDex(fd, size);
                close(fd);
            } else {
                LOGE("Failed to read dex fd");
            }
            close(companion);
        }

        void preAppSpecialize(zygisk::AppSpecializeArgs *args) override {
            Context::GetInstance()->OnNativeForkAndSpecializePre(
                    env_, args->uid, args->gids, args->nice_name,
                    args->is_child_zygote ? *args->is_child_zygote : false, args->app_data_dir);
        }

        void postAppSpecialize(const zygisk::AppSpecializeArgs *args) override {
            Context::GetInstance()->OnNativeForkAndSpecializePost(env_, args->nice_name,
                                                                  args->app_data_dir);
            if (*allowUnload) api_->setOption(zygisk::DLCLOSE_MODULE_LIBRARY);
        }

        void preServerSpecialize(zygisk::ServerSpecializeArgs *args) override {
            Context::GetInstance()->OnNativeForkSystemServerPre(env_);
        }

        void postServerSpecialize(const zygisk::ServerSpecializeArgs *args) override {
            Context::GetInstance()->OnNativeForkSystemServerPost(env_);
        }
    };

    std::tuple<int, std::size_t> InitCompanion() {
        LOGI("onModuleLoaded: welcome to LSPosed!");
        LOGI("onModuleLoaded: version v%s (%d)", versionName, versionCode);

        int (*ashmem_create_region)(const char *name, std::size_t size) = nullptr;
        int (*ashmem_set_prot_region)(int fd, int prot) = nullptr;

        std::string path = "/data/adb/modules/"s + lspd::moduleName + "/" + kDexPath;
        int fd = open(path.data(), O_RDONLY | O_CLOEXEC);
        if (fd < 0) {
            LOGE("Failed to load dex: %s", path.data());
            return {-1, 0};
        }
        auto fsize = lseek(fd, 0, SEEK_END);
        lseek(fd, 0, SEEK_SET);
        auto *cutils = dlopen("/system/lib" LP_SELECT("", "64") "/libcutils.so", 0);
        ashmem_create_region = cutils ? reinterpret_cast<decltype(ashmem_create_region)>(
                dlsym(cutils, "ashmem_create_region")) : nullptr;
        ashmem_set_prot_region = cutils ? reinterpret_cast<decltype(ashmem_set_prot_region)>(
                dlsym(cutils, "ashmem_set_prot_region")) : nullptr;
        int tmp_fd = -1;
        if (void *addr = nullptr;
                ashmem_create_region && ashmem_set_prot_region &&
                (tmp_fd = ashmem_create_region("lspd.dex", fsize)) > 0 &&
                (addr = mmap(nullptr, fsize, PROT_WRITE, MAP_SHARED, tmp_fd, 0)) &&
                read(fd, addr, fsize) > 0 && munmap(addr, fsize) == 0) {
            LOGD("using memfd");
            ashmem_set_prot_region(tmp_fd, PROT_READ);
            std::swap(fd, tmp_fd);
        } else {
            LOGD("using raw fd");
        }

        close(tmp_fd);
        dlclose(cutils);
        return {fd, fsize};
    }

    void CompanionEntry(int client) {
        using namespace std::string_literals;
        static auto[fd, size] = InitCompanion();
        if (fd > 0 && size > 0) {
            write_int(client, size);
            send_fd(client, fd);
        } else write_int(client, -1);
        close(client);
    }
} //namespace lspd

REGISTER_ZYGISK_MODULE(lspd::ZygiskModule);

REGISTER_ZYGISK_COMPANION(lspd::CompanionEntry);
