/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2022 LSPosed Contributors
 */

//
// Created by Nullptr on 2022/3/16.
//

#pragma once

#include "context.h"

namespace lspd {
    class MagiskLoader : public Context {
    public:
        inline static void Init() {
            instance_ = std::make_unique<MagiskLoader>();
        }

        inline static MagiskLoader *GetInstance() {
            return static_cast<MagiskLoader*>(instance_.get());
        }

        void OnNativeForkAndSpecializePre(JNIEnv *env, jint uid, jintArray &gids, jstring nice_name,
                                          jboolean is_child_zygote, jstring app_data_dir);

        void OnNativeForkAndSpecializePost(JNIEnv *env, jstring nice_name, jstring app_dir);

        void OnNativeForkSystemServerPost(JNIEnv *env);

        void OnNativeForkSystemServerPre(JNIEnv *env);

    protected:
        void LoadDex(JNIEnv *env, PreloadedDex &&dex) override;

        void SetupEntryClass(JNIEnv *env) override;

    private:
        bool skip_ = false;

        static void setAllowUnload(bool unload);
    };
} // namespace lspd
