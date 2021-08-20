#include <jni.h>
#include <unistd.h>
#include <string>
#include <android/log.h>
#include <array>

#include "logcat.h"

constexpr size_t kMaxLogSize = 32 * 1024 * 1024;

constexpr std::array<char, ANDROID_LOG_SILENT + 1> kLogChar = {
        /*ANDROID_LOG_UNKNOWN*/'?',
        /*ANDROID_LOG_DEFAULT*/ '?',
        /*ANDROID_LOG_VERBOSE*/ 'V',
        /*ANDROID_LOG_DEBUG*/ 'D',
        /*ANDROID_LOG_INFO*/'I',
        /*ANDROID_LOG_WARN*/'W',
        /*ANDROID_LOG_ERROR*/ 'E',
        /*ANDROID_LOG_FATAL*/ 'F',
        /*ANDROID_LOG_SILENT*/ 'S',
};

class UniqueFile : public std::unique_ptr<FILE, std::function<void(FILE *)>> {
    inline static deleter_type deleter = [](auto f) { f && f != stdout && fclose(f); };
public:
    explicit UniqueFile(FILE *f) : std::unique_ptr<FILE, std::function<void(FILE *)>>(f, deleter) {}

    UniqueFile(int fd, const char *mode) : UniqueFile(fd > 0 ? fdopen(fd, mode) : stdout) {};

    UniqueFile() : UniqueFile(nullptr) {};
};

class Logcat {
public:
    explicit Logcat(JNIEnv *env, jobject thiz, jmethodID method, jlong tid) :
            env_(env), thiz_(thiz), refresh_fd_method_(method), tid_(tid) { RefreshFd(); };

    void Run();

private:
    inline void RefreshFd();

    bool ProcessBuffer(struct log_msg *buf);

    void PrintLogLine(const AndroidLogEntry &entry);

    JNIEnv *env_;
    jobject thiz_;
    jmethodID refresh_fd_method_;
    jlong tid_;

    UniqueFile out_file_{};
    size_t print_count_ = 0;
};

void Logcat::PrintLogLine(const AndroidLogEntry &entry) {
    if (!out_file_) return;
    constexpr static size_t kMaxTimeBuff = 64;
    struct tm tm{};
    std::array<char, kMaxTimeBuff> time_buff;

    auto now = entry.tv_sec;
    auto nsec = entry.tv_nsec;
    if (now < 0) {
        nsec = NS_PER_SEC - nsec;
    }
    localtime_r(&now, &tm);
    strftime(time_buff.data(), time_buff.size(), "%Y-%m-%dT%H:%M:%S", &tm);
    print_count_ +=
            fprintf(out_file_.get(), "[ %s.%03ld %8d:%6d:%6d %c/%-15.*s ] %.*s\n",
                    time_buff.data(),
                    nsec / MS_PER_NSEC,
                    entry.uid, entry.pid, entry.tid,
                    kLogChar[entry.priority], static_cast<int>(entry.tagLen),
                    entry.tag,
                    static_cast<int>(entry.messageLen), entry.message);
}

void Logcat::RefreshFd() {
    out_file_ = UniqueFile(env_->CallIntMethod(thiz_, refresh_fd_method_), "w");
}

bool Logcat::ProcessBuffer(struct log_msg *buf) {
    int err;
    AndroidLogEntry entry;
    err = android_log_processLogBuffer(&buf->entry, &entry);
    if (err < 0) return false;

    std::string_view tag(entry.tag);
    if (buf->id() == log_id::LOG_ID_CRASH ||
        tag == "Magisk" ||
        tag.starts_with("Riru") ||
        tag.starts_with("LSPosed") ||
        tag == "XSharedPreferences") {
        PrintLogLine(entry);
    }
    return entry.pid == getpid() &&
           tag == "LSPosedLogcat" &&
           std::string_view(entry.message) == "!!stop!!" + std::to_string(tid_);
}

void Logcat::Run() {
    std::unique_ptr<logger_list, decltype(&android_logger_list_free)> logger_list{
            nullptr, &android_logger_list_free};

    logger_list.reset(android_logger_list_alloc(0, 0, 0));

    for (log_id id:{LOG_ID_MAIN, LOG_ID_CRASH}) {
        auto *logger = android_logger_open(logger_list.get(), id);
        if (logger == nullptr) {
            continue;
        }
        android_logger_set_log_size(logger, kMaxLogSize);
    }

    while (true) {
        struct log_msg msg{};
        int ret = android_logger_list_read(logger_list.get(), &msg);

        if (ret <= 0) {
            continue;
        }

        if (ProcessBuffer(&msg)) {
            break;
        }

        fflush(out_file_.get());

        if (print_count_ >= kMaxLogSize) {
            RefreshFd();
            print_count_ = 0;
        }
    }
}

extern "C"
JNIEXPORT void JNICALL
// NOLINTNEXTLINE
Java_org_lsposed_lspd_service_LogcatService_runLogcat(JNIEnv *env, jobject thiz, jlong tid) {
    jclass clazz = env->GetObjectClass(thiz);
    jmethodID method = env->GetMethodID(clazz, "refreshFd", "()I");
    Logcat logcat(env, thiz, method, tid);
    logcat.Run();
}
