/*
 * Copyright (c) 2019 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package org.meowcat.edxposed.manager.util;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;

public interface CustomThemeColor {

    @ColorRes
    int getResourceId();

    @NonNull
    String getResourceEntryName();
}
