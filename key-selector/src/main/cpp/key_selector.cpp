/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright (C) 2015-2016 The CyanogenMod Project
 * Copyright (C) 2021 LSPosed
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <cerrno>
#include <cstdlib>
#include <cstring>
#include <fcntl.h>
#include <filesystem>
#include <iostream>
#include <linux/input.h>
#include <sstream>
#include <sys/ioctl.h>
#include <sys/inotify.h>
#include <sys/poll.h>
#include <sys/system_properties.h>
#include <unistd.h>
#include <fstream>

#include "Languages.h"
#include "key_selector.h"

// Global variables
static struct pollfd *ufds;
static char **device_names;
static int nfds;

static int open_device(const char *device)
{
    int version;
    int fd;
    int clkid = CLOCK_MONOTONIC;
    struct pollfd *new_ufds;
    char **new_device_names;
    char name[80];
    char location[80];
    char idstr[80];
    input_id id{};

    fd = open(device, O_RDWR);
    if (fd < 0) {
        return -1;
    }

    if (ioctl(fd, EVIOCGVERSION, &version)) {
        return -1;
    }

    if (ioctl(fd, EVIOCGID, &id)) {
        return -1;
    }

    name[sizeof(name) - 1] = '\0';
    location[sizeof(location) - 1] = '\0';
    idstr[sizeof(idstr) - 1] = '\0';

    if (ioctl(fd, EVIOCGNAME(sizeof(name) - 1), &name) < 1) {
        name[0] = '\0';
    }

    if (ioctl(fd, EVIOCGPHYS(sizeof(location) - 1), &location) < 1) {
        location[0] = '\0';
    }

    if (ioctl(fd, EVIOCGUNIQ(sizeof(idstr) - 1), &idstr) < 1) {
        idstr[0] = '\0';
    }

    if (ioctl(fd, EVIOCSCLOCKID, &clkid) != 0) {
        // a non-fatal error
    }

    new_ufds = static_cast<pollfd *>(realloc(ufds, sizeof(ufds[0]) * (nfds + 1)));
    if (new_ufds == nullptr) {
        return -1;
    }

    ufds = new_ufds;
    new_device_names = static_cast<char **>(realloc(device_names,
                                                    sizeof(device_names[0]) * (nfds + 1)));
    if (new_device_names == nullptr) {
        return -1;
    }

    device_names = new_device_names;

    ufds[nfds].fd = fd;
    ufds[nfds].events = POLLIN;
    device_names[nfds] = strdup(device);
    nfds++;

    return 0;
}

int close_device(const char *device)
{
    int i;
    for (i = 1; i < nfds; i++) {
        if (strcmp(device_names[i], device) == 0) {
            int count = nfds - i - 1;
            free(device_names[i]);
            memmove(device_names + i, device_names + i + 1,
                    sizeof(device_names[0]) * count);
            memmove(ufds + i, ufds + i + 1, sizeof(ufds[0]) * count);
            nfds--;
            return 0;
        }
    }
    return -1;
}

static int read_notify(const char *dirname, int nfd)
{
    int res;
    char devname[PATH_MAX];
    char *filename;
    char event_buf[512];
    uint32_t event_size;
    int event_pos = 0;
    struct inotify_event *event;

    res = read(nfd, event_buf, sizeof(event_buf));
    if (res < (int)sizeof(*event)) {
        if (errno == EINTR) {
            return 0;
        }
        return 1;
    }

    strcpy(devname, dirname);
    filename = devname + strlen(devname);
    *filename++ = '/';

    while (res >= (int)sizeof(*event)) {
        event = (struct inotify_event *)(event_buf + event_pos);
        if (event->len) {
            strcpy(filename, event->name);
            if (event->mask & IN_CREATE) {
                open_device(devname);
            } else {
                close_device(devname);
            }
        }
        event_size = sizeof(*event) + event->len;
        res -= event_size;
        event_pos += event_size;
    }
    return 0;
}

static int scan_dir(const char *dirname)
{
    namespace fs = std::filesystem;
    try {
        for (auto &item: fs::directory_iterator(dirname)) {
            open_device(item.path().c_str());
        }
    } catch (const fs::filesystem_error &e) {
        std::cerr << e.what();
        return -1;
    }
    return 0;
}


