#include <jni.h>
#include <unistd.h>
#include <string>
#include <android/log.h>
#include <array>

#include "logcat.h"

class Logcat {
public:
    explicit Logcat(JNIEnv *env, jobject thiz, jmethodID method, jlong tid) :
            env_(env), thiz_(thiz), refresh_fd_method_(method), tid_(tid) { RefreshFd(); };

    void Run();

private:
    void RefreshFd();

    bool ProcessBuffer(struct log_msg *buf);

    static char FilterPriToChar(android_LogPriority pri);

    static int PrintLogLine(FILE *out, const AndroidLogEntry *entry);

    JNIEnv *env_;
    jobject thiz_;
    jmethodID refresh_fd_method_;
    jlong tid_;

    FILE *out_file_{nullptr};
    size_t print_count_ = 0;

    constexpr static size_t kMaxCount = 500;
    constexpr static uint32_t kSetLogSize = 32 * 1024 * 1024;
};

char Logcat::FilterPriToChar(android_LogPriority pri) {
    switch (pri) {
        case ANDROID_LOG_VERBOSE:
            return 'V';
        case ANDROID_LOG_DEBUG:
            return 'D';
        case ANDROID_LOG_INFO:
            return 'I';
        case ANDROID_LOG_WARN:
            return 'W';
        case ANDROID_LOG_ERROR:
            return 'E';
        case ANDROID_LOG_FATAL:
            return 'F';
        case ANDROID_LOG_SILENT:
            return 'S';
        case ANDROID_LOG_DEFAULT:
        case ANDROID_LOG_UNKNOWN:
        default:
            return '?';
    }
}

int Logcat::PrintLogLine(FILE *out, const AndroidLogEntry *entry) {
    constexpr static size_t kMaxTimeBuff = 64;
    struct tm ptm{};
    std::array<char, kMaxTimeBuff> time_buff;

    auto now = entry->tv_sec;
    auto nsec = entry->tv_nsec;
    if (now < 0) {
        nsec = NS_PER_SEC - nsec;
    }
    localtime_r(&now, &ptm);
    strftime(time_buff.data(), time_buff.size(), "%Y-%m-%dT%H:%M:%S", &ptm);
    fprintf(out, "[ %s.%03ld %5d:%5d:%5d %c/%-15.*s ] %.*s\n", time_buff.data(), nsec / MS_PER_NSEC,
            entry->uid, entry->pid, entry->tid,
            FilterPriToChar(entry->priority), static_cast<int>(entry->tagLen), entry->tag,
            entry->messageLen, entry->message);
}

void Logcat::RefreshFd() {
    out_file_ = fdopen(env_->CallIntMethod(thiz_, refresh_fd_method_), "w");
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
        ++print_count_;
        PrintLogLine(out_file_, &entry);
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
        android_logger_set_log_size(logger, kSetLogSize);
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

        if (print_count_ > kMaxCount) {
            RefreshFd();
            print_count_ = 0;
        }
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_org_lsposed_lspd_service_LogcatService_runLogcat(JNIEnv *env, jobject thiz, jlong tid) {
    jclass clazz = env->GetObjectClass(thiz);
    jmethodID method = env->GetMethodID(clazz, "refreshFd", "()I");
    Logcat logcat(env, thiz, method, tid);
    logcat.Run();
}
