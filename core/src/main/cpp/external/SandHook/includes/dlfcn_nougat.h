#ifndef DLFCN_NOUGAT_H
#define DLFCN_NOUGAT_H

//see implementation in https://tech.meituan.com/2017/07/20/android-remote-debug.html
extern "C" {
int fake_dlclose(void *handle);

void *fake_dlopen(const char *filename, int flags);

void *fake_dlsym(void *handle, const char *name);

const char *fake_dlerror();

void *getSymCompat(const char *filename, const char *name);
}

#endif //DLFCN_NOUGAT_H
