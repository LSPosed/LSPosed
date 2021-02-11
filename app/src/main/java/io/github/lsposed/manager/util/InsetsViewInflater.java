/*
 * This file is part of EdXposed.
 *
 * EdXposed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * EdXposed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with EdXposed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2020 EdXposed Contributors
 * Copyright (C) 2021 EdXposed Contributors
 */

package io.github.lsposed.manager.util;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

import rikka.insets.WindowInsetsHelper;
import rikka.layoutinflater.view.LayoutInflaterFactory;

public class InsetsViewInflater extends LayoutInflaterFactory {
    public InsetsViewInflater(@NonNull AppCompatDelegate delegate) {
        super(delegate);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable View parent, @NonNull String name, @NonNull Context context, @NonNull AttributeSet attrs) {
        WindowInsetsHelper.attach(view, attrs);
    }
}
