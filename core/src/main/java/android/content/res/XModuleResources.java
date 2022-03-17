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

package android.content.res;

import android.app.AndroidAppHelper;
import android.util.DisplayMetrics;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.IXposedHookZygoteInit.StartupParam;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import hidden.HiddenApiBridge;

/**
 * Provides access to resources from a certain path (usually the module's own path).
 */
public class XModuleResources extends Resources {
	private XModuleResources(AssetManager assets, DisplayMetrics metrics, Configuration config) {
		super(assets, metrics, config);
	}

	/**
	 * Creates a new instance.
	 *
	 * <p>This is usually called with {@link StartupParam#modulePath} from
	 * {@link IXposedHookZygoteInit#initZygote} and {@link InitPackageResourcesParam#res} from
	 * {@link IXposedHookInitPackageResources#handleInitPackageResources} (or {@code null} for
	 * system-wide replacements).
	 *
	 * @param path The path to the APK from which the resources should be loaded.
	 * @param origRes The resources object from which settings like the display metrics and the
	 *                configuration should be copied. May be {@code null}.
	 */
	public static XModuleResources createInstance(String path, XResources origRes) {
		if (path == null)
			throw new IllegalArgumentException("path must not be null");

		AssetManager assets = new AssetManager();
		HiddenApiBridge.AssetManager_addAssetPath(assets, path);

		XModuleResources res;
		if (origRes != null)
			res = new XModuleResources(assets, origRes.getDisplayMetrics(),	origRes.getConfiguration());
		else
			res = new XModuleResources(assets, null, null);

		AndroidAppHelper.addActiveResource(path, res);
		return res;
	}

	/**
	 * Creates an {@link XResForwarder} instance that forwards requests to {@code id} in this resource.
	 */
	public XResForwarder fwd(int id) {
		return new XResForwarder(this, id);
	}
}
