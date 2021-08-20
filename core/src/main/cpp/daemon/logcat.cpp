#include <jni.h>
#include <unistd.h>
#include <string>

#include <android/log.h>

#include "logcat.h"

class Logcat {
public:
    explicit Logcat(JNIEnv *env, jobject thiz, jmethodID method, jlong javaTid) :
            env(env), thiz(thiz), refreshFd_method(method), javaTid(javaTid) { RefreshFd(); };

    void Run();

private:
    void RefreshFd();

    bool ProcessBuffer(struct log_msg *buf);

    static char filterPriToChar(android_LogPriority pri);

    static int PrintLogLine(int fd, const AndroidLogEntry *entry);

    static char *formatLogLine(char *defaultBuffer,
                               size_t defaultBufferSize,
                               const AndroidLogEntry *entry,
                               size_t *p_outLength);

    JNIEnv *env;
    jobject thiz;
    jmethodID refreshFd_method;
    jlong javaTid;

    int output_fd = 0;
    size_t max_count = 500;
    size_t print_count = 0;
    unsigned long set_log_size = 32 * 1024 * 1024;
};

char Logcat::filterPriToChar(android_LogPriority pri) {
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

char *Logcat::formatLogLine(char *defaultBuffer,
                            size_t defaultBufferSize,
                            const AndroidLogEntry *entry,
                            size_t *p_outLength) {
    struct tm tmBuf{};
    struct tm *ptm;
    char timeBuf[64];
    char prefixBuf[128];
    char priChar;
    char *ret;
    time_t now;
    unsigned long nsec;
    priChar = filterPriToChar(entry->priority);
    size_t prefixLen = 0;
    size_t len;

    now = entry->tv_sec;
    nsec = entry->tv_nsec;
    if (now < 0) {
        nsec = NS_PER_SEC - nsec;
    }
    char uid[16];
    uid[0] = '\0';
    if (entry->uid >= 0) {
        snprintf(uid, sizeof(uid), "%5d:", entry->uid);
    } else {
        snprintf(uid, sizeof(uid), "      ");
    }
    ptm = localtime_r(&now, &tmBuf);
    strftime(timeBuf, sizeof(timeBuf), "%Y-%m-%dT%H:%M:%S", ptm);
    len = strlen(timeBuf);
    snprintf(timeBuf + len, sizeof(timeBuf) - len, ".%03ld", nsec / 1000000);

    len = snprintf(prefixBuf, sizeof(prefixBuf),
                   "[ %s %s%5d:%5d %c/%-8.*s ] ", timeBuf, uid, entry->pid, entry->tid, priChar,
                   (int) entry->tagLen, entry->tag);

    prefixLen += len;
    if (prefixLen >= sizeof(prefixBuf)) {
        prefixLen = sizeof(prefixBuf) - 1;
        prefixBuf[sizeof(prefixBuf) - 1] = '\0';
    }

    char *p;
    size_t bufferSize;
    bufferSize = prefixLen + 2;
    bufferSize += entry->messageLen;

    if (defaultBufferSize >= bufferSize) {
        ret = defaultBuffer;
    } else {
        ret = (char *) malloc(bufferSize);
        if (ret == nullptr) {
            return ret;
        }
    }

    ret[0] = '\0';
    p = ret;
    strcat(p, prefixBuf);
    p += prefixLen;
    strncat(p, entry->message, entry->messageLen);
    p += entry->messageLen;
    strcat(p, "\n");
    p += 1;

    if (p_outLength != nullptr) {
        *p_outLength = p - ret;
    }

    return ret;
}

int Logcat::PrintLogLine(int fd, const AndroidLogEntry *entry) {
    int ret;
    char defaultBuffer[512];
    char *outBuffer;
    size_t totalLen;
    outBuffer = formatLogLine(defaultBuffer, sizeof(defaultBuffer), entry, &totalLen);
    if (!outBuffer) return -1;
    do {
        ret = write(fd, outBuffer, totalLen);
    } while (ret < 0 && errno == EINTR);
    if (ret < 0) ret = 0;
    if (outBuffer != defaultBuffer) free(outBuffer);
    return ret;
}

void Logcat::RefreshFd() {
    output_fd = env->CallIntMethod(thiz, refreshFd_method);
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
        print_count++;
        PrintLogLine(output_fd, &entry);
    }
    if (entry.pid == getpid() &&
        tag == "LSPosedLogcat" &&
        std::string_view(entry.message) == "!!stop!!" + std::to_string(javaTid)) {
        return true;
    } else {
        return false;
    }
}

void Logcat::Run() {
    std::unique_ptr<logger_list, decltype(&android_logger_list_free)> logger_list{
            nullptr, &android_logger_list_free};

    logger_list.reset(android_logger_list_alloc(0, 0, 0));

    for (log_id id:{LOG_ID_MAIN, LOG_ID_CRASH}) {
        auto logger = android_logger_open(logger_list.get(), id);
        if (logger == nullptr) {
            continue;
        }
        android_logger_set_log_size(logger, set_log_size);
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

        if (print_count > max_count) {
            RefreshFd();
            print_count = 0;
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
