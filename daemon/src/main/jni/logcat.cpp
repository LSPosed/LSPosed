#include "logcat.h"

#include <jni.h>
#include <unistd.h>
#include <string>
#include <android/log.h>
#include <array>
#include <cinttypes>
#include <chrono>
#include <thread>
#include <sys/system_properties.h>

using namespace std::string_view_literals;
using namespace std::chrono_literals;

constexpr size_t kMaxLogSize = 4 * 1024 * 1024;
constexpr size_t kLogBufferSize = 64 * 1024;

namespace {
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

    size_t ParseUint(const char *s) {
        if (s[0] == '\0') return -1;

        while (isspace(*s)) {
            s++;
        }

        if (s[0] == '-') {
            return -1;
        }

        int base = (s[0] == '0' && (s[1] == 'x' || s[1] == 'X')) ? 16 : 10;
        char *end;
        auto result = strtoull(s, &end, base);
        if (end == s) {
            return -1;
        }
        if (*end != '\0') {
            const char *suffixes = "bkmgtpe";
            const char *suffix;
            if ((suffix = strchr(suffixes, tolower(*end))) == nullptr ||
                __builtin_mul_overflow(result, 1ULL << (10 * (suffix - suffixes)), &result)) {
                return -1;
            }
        }
        if (std::numeric_limits<size_t>::max() < result) {
            return -1;
        }
        return static_cast<size_t>(result);
    }

    inline size_t GetByteProp(std::string_view prop, size_t def = -1) {
        std::array<char, PROP_VALUE_MAX> buf{};
        if (__system_property_get(prop.data(), buf.data()) < 0) return def;
        return ParseUint(buf.data());
    }

    inline std::string GetStrProp(std::string_view prop, std::string def = {}) {
        std::array<char, PROP_VALUE_MAX> buf{};
        if (__system_property_get(prop.data(), buf.data()) < 0) return def;
        return {buf.data()};
    }

    inline bool SetIntProp(std::string_view prop, int val) {
        auto buf = std::to_string(val);
        return __system_property_set(prop.data(), buf.data()) >= 0;
    }

    inline bool SetStrProp(std::string_view prop, std::string_view val) {
        return __system_property_set(prop.data(), val.data()) >= 0;
    }

}  // namespace

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

    inline void Log(std::string_view str);

    void ProcessBuffer(struct log_msg *buf);

    static size_t PrintLogLine(const AndroidLogEntry &entry, FILE *out);

    JNIEnv *env_;
    jobject thiz_;
    jmethodID refresh_fd_method_;

    UniqueFile modules_file_{};
    size_t modules_file_part_ = 0;
    size_t modules_print_count_ = 0;

    UniqueFile verbose_file_{};
    size_t verbose_file_part_ = 0;
    size_t verbose_print_count_ = 0;

    pid_t my_pid_ = getpid();

    bool verbose_ = true;
};

size_t Logcat::PrintLogLine(const AndroidLogEntry &entry, FILE *out) {
    if (!out) return 0;
    constexpr static size_t kMaxTimeBuff = 64;
    struct tm tm{};
    std::array<char, kMaxTimeBuff> time_buff{};

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
    int len = fprintf(out, "[ %s.%03ld %8d:%6d:%6d %c/%-15.*s ] %.*s\n",
                      time_buff.data(),
                      nsec / MS_PER_NSEC,
                      entry.uid, entry.pid, entry.tid,
                      kLogChar[entry.priority], static_cast<int>(entry.tagLen),
                      entry.tag, static_cast<int>(message_len), message);
    fflush(out);
    // trigger overflow when failed to generate a new fd
    if (len <= 0) len = kMaxLogSize;
    return static_cast<size_t>(len);
}

void Logcat::RefreshFd(bool is_verbose) {
    constexpr auto start = "----part %zu start----\n";
    constexpr auto end = "-----part %zu end----\n";
    if (is_verbose) {
        verbose_print_count_ = 0;
        fprintf(verbose_file_.get(), end, verbose_file_part_);
        fflush(verbose_file_.get());
        verbose_file_ = UniqueFile(env_->CallIntMethod(thiz_, refresh_fd_method_, JNI_TRUE), "a");
        verbose_file_part_++;
        fprintf(verbose_file_.get(), start, verbose_file_part_);
        fflush(verbose_file_.get());
    } else {
        modules_print_count_ = 0;
        fprintf(modules_file_.get(), end, modules_file_part_);
        fflush(modules_file_.get());
        modules_file_ = UniqueFile(env_->CallIntMethod(thiz_, refresh_fd_method_, JNI_FALSE), "a");
        modules_file_part_++;
        fprintf(modules_file_.get(), start, modules_file_part_);
        fflush(modules_file_.get());
    }
}

inline void Logcat::Log(std::string_view str) {
    if (verbose_) {
        fprintf(verbose_file_.get(), "%.*s", static_cast<int>(str.size()), str.data());
        fflush(verbose_file_.get());
    }
    fprintf(modules_file_.get(), "%.*s", static_cast<int>(str.size()), str.data());
    fflush(modules_file_.get());
}

void Logcat::ProcessBuffer(struct log_msg *buf) {
    AndroidLogEntry entry;
    if (android_log_processLogBuffer(&buf->entry, &entry) < 0) return;

    entry.tagLen--;

    std::string_view tag(entry.tag, entry.tagLen);
    bool shortcut = false;
    if (tag == "LSPosed-Bridge"sv || tag == "XSharedPreferences"sv) [[unlikely]] {
        modules_print_count_ += PrintLogLine(entry, modules_file_.get());
        shortcut = true;
    }
    if (verbose_ && (shortcut || buf->id() == log_id::LOG_ID_CRASH ||
                     entry.pid == my_pid_ || tag == "Magisk"sv || tag == "Dobby"sv ||
                     tag.starts_with("Riru"sv) || tag.starts_with("zygisk"sv) ||
                     tag == "LSPlant"sv || tag.starts_with("LSPosed"sv))) [[unlikely]] {
        verbose_print_count_ += PrintLogLine(entry, verbose_file_.get());
    }
    if (entry.pid == my_pid_ && tag == "LSPosedLogcat"sv) [[unlikely]] {
        std::string_view msg(entry.message, entry.messageLen);
        if (msg == "!!start_verbose!!"sv) {
            verbose_ = true;
            verbose_print_count_ += PrintLogLine(entry, verbose_file_.get());
        } else if (msg == "!!stop_verbose!!"sv) {
            verbose_ = false;
        } else if (msg == "!!refresh_modules!!"sv) {
            RefreshFd(false);
        } else if (msg == "!!refresh_verbose!!"sv) {
            RefreshFd(true);
        }
    }
}

void Logcat::Run() {
    constexpr size_t tail_after_crash = 10U;
    size_t tail = 0;
    RefreshFd(true);
    RefreshFd(false);
}

extern "C"
JNIEXPORT void JNICALL
// NOLINTNEXTLINE
Java_org_lsposed_lspd_service_LogcatService_runLogcat(JNIEnv *env, jobject thiz) {
    jclass clazz = env->GetObjectClass(thiz);
    jmethodID method = env->GetMethodID(clazz, "refreshFd", "(Z)I");
}
