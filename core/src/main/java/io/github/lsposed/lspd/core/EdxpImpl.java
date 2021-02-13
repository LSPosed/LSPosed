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
 * Copyright (C) 2020 EdXposed Contributors
 * Copyright (C) 2021 LSPosed Contributors
 */

package io.github.lsposed.lspd.core;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import io.github.lsposed.common.KeepAll;
import io.github.lsposed.lspd.proxy.Router;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.SOURCE;

public interface EdxpImpl extends KeepAll {

    int NONE = 0;
    int YAHFA = 1;
    int SANDHOOK = 2;

    @NonNull
    Proxy getNormalProxy();

    @NonNull
    Router getRouter();

    @Variant
    int getVariant();

    void init();

    boolean isInitialized();

    @Retention(SOURCE)
    @IntDef({NONE, YAHFA, SANDHOOK})
    @interface Variant {
    }
}
