#include <jni.h>
#include <unistd.h>
#include <string>
#include <android/log.h>
#include <array>
#include <inttypes.h>

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
            env_(env), thiz_(thiz), refresh_fd_method_(method), tid_(tid) {};

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
    size_t file_count_ = 1;
};

void Logcat::PrintLogLine(const AndroidLogEntry &entry) {
    if (!out_file_) return;
    constexpr static size_t kMaxTimeBuff = 64;
    struct tm tm{};
    std::array<char, kMaxTimeBuff> time_buff;

    auto now = entry.tv_sec;
    auto nsec = entry.tv_nsec;
    auto message_len = entry.messageLen;
    const auto *message = entry.message;
    if (now < 0) {
        nsec = NS_PER_SEC - nsec;
    }
    if (message_len >= 1 && message[message_len - 1] == '\n') {
        --message_len;
    }
    localtime_r(&now, &tm);
    strftime(time_buff.data(), time_buff.size(), "%Y-%m-%dT%H:%M:%S", &tm);
    print_count_ +=
            fprintf(out_file_.get(), "[ %s.%03ld %8d:%6d:%6d %c/%-15.*s ] %.*s\n",
                    time_buff.data(),
                    nsec / MS_PER_NSEC,
                    entry.uid, entry.pid, entry.tid,
                    kLogChar[entry.priority], static_cast<int>(entry.tagLen),
                    entry.tag, static_cast<int>(message_len), message);
}

void Logcat::RefreshFd() {
    print_count_ = 0;
    out_file_ = UniqueFile(env_->CallIntMethod(thiz_, refresh_fd_method_), "w");
    fprintf(out_file_.get(), "%" PRId64 "-%zu\n", tid_, file_count_);
    file_count_++;
}

bool Logcat::ProcessBuffer(struct log_msg *buf) {
    AndroidLogEntry entry;
    if (android_log_processLogBuffer(&buf->entry, &entry) < 0) return false;

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
    constexpr size_t tail_after_crash = 10U;
    size_t tail = 0;

    while (true) {
        std::unique_ptr<logger_list, decltype(&android_logger_list_free)> logger_list{
                android_logger_list_alloc(0, tail, 0), &android_logger_list_free};
        tail = tail_after_crash;

        for (log_id id:{LOG_ID_MAIN, LOG_ID_CRASH}) {
            auto *logger = android_logger_open(logger_list.get(), id);
            if (logger == nullptr) continue;
            android_logger_set_log_size(logger, kMaxLogSize);
        }

        RefreshFd();

        struct log_msg msg{};

        while (true) {
            if (android_logger_list_read(logger_list.get(), &msg) <= 0) [[unlikely]] break;

            if (ProcessBuffer(&msg)) [[unlikely]] return;

            fflush(out_file_.get());

            if (print_count_ >= kMaxLogSize) [[unlikely]] RefreshFd();
        }
        fprintf(out_file_.get(), "\nLogd maybe crashed, retrying in %" PRId64 "-%zu file after 1s\n",
                tid_, file_count_ + 1);
        sleep(1);
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
