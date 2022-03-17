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

package org.lsposed.manager.util.chrome;

import android.app.Activity;
import android.text.style.URLSpan;
import android.view.View;

import org.lsposed.manager.util.NavUtil;

public class CustomTabsURLSpan extends URLSpan {

    private final Activity activity;

    public CustomTabsURLSpan(Activity activity, String url) {
        super(url);
        this.activity = activity;
    }

    @Override
    public void onClick(View widget) {
        String url = getURL();
        NavUtil.startURL(activity, url);
    }
}
