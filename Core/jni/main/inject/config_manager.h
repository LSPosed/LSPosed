//
// Created by Solo on 2019/1/27.
//

#ifndef EDXPOSED_CONFIG_MANAGER_H
#define EDXPOSED_CONFIG_MANAGER_H

int is_app_need_hook(JNIEnv *env, jstring appDataDir);

bool is_dynamic_modules();

#endif //EDXPOSED_CONFIG_MANAGER_H
