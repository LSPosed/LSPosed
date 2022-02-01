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
#include "symbol_cache.h"

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

    class SharedMem {
        inline static void *cutils = nullptr;

        inline static int (*ashmem_create_region)(const char *name, std::size_t size) = nullptr;

        inline static int (*ashmem_set_prot_region)(int fd, int prot) = nullptr;

        inline static bool init = false;

        static void Init() {
            if (init) return;
            cutils = dlopen("/system/lib" LP_SELECT("", "64") "/libcutils.so", 0);
            ashmem_create_region = cutils ? reinterpret_cast<decltype(ashmem_create_region)>(
                    dlsym(cutils, "ashmem_create_region")) : nullptr;
            ashmem_set_prot_region = cutils ? reinterpret_cast<decltype(ashmem_set_prot_region)>(
                    dlsym(cutils, "ashmem_set_prot_region")) : nullptr;
            init = true;
        }

        int fd_ = -1;
        std::size_t size_ = 0;

        class MappedMem {
            void *addr_ = nullptr;
            std::size_t size_ = 0;

            friend class SharedMem;

            MappedMem(int fd, std::size_t size, int prot, int flags, off_t offset) : addr_(
                    mmap(nullptr, size, prot, flags, fd, offset)), size_(size) {
                if (addr_ == MAP_FAILED) {
                    PLOGE("failed to mmap");
                    addr_ = nullptr;
                    size_ = 0;
                }
            }

            MappedMem(const MappedMem &) = delete;

            MappedMem &operator=(const MappedMem &other) = delete;

        public:
            MappedMem(MappedMem &&other) : addr_(other.addr_), size_(other.size_) {
                other.addr_ = nullptr;
                other.size_ = 0;
            }

            MappedMem &operator=(MappedMem &&other) {
                new(this)MappedMem(std::move(other));
                return *this;
            }

            constexpr operator bool() { return addr_; }

            ~MappedMem() {
                if (addr_) {
                    munmap(addr_, size_);
                }
            }

            constexpr auto size() const { return size_; }

            constexpr auto get() const { return addr_; }
        };

    public:
        MappedMem map(int prot, int flags, off_t offset) {
            return {fd_, size_, prot, flags, offset};
        }

        constexpr bool ok() const { return fd_ > 0 && size_ > 0; }

        SharedMem(std::string_view name, std::size_t size) {
            Init();
            if (ashmem_create_region && (fd_ = ashmem_create_region(name.data(), size)) > 0) {
                size_ = size;
                LOGD("using memfd");
            } else {
                LOGD("using tmp file");
                auto *tmp = tmpfile();
                if (tmp) {
                    fd_ = fileno(tmp);
                    ftruncate(fd_, size);
                    size_ = size;
                }
            }
        }

        void SetProt(int prot) {
            ashmem_set_prot_region(fd_, prot);
        }

        SharedMem() : fd_(-1), size_(0) {
            Init();
        }

        constexpr auto get() const { return fd_; }

        constexpr auto size() const { return size_; }
    };

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
                // Context::GetInstance()->PreLoadDex(fd, size);
                // TODO: remove me
                close(fd);
            } else {
                LOGE("Failed to read dex fd");
            }
            if (int fd = -1, size = 0; (size = read_int(companion)) > 0 &&
                                       (fd = recv_fd(companion)) != -1) {
                if (auto addr = mmap(nullptr, size, PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
                        addr && addr != MAP_FAILED) {
                    InitSymbolCache(reinterpret_cast<SymbolCache *>(addr));
                    msync(addr, size, MS_SYNC);
                    munmap(addr, size);
                } else {
                    InitSymbolCache(nullptr);
                }
                close(fd);
            } else {
                LOGE("Failed to read symbol fd");
                InitSymbolCache(nullptr);
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
            if (__system_property_find("ro.vendor.product.ztename")) {
                auto *process = env_->FindClass("android/os/Process");
                auto *set_argv0 = env_->GetStaticMethodID(process, "setArgV0",
                                                          "(Ljava/lang/String;)V");
                env_->CallStaticVoidMethod(process, set_argv0, env_->NewStringUTF("system_server"));
            }
            Context::GetInstance()->OnNativeForkSystemServerPost(env_);
        }
    };

    std::tuple<SharedMem, SharedMem> InitCompanion() {
        LOGI("ZygiskCompanion: welcome to LSPosed!");
        LOGI("ZygiskCompanion: version v%s (%d)", versionName, versionCode);

        std::string path = "/data/adb/modules/"s + lspd::moduleName + "/" + kDexPath;
        int dex_fd = open(path.data(), O_RDONLY | O_CLOEXEC);
        if (dex_fd < 0) {
            PLOGE("Failed to load dex: %s", path.data());
            return {{}, {}};
        }
        size_t dex_size = lseek(dex_fd, 0, SEEK_END);
        lseek(dex_fd, 0, SEEK_SET);

        SharedMem dex{"lspd.dex", dex_size};
        SharedMem symbol{"symbol", sizeof(lspd::SymbolCache)};

        if (!dex.ok() || !symbol.ok()) {
            PLOGE("Failed to allocate shared mem");
            close(dex_fd);
            return {{}, {}};
        }

        if (auto dex_map = dex.map(PROT_WRITE, MAP_SHARED, 0); !dex_map ||
                                                               read(dex_fd, dex_map.get(),
                                                                    dex_map.size()) < 0) {
            PLOGE("Failed to read dex %p", dex_map.get());
            close(dex_fd);
            return {{}, {}};
        }

        dex.SetProt(PROT_READ);

        if (auto symbol_map = symbol.map(PROT_WRITE, MAP_SHARED, 0); symbol_map) {
            memcpy(symbol_map.get(), lspd::symbol_cache.get(), symbol_map.size());
        }

        close(dex_fd);
        return {std::move(dex), std::move(symbol)};
    }

    void CompanionEntry(int client) {
        using namespace std::string_literals;
        static auto[dex, symbol] = InitCompanion();
        LOGD("Got dex with fd=%d size=%d; Got cache with fd=%d size=%d", dex.get(),
             (int) dex.size(),
             symbol.get(), (int) symbol.size());
        if (dex.ok()) {
            write_int(client, dex.size());
            send_fd(client, dex.get());
        } else write_int(client, -1);
        if (symbol.ok()) {
            write_int(client, symbol.size());
            send_fd(client, symbol.get());
        } else write_int(client, -1);
        close(client);
    }
} //namespace lspd

REGISTER_ZYGISK_MODULE(lspd::ZygiskModule);

REGISTER_ZYGISK_COMPANION(lspd::CompanionEntry);
