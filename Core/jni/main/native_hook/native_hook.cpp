#include <cstdio>
#include <cstring>
#include <chrono>
#include <fcntl.h>
#include <unistd.h>
#include <sys/vfs.h>
#include <sys/stat.h>
#include <dirent.h>
#include <dlfcn.h>
#include <cstdlib>
#include <string>
#include <sys/system_properties.h>

#include <jni.h>

#include "include/riru.h"
#include "include/logging.h"
#include "native_hook.h"
#include "java_hook/java_hook.h"
#include "inject/framework_hook.h"