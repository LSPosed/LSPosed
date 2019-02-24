//
// Created by Solo on 2019/1/27.
//

#ifndef EDXPOSED_CONFIG_MANAGER_H
#define EDXPOSED_CONFIG_MANAGER_H

bool is_app_need_hook(JNIEnv *env, jstring appDataDir);

bool is_black_white_list_enabled();

bool is_dynamic_modules_enabled();

jstring get_installer_pkg_name(JNIEnv *env);

#endif //EDXPOSED_CONFIG_MANAGER_H
