#ifndef RIRU_H
#define RIRU_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif
__attribute__((visibility("default"))) void riru_set_module_name(const char *name);

/**
 * Get Riru version.
 *
 * @return Riru version
 */
int riru_get_version();

/*
 * Get new_func address from last module which hook func.
 * Use this as your old_func if you want to hook func.
 *
 * @param name a unique name
 * @return new_func from last module or null
 */
void *riru_get_func(const char *name);

/*
 * Java native version of riru_get_func.
 *
 * @param className class name
 * @param name method name
 * @param signature method signature
 * @return new_func address from last module or original address
 */
void *riru_get_native_method_func(const char *className, const char *name, const char *signature);

/*
 * Set new_func address for next module which wants to hook func.
 *
 * @param name a unique name
 * @param func your new_func address
 */
void riru_set_func(const char *name, void *func);

/*
 * Java native method version of riru_set_func.
 *
 * @param className class name
 * @param name method name
 * @param signature method signature
 * @param func your new_func address
 */
void riru_set_native_method_func(const char *className, const char *name, const char *signature,
                                 void *func);

/**
 * Get native methods from Riru's jniRegisterNativeMethods hook.
 * If both name and signature are null, all the class's methods will be returned.
 *
 * @param className className
 * @param name method name, null for the method with specific signature
 * @param signature method signature, null for the method with specific name
 * @return JNINativeMethod*
 */
const JNINativeMethod *riru_get_original_native_methods(const char *className, const char *name,
                                                        const char *signature);

/**
 * Return if Zygote class's native method nativeForkAndSpecialize & nativeForkSystemServer is replaced by
 * Riru.
 *
 * @return methods replaced
 */
int riru_is_zygote_methods_replaced();

/**
 * Return calls count of Zygote class's native method nativeForkAndSpecialize replaced by Riru.
 *
 * @return nativeForkAndSpecialize calls count
 */
int riru_get_nativeForkAndSpecialize_calls_count();

/**
 * Return calls count of Zygote class native's method nativeForkSystemServer replaced by Riru.
 *
 * @return nativeForkAndSpecialize calls count
 */
int riru_get_nativeForkSystemServer_calls_count();
#ifdef __cplusplus
}
#endif

#endif