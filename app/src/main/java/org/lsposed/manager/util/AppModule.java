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

package org.lsposed.manager.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.AppGlideModule;
import com.caverock.androidsvg.SVG;

import org.lsposed.manager.App;
import org.lsposed.manager.R;
import org.lsposed.manager.util.svg.ExternalFileResolver;
import org.lsposed.manager.util.svg.SvgDecoder;
import org.lsposed.manager.util.svg.SvgDrawableTranscoder;

import java.io.InputStream;

import me.zhanghai.android.appiconloader.glide.AppIconModelLoader;

@GlideModule
public class AppModule extends AppGlideModule {
    @Override
    public void registerComponents(Context context, @NonNull Glide glide, Registry registry) {
        int iconSize = context.getResources().getDimensionPixelSize(R.dimen.app_icon_size);
        registry.prepend(PackageInfo.class, Bitmap.class, new AppIconModelLoader.Factory(iconSize,
                context.getApplicationInfo().loadIcon(context.getPackageManager()) instanceof AdaptiveIconDrawable, context));
        OkHttpUrlLoader.Factory factory = new OkHttpUrlLoader.Factory(App.getOkHttpClient());
        registry.replace(GlideUrl.class, InputStream.class, factory);
        SVG.registerExternalFileResolver(new ExternalFileResolver());
        registry.register(SVG.class, Drawable.class, new SvgDrawableTranscoder(context))
                .append(InputStream.class, SVG.class, new SvgDecoder());
    }
}

