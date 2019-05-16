//
// Created by Solo on 2019/1/27.
//

#ifndef EDXPOSED_CONFIG_MANAGER_H
#define EDXPOSED_CONFIG_MANAGER_H

#include <jni.h>

bool is_app_need_hook(JNIEnv *env, jclass, jstring appDataDir);

bool is_black_white_list_enabled();

bool is_dynamic_modules_enabled();

bool is_resources_hook_enabled();

bool is_deopt_boot_image_enabled();

jstring get_installer_pkg_name(JNIEnv *env, jclass clazz);

#endif //EDXPOSED_CONFIG_MANAGER_H
