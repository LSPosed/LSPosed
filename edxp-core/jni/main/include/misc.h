#ifndef MISC_H
#define MISC_H

#include <sys/types.h>

ssize_t fdgets(char *buf, const size_t size, int fd);

char *get_cmdline_from_pid(pid_t pid, char *buf, size_t len);

int read_to_buf(const char *filename, void *buf, size_t len);

char *getAppId(char *application_id, size_t size);

#endif // MISC_H
