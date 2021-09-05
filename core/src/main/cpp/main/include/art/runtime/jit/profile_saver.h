//
// Created by loves on 6/19/2021.
//

#ifndef LSPATCH_PROFILE_SAVER_H
#define LSPATCH_PROFILE_SAVER_H

#include <base/object.h>

namespace art {
    CREATE_MEM_HOOK_STUB_ENTRIES(
            "_ZN3art12ProfileSaver20ProcessProfilingInfoEbPt",
            bool, ProcessProfilingInfo, (void * thiz, bool, uint16_t *), {
                LOGD("skipped profile saving");
                return true;
            });

    CREATE_MEM_HOOK_STUB_ENTRIES(
            "_ZN3art12ProfileSaver20ProcessProfilingInfoEbbPt",
            bool, ProcessProfilingInfoWithBool, (void * thiz, bool, bool, uint16_t *), {
                LOGD("skipped profile saving");
                return true;
            });

    CREATE_HOOK_STUB_ENTRIES(
            "execve",
            int, execve, (const char *pathname, const char *argv[], char *const envp[]), {
                if (strstr(pathname, "dex2oat")) {
                    size_t count = 0;
                    while (argv[count++] != nullptr);
                    std::unique_ptr<const char *[]> new_args = std::make_unique<const char *[]>(
                            count + 1);
                    for (size_t i = 0; i < count - 1; ++i)
                        new_args[i] = argv[i];
                    new_args[count - 1] = "--inline-max-code-units=0";
                    new_args[count] = nullptr;

                    LOGD("dex2oat by disable inline!");
                    int ret = backup(pathname, new_args.get(), envp);
                    return ret;
                }
                int ret = backup(pathname, argv, envp);
                return ret;
            });


    static void DisableInline(const SandHook::ElfImg &handle) {
        lspd::HookSyms(handle, ProcessProfilingInfo, ProcessProfilingInfoWithBool);
        lspd::HookSymNoHandle(reinterpret_cast<void*>(&::execve), execve);
    }
}


#endif //LSPATCH_PROFILE_SAVER_H
