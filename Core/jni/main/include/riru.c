#include <dlfcn.h>
#include <memory.h>
#include <include/logging.h>

#ifdef __LP64__
#define LIB "/system/lib64/libmemtrack.so"
#else
#define LIB "/system/lib/libmemtrack.so"
#endif

static void *riru_handle;
static char *riru_module_name;

static void *get_handle() {
    if (riru_handle == NULL)
        riru_handle = dlopen(LIB, RTLD_NOW | RTLD_GLOBAL);

    return riru_handle;
}

const char *riru_get_module_name() {
    return riru_module_name;
}

void riru_set_module_name(const char *name) {
    riru_module_name = strdup(name);
}

int riru_get_version() {
    static void **sym;
    void *handle;
    if ((handle = get_handle()) == NULL) return -1;
    if (sym == NULL) sym = dlsym(handle, "riru_get_version");
    if (sym) return ((int (*)()) sym)();
    return -1;
}

void *riru_get_func(const char *name) {
    static void **sym;
    void *handle;
    if ((handle = get_handle()) == NULL) return NULL;
    if (sym == NULL) sym = dlsym(handle, "riru_get_func");
    if (sym) return ((void *(*)(const char *, const char *)) sym)(riru_get_module_name(), name);
    return NULL;
}

void *riru_get_native_method_func(const char *className, const char *name, const char *signature) {
    static void **sym;
    void *handle;
    if ((handle = get_handle()) == NULL) return NULL;
    if (sym == NULL) sym = dlsym(handle, "riru_get_native_method_func");
    if (sym)
        return ((void *(*)(const char *, const char *, const char *, const char *)) sym)(
                riru_get_module_name(), className, name, signature);
    return NULL;
}

void riru_set_func(const char *name, void *func) {
    static void **sym;
    void *handle;
    if ((handle = get_handle()) == NULL) return;
    if (sym == NULL) sym = dlsym(handle, "riru_set_func");
    if (sym)
        ((void *(*)(const char *, const char *, void *)) sym)(riru_get_module_name(), name, func);
}

void riru_set_native_method_func(const char *className, const char *name, const char *signature,
                                 void *func) {
    static void **sym;
    void *handle;
    if ((handle = get_handle()) == NULL) return;
    if (sym == NULL) sym = dlsym(handle, "riru_set_native_method_func");
    if (sym)
        ((void *(*)(const char *, const char *, const char *, const char *, void *)) sym)(
                riru_get_module_name(), className, name, signature, func);
}