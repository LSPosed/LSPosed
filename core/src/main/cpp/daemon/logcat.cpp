#include <jni.h>
#include <unistd.h>
#include <string>
#include <android/log.h>
#include <array>
#include <cinttypes>
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

    UniqueFile() : UniqueFile(stdout) {};
};

class Logcat {
public:
    explicit Logcat(JNIEnv *env, jobject thiz, jmethodID method) :
            env_(env), thiz_(thiz), refresh_fd_method_(method) {}

    [[noreturn]] void Run();

private:
    inline void RefreshFd(bool is_verbose);

    void ProcessBuffer(struct log_msg *buf);

    static int PrintLogLine(const AndroidLogEntry &entry, FILE *out);

    JNIEnv *env_;
    jobject thiz_;
    jmethodID refresh_fd_method_;
    std::string id_ = "0";

    UniqueFile modules_file_{};
    size_t modules_file_part_ = 0;
    size_t modules_print_count_ = 0;

    UniqueFile verbose_file_{};
    size_t verbose_file_part_ = 0;
    size_t verbose_print_count_ = 0;

    bool verbose_ = true;

    const std::string start_verbose_inst_ = "!!start_verbose!!";
    const std::string stop_verbose_inst_ = "!!stop_verbose!!";
};

int Logcat::PrintLogLine(const AndroidLogEntry &entry, FILE *out) {
    if (!out) return 0;
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
    return fprintf(out, "[ %s.%03ld %8d:%6d:%6d %c/%-15.*s ] %.*s\n",
                   time_buff.data(),
                   nsec / MS_PER_NSEC,
                   entry.uid, entry.pid, entry.tid,
                   kLogChar[entry.priority], static_cast<int>(entry.tagLen),
                   entry.tag, static_cast<int>(message_len), message);
}

void Logcat::RefreshFd(bool is_verbose) {
    if (is_verbose) {
        verbose_print_count_ = 0;
        fprintf(verbose_file_.get(), "----%s-%zu end----\n", id_.data(), verbose_file_part_);
        verbose_file_ = UniqueFile(env_->CallIntMethod(thiz_, refresh_fd_method_, JNI_TRUE), "a");
        verbose_file_part_++;
        fprintf(verbose_file_.get(), "----%s-%zu start----\n", id_.data(), verbose_file_part_);
    } else {
        modules_print_count_ = 0;
        fprintf(modules_file_.get(), "----%zu end----\n", modules_file_part_);
        modules_file_ = UniqueFile(env_->CallIntMethod(thiz_, refresh_fd_method_, JNI_FALSE), "a");
        modules_file_part_++;
        fprintf(modules_file_.get(), "----%zu start----\n", modules_file_part_);
    }
}

void Logcat::ProcessBuffer(struct log_msg *buf) {
    AndroidLogEntry entry;
    if (android_log_processLogBuffer(&buf->entry, &entry) < 0) return;

    std::string_view tag(entry.tag);
    bool shortcut = false;
    if (tag == "LSPosed-Bridge" || tag == "XSharedPreferences") [[unlikely]] {
        modules_print_count_ += PrintLogLine(entry, modules_file_.get());
        shortcut = true;
    }
    if (verbose_ && (shortcut || buf->id() == log_id::LOG_ID_CRASH ||
                     tag == "Magisk" ||
                     tag.starts_with("Riru") ||
                     tag.starts_with("LSPosed"))) [[unlikely]] {
        verbose_print_count_ += PrintLogLine(entry, verbose_file_.get());
    }
    if (entry.pid == getpid() && tag == "LSPosedLogcat") [[unlikely]] {
        if (std::string_view(entry.message).starts_with(start_verbose_inst_)) {
            verbose_ = true;
            id_ = std::string(entry.message, start_verbose_inst_.length(), std::string::npos);
            verbose_print_count_ += PrintLogLine(entry, verbose_file_.get());
        } else if (std::string_view(entry.message) == stop_verbose_inst_ + id_) {
            verbose_ = false;
        }
    }
}

void Logcat::Run() {
    constexpr size_t tail_after_crash = 10U;
    size_t tail = 0;
    RefreshFd(true);
    RefreshFd(false);
    while (true) {
        std::unique_ptr<logger_list, decltype(&android_logger_list_free)> logger_list{
                android_logger_list_alloc(0, tail, 0), &android_logger_list_free};
        tail = tail_after_crash;

        for (log_id id:{LOG_ID_MAIN, LOG_ID_CRASH}) {
            auto *logger = android_logger_open(logger_list.get(), id);
            if (logger == nullptr) continue;
            android_logger_set_log_size(logger, kMaxLogSize);
        }

        struct log_msg msg{};

        while (true) {
            if (android_logger_list_read(logger_list.get(), &msg) <= 0) [[unlikely]] break;

            ProcessBuffer(&msg);

            fflush(verbose_file_.get());
            fflush(modules_file_.get());

            if (verbose_print_count_ >= kMaxLogSize) [[unlikely]] RefreshFd(true);
            if (modules_print_count_ >= kMaxLogSize) [[unlikely]] RefreshFd(false);
        }
        fprintf(verbose_file_.get(), "\nLogd maybe crashed, retrying in 1s...\n");
        fprintf(modules_file_.get(), "\nLogd maybe crashed, retrying in 1s...\n");
        sleep(1);
    }
}

extern "C"
JNIEXPORT void JNICALL
// NOLINTNEXTLINE
Java_org_lsposed_lspd_service_LogcatService_runLogcat(JNIEnv *env, jobject thiz) {
    jclass clazz = env->GetObjectClass(thiz);
    jmethodID method = env->GetMethodID(clazz, "refreshFd", "(Z)I");
    Logcat logcat(env, thiz, method);
    logcat.Run();
}