uint32_t get_event() {
    int i;
    int res;
    input_event event{};
    const char *device_path = "/dev/input";
    unsigned char keys;

    keys = KEYCHECK_CHECK_VOLUMEDOWN | KEYCHECK_CHECK_VOLUMEUP;
    nfds = 1;
    ufds = static_cast<pollfd *>(calloc(1, sizeof(ufds[0])));
    ufds[0].fd = inotify_init();
    ufds[0].events = POLLIN;

    res = inotify_add_watch(ufds[0].fd, device_path, IN_DELETE | IN_CREATE);
    if (res < 0) {
        std::cerr << "inotify_add_watch failed" << std::endl;
        exit(1);
    }

    res = scan_dir(device_path);
    if (res < 0) {
        std::cerr << "scan dev failed" << std::endl;
        exit(1);
    }

    while (true) {
        poll(ufds, nfds, -1);
        if (ufds[0].revents & POLLIN) {
            read_notify(device_path, ufds[0].fd);
        }

        for (i = 1; i < nfds; i++) {
            if ((ufds[i].revents) && (ufds[i].revents & POLLIN)) {
                res = read(ufds[i].fd, &event, sizeof(event));
                if (res < (int)sizeof(event)) {
                    return 1;
                }

                // keypress only
                if (event.value == 1) {
                    if (event.code == KEY_VOLUMEDOWN &&
                        (keys & KEYCHECK_CHECK_VOLUMEDOWN) != 0) {
                        return KEYCHECK_PRESSED_VOLUMEDOWN;
                    }
                    else if (event.code == KEY_VOLUMEUP &&
                             (keys & KEYCHECK_CHECK_VOLUMEUP) != 0) {
                        return KEYCHECK_PRESSED_VOLUMEUP;
                    }
                }
            }
        }
    }
}

// for phone which has no button
uint16_t timeout = 10;
std::unique_ptr<Languages> l = nullptr;

int main() {
    if (getuid() != 0) {
        std::cerr << "Root required" << std::endl;
        exit(1);
    }

    // languages
    char locale[256];
    __system_property_get("persist.sys.locale", locale);
    if (locale[0] == 'z' && locale[1] == 'h') {
        l = std::make_unique<LanguageChinese>();
    } else {
        l = std::make_unique<Languages>();
    }

    // get current arch
#if defined(__arm__)
    const Arch arch = ARM;
#elif defined(__aarch64__)
    const Arch arch = ARM64;
#elif defined(__i386__)
    const Arch arch = x86;
#elif defined(__x86_64__)
    const Arch arch = x86_64;
#else
#error "Unsupported arch"
#endif

    std::unordered_map<Variant, VariantDetail> variants;
    for (const auto i: AllVariants) {
        switch (i) {
            case Variant::YAHFA:
                variants[i] = {
                        .expression = "YAHFA",
                        .supported_arch = {ARM, ARM64, x86, x86_64}
                };
                break;
            case Variant::SandHook:
                variants[i] = {
                        .expression = "SandHook",
                        .supported_arch = {ARM, ARM64}
                };
                break;
        }
    }

    Variant cursor = Variant::YAHFA;

    // Load current variant
    std::filesystem::path lspd_folder;
    bool found = false;
    for (auto &item: std::filesystem::directory_iterator("/data/misc/")) {
        if (item.is_directory() && item.path().string().starts_with("/data/misc/lspd")) {
            lspd_folder = item;
            found = true;
            break;
        }
    }

    if (found) {
        const auto variant_file = lspd_folder / "variant";
        if (std::filesystem::exists(variant_file)) {
            std::ifstream ifs(variant_file);
            if (ifs.good()) {
                std::string line;
                std::getline(ifs, line);
                char* end;
                int i = std::strtol(line.c_str(), &end, 10);
                switch (i) {
                    default:
                    case 1:
                        cursor = Variant::YAHFA;
                        break;
                    case 2:
                        cursor = Variant::SandHook;
                        break;
                }
                timeout = 5;
            }
        }
    }

    alarm(timeout);
    signal(SIGALRM, [](int){
        std::cout << l->timeout(timeout) << std::endl;
        exit(static_cast<int>(Variant::YAHFA));
    });

    auto print_status = [&cursor, variants, arch](){
        //std::cout << "\33[2K\r"; // clear this line
        std::stringstream ss;
        for (const auto &i: variants) {
            if (!i.second.supported_arch.contains(arch)) {
                continue;
            }
            ss << "[";
            ss << (cursor == i.first ? "√" : " ");
            ss << "] ";
            ss << i.second.expression;
            ss << " ";
        }
        std::cout << ss.str() << std::endl;
    };

    std::cout << l->desc_line_1() << std::endl;
    std::cout << l->desc_line_2(timeout) << std::endl;
    print_status();
    while (int event = get_event()) {
        bool leave = false;
        //std::cout << event << " " << cursor << std::endl;
        switch (event) {
            case KEYCHECK_PRESSED_VOLUMEUP:
                leave = true;
                break;
            case KEYCHECK_PRESSED_VOLUMEDOWN:
                cursor++;
                break;
            default:
                std::cout << "ERROR\n";
        }
        if (leave) {
            break;
        }
        print_status();
    }

    // std::cout << std::endl << cursor << std::endl;
    return static_cast<int>(cursor);
}