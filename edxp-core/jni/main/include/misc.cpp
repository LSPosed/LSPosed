#include <sys/types.h>
#include <unistd.h>
#include <string>
#include <vector>
#include <mntent.h>
#include <jni.h>
#include <asm/fcntl.h>
#include <fcntl.h>
#include <dlfcn.h>
#include "java_hook/java_hook.h"
#include "include/logging.h"
#include "misc.h"

ssize_t fdgets(char *buf, const size_t size, int fd) {
    ssize_t len = 0;
    buf[0] = '\0';
    while (len < size - 1) {
        ssize_t ret = read(fd, buf + len, 1);
        if (ret < 0)
            return -1;
        if (ret == 0)
            break;
        if (buf[len] == '\0' || buf[len++] == '\n') {
            break;
        }
    }
    buf[len] = '\0';
    buf[size - 1] = '\0';
    return len;
}

char *get_cmdline_from_pid(pid_t pid, char *buf, size_t len) {
    char filename[32];
    if (pid < 1 || buf == NULL) {
        printf("%s: illegal para\n", __func__);
        return NULL;
    }

    snprintf(filename, 32, "/proc/%d/cmdline", pid);
    int read_ret = read_to_buf(filename, buf, len);
    if (read_ret < 0)
        return NULL;

    if (buf[read_ret - 1] == '\n')
        buf[--read_ret] = 0;

    char *name = buf;
    while (read_ret) {
        if (((unsigned char) *name) < ' ')
            *name = ' ';
        name++;
        read_ret--;
    }
    *name = 0;
    name = NULL;

    LOGV("cmdline:%s\n", buf);
    return buf;
}

int read_to_buf(const char *filename, void *buf, size_t len) {
    int fd;
    int ret;
    if (buf == NULL) {
        printf("%s: illegal para\n", __func__);
        return -1;
    }
    memset(buf, 0, len);
    fd = open(filename, O_RDONLY);
    if (fd < 0) {
        perror("open");
        return -1;
    }
    ret = (int) read(fd, buf, len);
    close(fd);
    return ret;
}

char *getAppId(char *application_id, size_t size) {
    pid_t pid = getpid();
//    LOGV("process new id %d", pid);
    char path[64] = {0};
    sprintf(path, "/proc/%d/cmdline", pid);
    FILE *cmdline = fopen(path, "r");
    if (cmdline) {
        fread(application_id, size, 1, cmdline);
//        LOGV("application id %s", application_id);
        fclose(cmdline);
    }
    return application_id;
}