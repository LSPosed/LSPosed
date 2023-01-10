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

import static org.lsposed.lspd.nativebridge.ResourcesHook.rewriteXmlReferencesNative;
import static de.robv.android.xposed.XposedHelpers.decrementMethodDepth;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.getLongField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.incrementMethodDepth;

import android.content.Context;
import android.content.pm.PackageParser;
import android.content.pm.PackageParser.PackageParserException;
import android.graphics.Color;
import android.graphics.Movie;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.StrictMode;
import android.text.Html;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.WeakHashMap;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedBridge.CopyOnWriteSortedSet;
import de.robv.android.xposed.XposedInit;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XC_LayoutInflated.LayoutInflatedParam;
import de.robv.android.xposed.callbacks.XCallback;
import xposed.dummy.XResourcesSuperClass;
import xposed.dummy.XTypedArraySuperClass;

/**
 * {@link android.content.res.Resources} subclass that allows replacing individual resources.
 *
 * <p>Xposed replaces the standard resources with this class, which overrides the methods used for
 * retrieving individual resources and adds possibilities to replace them. These replacements can
 * be set using the methods made available via the API methods in this class.
 */
@SuppressWarnings("JniMissingFunction")
public class XResources extends XResourcesSuperClass {
	private static final SparseArray<HashMap<String, Object>> sReplacements = new SparseArray<>();
	private static final SparseArray<HashMap<String, ResourceNames>> sResourceNames = new SparseArray<>();

	// A resource ID is a 32 bit number of the form: PPTTNNNN. PP is the package the resource is for;
	// TT is the type of the resource;
	// NNNN is the name of the resource in that type.
	// For applications resources, PP is always 0x7f.
	private static final byte[] sSystemReplacementsCache = new byte[256]; // bitmask: 0x000700ff => 2048 bit => 256 bytes
	private byte[] mReplacementsCache; // bitmask: 0x0007007f => 1024 bit => 128 bytes
	private static final HashMap<String, byte[]> sReplacementsCacheMap = new HashMap<>();
	private static final SparseArray<ColorStateList> sColorStateListCache = new SparseArray<>(0);

	private static final SparseArray<HashMap<String, CopyOnWriteSortedSet<XC_LayoutInflated>>> sLayoutCallbacks = new SparseArray<>();
	private static final WeakHashMap<XmlResourceParser, XMLInstanceDetails> sXmlInstanceDetails = new WeakHashMap<>();

	private static final String EXTRA_XML_INSTANCE_DETAILS = "xmlInstanceDetails";
	private static final ThreadLocal<LinkedList<MethodHookParam>> sIncludedLayouts = ThreadLocal.withInitial(() -> new LinkedList<>());

	private static final HashMap<String, Long> sResDirLastModified = new HashMap<>();
	private static final HashMap<String, String> sResDirPackageNames = new HashMap<>();
	private static ThreadLocal<Object> sLatestResKey = null;

	private String mResDir;
	private String mPackageName;

	public XResources(ClassLoader classLoader, String resDir) {
		super(classLoader);

		this.mResDir = resDir;
		this.mPackageName = getPackageName(resDir);

		if (resDir != null) {
			synchronized (sReplacementsCacheMap) {
				mReplacementsCache = sReplacementsCacheMap.computeIfAbsent(resDir, k -> new byte[128]);
			}
		}
	}

	/** Dummy, will never be called (objects are transferred to this class only). */
//	private XResources() {
//		throw new UnsupportedOperationException();
//	}

	/** @hide */
	public boolean isFirstLoad() {
		synchronized (sReplacements) {
			if (mResDir == null)
				return false;

			final StrictMode.ThreadPolicy policy = StrictMode.allowThreadDiskReads();
			Long lastModification = new File(mResDir).lastModified();
			Long oldModified = sResDirLastModified.get(mResDir);
			StrictMode.setThreadPolicy(policy);
			if (lastModification.equals(oldModified))
				return false;

			sResDirLastModified.put(mResDir, lastModification);

			if (oldModified == null)
				return true;

			// file was changed meanwhile => remove old replacements
			for (int i = 0; i < sReplacements.size(); i++) {
				sReplacements.valueAt(i).remove(mResDir);
			}
			Arrays.fill(mReplacementsCache, (byte) 0);
			return true;
		}
	}

	/** @hide */
	public static void setPackageNameForResDir(String packageName, String resDir) {
		synchronized (sResDirPackageNames) {
			sResDirPackageNames.put(resDir, packageName);
		}
	}

	/**
	 * Returns the name of the package that these resources belong to, or "android" for system resources.
	 */
	@NonNull
	public String getPackageName() {
		return mPackageName;
	}

	private static String getPackageName(String resDir) {
		if (resDir == null)
			return "android";

		String packageName;
		synchronized (sResDirPackageNames) {
			packageName = sResDirPackageNames.get(resDir);
		}

		if (packageName != null)
			return packageName;

		PackageParser.PackageLite pkgInfo;
		try {
			pkgInfo = PackageParser.parsePackageLite(new File(resDir), 0);
		} catch (PackageParserException e) {
			throw new IllegalStateException("Could not determine package name for " + resDir, e);
		}
		if (pkgInfo != null && pkgInfo.packageName != null) {
//			Log.w(XposedBridge.TAG, "Package name for " + resDir + " had to be retrieved via parser");
			packageName = pkgInfo.packageName;
			setPackageNameForResDir(packageName, resDir);
			return packageName;
		}

		throw new IllegalStateException("Could not determine package name for " + resDir);
	}

	/**
	 * Special case of {@link #getPackageName} during object creation.
	 *
	 * <p>For a short moment during/after the creation of a new {@link android.content.res Resources}
	 * object, it isn't an instance of {@link XResources} yet. For any hooks that need information
	 * about the just created object during this particular stage, this method will return the
	 * package name.
	 *
	 * <p class="warning">If you call this method outside of {@code getTopLevelResources()}, it
	 * throws an {@code IllegalStateException}.
	 */
	public static String getPackageNameDuringConstruction() {
		Object key;
		if (sLatestResKey == null || (key = sLatestResKey.get()) == null)
			throw new IllegalStateException("This method can only be called during getTopLevelResources()");

		String resDir = (String) getObjectField(key, "mResDir");
		return getPackageName(resDir);
	}

	/** @hide */
	public static void init(ThreadLocal<Object> latestResKey) throws Exception {
		sLatestResKey = latestResKey;

		findAndHookMethod(LayoutInflater.class, "inflate", XmlPullParser.class, ViewGroup.class, boolean.class, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (param.hasThrowable())
					return;

				XMLInstanceDetails details;
				synchronized (sXmlInstanceDetails) {
					details = sXmlInstanceDetails.get(param.args[0]);
				}
				if (details != null) {
					LayoutInflatedParam liparam = new LayoutInflatedParam(details.callbacks);
					liparam.view = (View) param.getResult();
					liparam.resNames = details.resNames;
					liparam.variant = details.variant;
					liparam.res = details.res;
					XCallback.callAll(liparam);
				}
			}
		});

		final XC_MethodHook parseIncludeHook = new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				sIncludedLayouts.get().push(param);
			}

			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				sIncludedLayouts.get().pop();

				if (param.hasThrowable())
					return;

				// filled in by our implementation of getLayout()
				XMLInstanceDetails details = (XMLInstanceDetails) param.getObjectExtra(EXTRA_XML_INSTANCE_DETAILS);
				if (details != null) {
					LayoutInflatedParam liparam = new LayoutInflatedParam(details.callbacks);
					ViewGroup group = (ViewGroup) param.args[2];
					liparam.view = group.getChildAt(group.getChildCount() - 1);
					liparam.resNames = details.resNames;
					liparam.variant = details.variant;
					liparam.res = details.res;
					XCallback.callAll(liparam);
				}
			}
		};
		findAndHookMethod(LayoutInflater.class, "parseInclude", XmlPullParser.class, Context.class,
				View.class, AttributeSet.class, parseIncludeHook);
	}

	/**
	 * Wrapper for information about an indiviual resource.
	 */
	public static class ResourceNames {
		/** The resource ID. */
		public final int id;
		/** The resource package name as returned by {@link #getResourcePackageName}. */
		public final String pkg;
		/** The resource entry name as returned by {@link #getResourceEntryName}. */
		public final String name;
		/** The resource type name as returned by {@link #getResourceTypeName}. */
		public final String type;
		/** The full resource nameas returned by {@link #getResourceName}. */
		public final String fullName;

		private ResourceNames(int id, String pkg, String name, String type) {
			this.id = id;
			this.pkg = pkg;
			this.name = name;
			this.type = type;
			this.fullName = pkg + ":" + type + "/" + name;
		}

		/**
		 * Returns whether all non-null parameters match the values of this object.
		 */
		public boolean equals(String pkg, String name, String type, int id) {
			return (pkg  == null || pkg.equals(this.pkg))
				&& (name == null || name.equals(this.name))
				&& (type == null || type.equals(this.type))
				&& (id == 0 || id == this.id);
		}
	}

	private ResourceNames getResourceNames(int id) {
		return new ResourceNames(
				id,
				getResourcePackageName(id),
				getResourceTypeName(id),
				getResourceEntryName(id));
	}

	private static ResourceNames getSystemResourceNames(int id) {
		Resources sysRes = getSystem();
		return new ResourceNames(
				id,
				sysRes.getResourcePackageName(id),
				sysRes.getResourceTypeName(id),
				sysRes.getResourceEntryName(id));
	}

	private static void putResourceNames(String resDir, ResourceNames resNames) {
		int id = resNames.id;
		synchronized (sResourceNames) {
			HashMap<String, ResourceNames> inner = sResourceNames.get(id);
			if (inner == null) {
				inner = new HashMap<>();
				sResourceNames.put(id, inner);
			}
			synchronized (inner) {
				inner.put(resDir, resNames);
			}
		}
	}

	// =======================================================
	//   DEFINING REPLACEMENTS
	// =======================================================

	/**
	 * Sets a replacement for an individual resource. See {@link #setReplacement(String, String, String, Object)}.
	 *
	 * @param id The ID of the resource which should be replaced.
	 * @param replacement The replacement, see above.
	 */
	public void setReplacement(int id, Object replacement) {
		setReplacement(id, replacement, this);
	}

	/**
	 * Sets a replacement for an individual resource. See {@link #setReplacement(String, String, String, Object)}.
	 *
	 * @deprecated Use {@link #setReplacement(String, String, String, Object)} instead.
	 *
	 * @param fullName The full resource name, e.g. {@code com.example.myapplication:string/app_name}.
	 *                 See {@link #getResourceName}.
	 * @param replacement The replacement.
	 */
	@Deprecated
	public void setReplacement(String fullName, Object replacement) {
		int id = getIdentifier(fullName, null, null);
		if (id == 0)
			throw new NotFoundException(fullName);
		setReplacement(id, replacement, this);
	}

	/**
	 * Sets a replacement for an individual resource. If called more than once for the same ID, the
	 * replacement from the last call is used. Setting the replacement to {@code null} removes it.
	 *
	 * <p>The allowed replacements depend on the type of the source. All types accept an
	 * {@link XResForwarder} object, which is usually created with {@link XModuleResources#fwd}.
	 * The resource request will then be forwarded to another {@link android.content.res.Resources}
	 * object. In addition to that, the following replacement types are accepted:
	 *
	 * <table>
	 *     <thead>
	 *     <tr><th>Resource type</th> <th>Additional allowed replacement types (*)</th> <th>Returned from (**)</th></tr>
	 *     </thead>
	 *
	 *     <tbody>
	 *     <tr><td><a href="http://developer.android.com/guide/topics/resources/animation-resource.html">Animation</a></td>
	 *         <td>&nbsp;<i>none</i></td>
	 *         <td>{@link #getAnimation}</td>
	 *     </tr>
	 *
	 *     <tr><td><a href="http://developer.android.com/guide/topics/resources/more-resources.html#Bool">Bool</a></td>
	 *         <td>{@link Boolean}</td>
	 *         <td>{@link #getBoolean}</td>
	 *     </tr>
	 *
	 *     <tr><td><a href="http://developer.android.com/guide/topics/resources/more-resources.html#Color">Color</a></td>
	 *         <td>{@link Integer} (you might want to use {@link Color#parseColor})</td>
	 *         <td>{@link #getColor}<br>
	 *             {@link #getDrawable} (creates a {@link ColorDrawable})<br>
	 *             {@link #getColorStateList} (calls {@link android.content.res.ColorStateList#valueOf})
	 *         </td>
	 *     </tr>
	 *
	 *     <tr><td><a href="http://developer.android.com/guide/topics/resources/color-list-resource.html">Color State List</a></td>
	 *         <td>{@link android.content.res.ColorStateList}<br>
	 *             {@link Integer} (calls {@link android.content.res.ColorStateList#valueOf})
	 *         </td>
	 *         <td>{@link #getColorStateList}</td>
	 *     </tr>
	 *
	 *     <tr><td><a href="http://developer.android.com/guide/topics/resources/more-resources.html#Dimension">Dimension</a></td>
	 *         <td>{@link DimensionReplacement} <i>(since v50)</i></td>
	 *         <td>{@link #getDimension}<br>
	 *             {@link #getDimensionPixelOffset}<br>
	 *             {@link #getDimensionPixelSize}
	 *         </td>
	 *     </tr>
	 *
	 *     <tr><td><a href="http://developer.android.com/guide/topics/resources/drawable-resource.html">Drawable</a>
	 *             (including <a href="http://developer.android.com/tools/projects/index.html#mipmap">mipmap</a>)</td>
	 *         <td>{@link DrawableLoader}<br>
	 *             {@link Integer} (creates a {@link ColorDrawable})
	 *         </td>
	 *         <td>{@link #getDrawable}<br>
	 *             {@link #getDrawableForDensity}
	 *         </td>
	 *     </tr>
	 *
	 *     <tr><td>Fraction</td>
	 *         <td>&nbsp;<i>none</i></td>
	 *         <td>{@link #getFraction}</td>
	 *     </tr>
	 *
	 *     <tr><td><a href="http://developer.android.com/guide/topics/resources/more-resources.html#Integer">Integer</a></td>
	 *         <td>{@link Integer}</td>
	 *         <td>{@link #getInteger}</td>
	 *     </tr>
	 *
	 *     <tr><td><a href="http://developer.android.com/guide/topics/resources/more-resources.html#IntegerArray">Integer Array</a></td>
	 *         <td>{@code int[]}</td>
	 *         <td>{@link #getIntArray}</td>
	 *     </tr>
	 *
	 *     <tr><td><a href="http://developer.android.com/guide/topics/resources/layout-resource.html">Layout</a></td>
	 *         <td>&nbsp;<i>none, but see {@link #hookLayout}</i></td>
	 *         <td>{@link #getLayout}</td>
	 *     </tr>
	 *
	 *     <tr><td>Movie</td>
	 *         <td>&nbsp;<i>none</i></td>
	 *         <td>{@link #getMovie}</td>
	 *     </tr>
	 *
	 *     <tr><td><a href="http://developer.android.com/guide/topics/resources/string-resource.html#Plurals">Quantity Strings (Plurals)</a></td>
	 *         <td>&nbsp;<i>none</i></td>
	 *         <td>{@link #getQuantityString}<br>
	 *             {@link #getQuantityText}
	 *         </td>
	 *     </tr>
	 *
	 *     <tr><td><a href="http://developer.android.com/guide/topics/resources/string-resource.html#String">String</a></td>
	 *         <td>{@link String}<br>
	 *             {@link CharSequence} (for styled texts, see also {@link Html#fromHtml})
	 *         </td>
	 *         <td>{@link #getString}<br>
	 *             {@link #getText}
	 *         </td>
	 *     </tr>
	 *
	 *     <tr><td><a href="http://developer.android.com/guide/topics/resources/string-resource.html#StringArray">String Array</a></td>
	 *         <td>{@code String[]}<br>
	 *             {@code CharSequence[]} (for styled texts, see also {@link Html#fromHtml})
	 *         </td>
	 *         <td>{@link #getStringArray}<br>
	 *             {@link #getTextArray}
	 *         </td>
	 *     </tr>
	 *
	 *     <tr><td>XML</td>
	 *         <td>&nbsp;<i>none</i></td>
	 *         <td>{@link #getXml}<br>
	 *             {@link #getQuantityText}
	 *         </td>
	 *     </tr>
	 *
	 *     </tbody>
	 * </table>
	 *
	 * <p>Other resource types, such as
	 * <a href="http://developer.android.com/guide/topics/resources/style-resource.html">styles/themes</a>,
	 * {@linkplain #openRawResource raw resources} and
	 * <a href="http://developer.android.com/guide/topics/resources/more-resources.html#TypedArray">typed arrays</a>
	 * can't be replaced.
	 *
	 * <p><i>
	 *    * Auto-boxing allows you to use literals like {@code 123} where an {@link Integer} is
	 *      accepted, so you don't neeed to call methods like {@link Integer#valueOf(int)} manually.<br>
	 *    ** Some of these methods have multiple variants, only one of them is mentioned here.
	 * </i>
	 *
	 * @param pkg The package name, e.g. {@code com.example.myapplication}.
	 *            See {@link #getResourcePackageName}.
	 * @param type The type name, e.g. {@code string}.
	 *            See {@link #getResourceTypeName}.
	 * @param name The entry name, e.g. {@code app_name}.
	 *            See {@link #getResourceEntryName}.
	 * @param replacement The replacement.
	 */
	public void setReplacement(String pkg, String type, String name, Object replacement) {
		int id = getIdentifier(name, type, pkg);
		if (id == 0)
			throw new NotFoundException(pkg + ":" + type + "/" + name);
		setReplacement(id, replacement, this);
	}

	/**
	 * Sets a replacement for an individual Android framework resource (in the {@code android} package).
	 * See {@link #setSystemWideReplacement(String, String, String, Object)}.
	 *
	 * @param id The ID of the resource which should be replaced.
	 * @param replacement The replacement.
	 */
	public static void setSystemWideReplacement(int id, Object replacement) {
		setReplacement(id, replacement, null);
	}

	/**
	 * Sets a replacement for an individual Android framework resource (in the {@code android} package).
	 * See {@link #setSystemWideReplacement(String, String, String, Object)}.
	 *
	 * @deprecated Use {@link #setSystemWideReplacement(String, String, String, Object)} instead.
	 *
	 * @param fullName The full resource name, e.g. {@code android:string/yes}.
	 *                 See {@link #getResourceName}.
	 * @param replacement The replacement.
	 */
	@Deprecated
	public static void setSystemWideReplacement(String fullName, Object replacement) {
		int id = getSystem().getIdentifier(fullName, null, null);
		if (id == 0)
			throw new NotFoundException(fullName);
		setReplacement(id, replacement, null);
	}

	/**
	 * Sets a replacement for an individual Android framework resource (in the {@code android} package).
	 *
	 * <p>Some resources are part of the Android framework and can be used in any app. They're
	 * accessible via {@link android.R android.R} and are not bound to a specific
	 * {@link android.content.res.Resources} instance. Such resources can be replaced in
	 * {@link IXposedHookZygoteInit#initZygote initZygote()} for all apps. As there is no
	 * {@link XResources} object easily available in that scope, this static method can be used
	 * to set resource replacements. All other details (e.g. how certain types can be replaced) are
	 * mentioned in {@link #setReplacement(String, String, String, Object)}.
	 *
	 * @param pkg The package name, should always be {@code android} here.
	 *            See {@link #getResourcePackageName}.
	 * @param type The type name, e.g. {@code string}.
	 *            See {@link #getResourceTypeName}.
	 * @param name The entry name, e.g. {@code yes}.
	 *            See {@link #getResourceEntryName}.
	 * @param replacement The replacement.
	 */
	public static void setSystemWideReplacement(String pkg, String type, String name, Object replacement) {
		int id = getSystem().getIdentifier(name, type, pkg);
		if (id == 0)
			throw new NotFoundException(pkg + ":" + type + "/" + name);
		setReplacement(id, replacement, null);
	}

	private static void setReplacement(int id, Object replacement, XResources res) {
		String resDir = (res != null) ? res.mResDir : null;
		if (res == null) {
			try {
				XposedInit.hookResources();
			} catch (Throwable throwable) {
				throw new IllegalStateException("Failed to initialize resources hook");
			}
		}
		if (id == 0)
			throw new IllegalArgumentException("id 0 is not an allowed resource identifier");
		else if (resDir == null && id >= 0x7f000000)
			throw new IllegalArgumentException("ids >= 0x7f000000 are app specific and cannot be set for the framework");

		if (replacement instanceof Drawable)
			throw new IllegalArgumentException("Drawable replacements are deprecated since Xposed 2.1. Use DrawableLoader instead.");

		// Cache that we have a replacement for this ID, false positives are accepted to save memory.
		if (id < 0x7f000000) {
			int cacheKey = (id & 0x00070000) >> 11 | (id & 0xf8) >> 3;
			synchronized (sSystemReplacementsCache) {
				sSystemReplacementsCache[cacheKey] |= 1 << (id & 7);
			}
		} else {
			int cacheKey = (id & 0x00070000) >> 12 | (id & 0x78) >> 3;
			synchronized (res.mReplacementsCache) {
				res.mReplacementsCache[cacheKey] |= 1 << (id & 7);
			}
		}

		synchronized (sReplacements) {
			HashMap<String, Object> inner = sReplacements.get(id);
			if (inner == null) {
				inner = new HashMap<>();
				sReplacements.put(id, inner);
			}
			inner.put(resDir, replacement);
		}
	}

	// =======================================================
	//   RETURNING REPLACEMENTS
	// =======================================================

	private Object getReplacement(int id) {
		if (id <= 0)
			return null;

		// Check the cache whether it's worth looking for replacements
		if (id < 0x7f000000) {
			int cacheKey = (id & 0x00070000) >> 11 | (id & 0xf8) >> 3;
			if ((sSystemReplacementsCache[cacheKey] & (1 << (id & 7))) == 0)
				return null;
		} else if (mResDir != null) {
			int cacheKey = (id & 0x00070000) >> 12 | (id & 0x78) >> 3;
			if ((mReplacementsCache[cacheKey] & (1 << (id & 7))) == 0)
				return null;
		}

		HashMap<String, Object> inner;
		synchronized (sReplacements) {
			inner = sReplacements.get(id);
		}

		if (inner == null)
			return null;

		synchronized (inner) {
			Object result = inner.get(mResDir);
			if (result != null || mResDir == null)
				return result;
			return inner.get(null);
		}
	}

	/** @hide */
	@Override
	public XmlResourceParser getAnimation(int id) throws NotFoundException {
		Object replacement = getReplacement(id);
		if (replacement instanceof XResForwarder) {
			Resources repRes = ((XResForwarder) replacement).getResources();
			int repId = ((XResForwarder) replacement).getId();

			boolean loadedFromCache = isXmlCached(repRes, repId);
			XmlResourceParser result = repRes.getAnimation(repId);

			if (!loadedFromCache) {
				long parseState = getLongField(result, "mParseState");
				rewriteXmlReferencesNative(parseState, this, repRes);
			}

			return result;
		}
		return super.getAnimation(id);
	}

	/** @hide */
	@Override
	public boolean getBoolean(int id) throws NotFoundException {
		Object replacement = getReplacement(id);
		if (replacement instanceof Boolean) {
			return (Boolean) replacement;
		} else if (replacement instanceof XResForwarder) {
			Resources repRes = ((XResForwarder) replacement).getResources();
			int repId = ((XResForwarder) replacement).getId();
			return repRes.getBoolean(repId);
		}
		return super.getBoolean(id);
	}

	/** @hide */
	@Override
	public int getColor(int id) throws NotFoundException {
		Object replacement = getReplacement(id);
		if (replacement instanceof Integer) {
			return (Integer) replacement;
		} else if (replacement instanceof XResForwarder) {
			Resources repRes = ((XResForwarder) replacement).getResources();
			int repId = ((XResForwarder) replacement).getId();
			return repRes.getColor(repId);
		}
		return super.getColor(id);
	}

	/** @hide */
	@Override
	public ColorStateList getColorStateList(int id) throws NotFoundException {
		Object replacement = getReplacement(id);
		if (replacement instanceof ColorStateList) {
			return (ColorStateList) replacement;
		} else if (replacement instanceof Integer) {
			int color = (Integer) replacement;
			synchronized (sColorStateListCache) {
				ColorStateList result = sColorStateListCache.get(color);
				if (result == null) {
					result = ColorStateList.valueOf(color);
					sColorStateListCache.put(color, result);
				}
				return result;
			}
		} else if (replacement instanceof XResForwarder) {
			Resources repRes = ((XResForwarder) replacement).getResources();
			int repId = ((XResForwarder) replacement).getId();
			return repRes.getColorStateList(repId);
		}
		return super.getColorStateList(id);
	}

	/** @hide */
	@Override
	public float getDimension(int id) throws NotFoundException {
		Object replacement = getReplacement(id);
		if (replacement instanceof DimensionReplacement) {
			return ((DimensionReplacement) replacement).getDimension(getDisplayMetrics());
		} else if (replacement instanceof XResForwarder) {
			Resources repRes = ((XResForwarder) replacement).getResources();
			int repId = ((XResForwarder) replacement).getId();
			return repRes.getDimension(repId);
		}
		return super.getDimension(id);
	}

	/** @hide */
	@Override
	public int getDimensionPixelOffset(int id) throws NotFoundException {
		Object replacement = getReplacement(id);
		if (replacement instanceof DimensionReplacement) {
			return ((DimensionReplacement) replacement).getDimensionPixelOffset(getDisplayMetrics());
		} else if (replacement instanceof XResForwarder) {
			Resources repRes = ((XResForwarder) replacement).getResources();
			int repId = ((XResForwarder) replacement).getId();
			return repRes.getDimensionPixelOffset(repId);
		}
		return super.getDimensionPixelOffset(id);
	}

	/** @hide */
	@Override
	public int getDimensionPixelSize(int id) throws NotFoundException {
		Object replacement = getReplacement(id);
		if (replacement instanceof DimensionReplacement) {
			return ((DimensionReplacement) replacement).getDimensionPixelSize(getDisplayMetrics());
		} else if (replacement instanceof XResForwarder) {
			Resources repRes = ((XResForwarder) replacement).getResources();
			int repId = ((XResForwarder) replacement).getId();
			return repRes.getDimensionPixelSize(repId);
		}
		return super.getDimensionPixelSize(id);
	}

	/** @hide */
	@Override
	public Drawable getDrawable(int id) throws NotFoundException {
		try {
			if (incrementMethodDepth("getDrawable") == 1) {
				Object replacement = getReplacement(id);
				if (replacement instanceof DrawableLoader) {
					try {
						Drawable result = ((DrawableLoader) replacement).newDrawable(this, id);
						if (result != null)
							return result;
					} catch (Throwable t) { XposedBridge.log(t); }
				} else if (replacement instanceof Integer) {
					return new ColorDrawable((Integer) replacement);
				} else if (replacement instanceof XResForwarder) {
					Resources repRes = ((XResForwarder) replacement).getResources();
					int repId = ((XResForwarder) replacement).getId();
					return repRes.getDrawable(repId);
				}
			}
			return super.getDrawable(id);
		} finally {
			decrementMethodDepth("getDrawable");
		}
	}

	/** @hide */
	@Override
	public Drawable getDrawable(int id, Theme theme) throws NotFoundException {
		try {
			if (incrementMethodDepth("getDrawable") == 1) {
				Object replacement = getReplacement(id);
				if (replacement instanceof DrawableLoader) {
					try {
						Drawable result = ((DrawableLoader) replacement).newDrawable(this, id);
						if (result != null)
							return result;
					} catch (Throwable t) { XposedBridge.log(t); }
				} else if (replacement instanceof Integer) {
					return new ColorDrawable((Integer) replacement);
				} else if (replacement instanceof XResForwarder) {
					Resources repRes = ((XResForwarder) replacement).getResources();
					int repId = ((XResForwarder) replacement).getId();
					return repRes.getDrawable(repId);
				}
			}
			return super.getDrawable(id, theme);
		} finally {
			decrementMethodDepth("getDrawable");
		}
	}

	/** @hide */
	@Override
	public Drawable getDrawableForDensity(int id, int density) throws NotFoundException {
		try {
			if (incrementMethodDepth("getDrawableForDensity") == 1) {
				Object replacement = getReplacement(id);
				if (replacement instanceof DrawableLoader) {
					try {
						Drawable result = ((DrawableLoader) replacement).newDrawableForDensity(this, id, density);
						if (result != null)
							return result;
					} catch (Throwable t) { XposedBridge.log(t); }
				} else if (replacement instanceof Integer) {
					return new ColorDrawable((Integer) replacement);
				} else if (replacement instanceof XResForwarder) {
					Resources repRes = ((XResForwarder) replacement).getResources();
					int repId = ((XResForwarder) replacement).getId();
					return repRes.getDrawableForDensity(repId, density);
				}
			}
			return super.getDrawableForDensity(id, density);
		} finally {
			decrementMethodDepth("getDrawableForDensity");
		}
	}

	/** @hide */
	@Override
	public Drawable getDrawableForDensity(int id, int density, Theme theme) throws NotFoundException {
		try {
			if (incrementMethodDepth("getDrawableForDensity") == 1) {
				Object replacement = getReplacement(id);
				if (replacement instanceof DrawableLoader) {
					try {
						Drawable result = ((DrawableLoader) replacement).newDrawableForDensity(this, id, density);
						if (result != null)
							return result;
					} catch (Throwable t) { XposedBridge.log(t); }
				} else if (replacement instanceof Integer) {
					return new ColorDrawable((Integer) replacement);
				} else if (replacement instanceof XResForwarder) {
					Resources repRes = ((XResForwarder) replacement).getResources();
					int repId = ((XResForwarder) replacement).getId();
					return repRes.getDrawableForDensity(repId, density);
				}
			}
			return super.getDrawableForDensity(id, density, theme);
		} finally {
			decrementMethodDepth("getDrawableForDensity");
		}
	}

	/** @hide */
	@RequiresApi(Build.VERSION_CODES.Q)
	@Override
	public float getFloat(int id) {
		Object replacement = getReplacement(id);
		if (replacement instanceof Float) {
			return (Float) replacement;
		} else if (replacement instanceof XResForwarder) {
			Resources repRes = ((XResForwarder) replacement).getResources();
			int repId = ((XResForwarder) replacement).getId();
			return repRes.getFloat(repId);
		}
		return super.getFloat(id);
	}

	/** @hide */
	@Override
	public Typeface getFont(int id) {
		Object replacement = getReplacement(id);
		if (replacement instanceof Typeface) {
			return (Typeface) replacement;
		} else if (replacement instanceof XResForwarder) {
			Resources repRes = ((XResForwarder) replacement).getResources();
			int repId = ((XResForwarder) replacement).getId();
			return repRes.getFont(repId);
		}
		return super.getFont(id);
	}

	/** @hide */
	@Override
	public float getFraction(int id, int base, int pbase) {
		Object replacement = getReplacement(id);
		if (replacement instanceof XResForwarder) {
			Resources repRes = ((XResForwarder) replacement).getResources();
			int repId = ((XResForwarder) replacement).getId();
			return repRes.getFraction(repId, base, pbase);
		}
		return super.getFraction(id, base, pbase);
	}

	/** @hide */
	@Override
	public int getInteger(int id) throws NotFoundException {
		Object replacement = getReplacement(id);
		if (replacement instanceof Integer) {
			return (Integer) replacement;
		} else if (replacement instanceof XResForwarder) {
			Resources repRes = ((XResForwarder) replacement).getResources();
			int repId = ((XResForwarder) replacement).getId();
			return repRes.getInteger(repId);
		}
		return super.getInteger(id);
	}

	/** @hide */
	@Override
	public int[] getIntArray(int id) throws NotFoundException {
		Object replacement = getReplacement(id);
		if (replacement instanceof int[]) {
			return (int[]) replacement;
		} else if (replacement instanceof XResForwarder) {
			Resources repRes = ((XResForwarder) replacement).getResources();
			int repId = ((XResForwarder) replacement).getId();
			return repRes.getIntArray(repId);
		}
		return super.getIntArray(id);
	}

	/** @hide */
	@Override
	public XmlResourceParser getLayout(int id) throws NotFoundException {
		XmlResourceParser result;
		Object replacement = getReplacement(id);
		if (replacement instanceof XResForwarder) {
			Resources repRes = ((XResForwarder) replacement).getResources();
			int repId = ((XResForwarder) replacement).getId();

			boolean loadedFromCache = isXmlCached(repRes, repId);
			result = repRes.getLayout(repId);

			if (!loadedFromCache) {
				long parseState = getLongField(result, "mParseState");
				rewriteXmlReferencesNative(parseState, this, repRes);
			}
		} else {
			result = super.getLayout(id);
		}

		// Check whether this layout is hooked
		HashMap<String, CopyOnWriteSortedSet<XC_LayoutInflated>> inner;
		synchronized (sLayoutCallbacks) {
			inner = sLayoutCallbacks.get(id);
		}
		if (inner != null) {
			CopyOnWriteSortedSet<XC_LayoutInflated> callbacks;
			synchronized (inner) {
				callbacks = inner.get(mResDir);
				if (callbacks == null && mResDir != null)
					callbacks = inner.get(null);
			}
			if (callbacks != null) {
				String variant = "layout";
				TypedValue value = (TypedValue) getObjectField(this, "mTmpValue");
				getValue(id, value, true);
				if (value.type == TypedValue.TYPE_STRING) {
					String[] components = value.string.toString().split("/", 3);
					if (components.length == 3)
						variant = components[1];
					else
						XposedBridge.log("Unexpected resource path \"" + value.string.toString()
								+ "\" for resource id 0x" + Integer.toHexString(id));
				} else {
					XposedBridge.log(new NotFoundException("Could not find file name for resource id 0x") + Integer.toHexString(id));
				}

				synchronized (sXmlInstanceDetails) {
					synchronized (sResourceNames) {
						HashMap<String, ResourceNames> resNamesInner = sResourceNames.get(id);
						if (resNamesInner != null) {
							synchronized (resNamesInner) {
								XMLInstanceDetails details = new XMLInstanceDetails(resNamesInner.get(mResDir), variant, callbacks);
								sXmlInstanceDetails.put(result, details);

								// if we were called inside LayoutInflater.parseInclude, store the details for it
								MethodHookParam top = sIncludedLayouts.get().peek();
								if (top != null)
									top.setObjectExtra(EXTRA_XML_INSTANCE_DETAILS, details);
							}
						}
					}
				}
			}
		}

		return result;
	}

	/** @hide */
	@Override
	public Movie getMovie(int id) throws NotFoundException {
		Object replacement = getReplacement(id);
		if (replacement instanceof XResForwarder) {
			Resources repRes = ((XResForwarder) replacement).getResources();
			int repId = ((XResForwarder) replacement).getId();
			return repRes.getMovie(repId);
		}
		return super.getMovie(id);
	}

	/** @hide */
	@Override
	public CharSequence getQuantityText(int id, int quantity) throws NotFoundException {
		Object replacement = getReplacement(id);
		if (replacement instanceof XResForwarder) {
			Resources repRes = ((XResForwarder) replacement).getResources();
			int repId = ((XResForwarder) replacement).getId();
			return repRes.getQuantityText(repId, quantity);
		}
		return super.getQuantityText(id, quantity);
	}
	// these are handled by getQuantityText:
	// public String getQuantityString(int id, int quantity);
	// public String getQuantityString(int id, int quantity, Object... formatArgs);

	/** @hide */
	@Override
	public String[] getStringArray(int id) throws NotFoundException {
		Object replacement = getReplacement(id);
		if (replacement instanceof String[]) {
			return (String[]) replacement;
		} else if (replacement instanceof XResForwarder) {
			Resources repRes = ((XResForwarder) replacement).getResources();
			int repId = ((XResForwarder) replacement).getId();
			return repRes.getStringArray(repId);
		}
		return super.getStringArray(id);
	}

	/** @hide */
	@Override
	public CharSequence getText(int id) throws NotFoundException {
		Object replacement = getReplacement(id);
		if (replacement instanceof CharSequence) {
			return (CharSequence) replacement;
		} else if (replacement instanceof XResForwarder) {
			Resources repRes = ((XResForwarder) replacement).getResources();
			int repId = ((XResForwarder) replacement).getId();
			return repRes.getText(repId);
		}
		return super.getText(id);
	}
	// these are handled by getText:
	// public String getString(int id);
	// public String getString(int id, Object... formatArgs);

	/** @hide */
	@Override
	public CharSequence getText(int id, CharSequence def) {
		Object replacement = getReplacement(id);
		if (replacement instanceof CharSequence) {
			return (CharSequence) replacement;
		} else if (replacement instanceof XResForwarder) {
			Resources repRes = ((XResForwarder) replacement).getResources();
			int repId = ((XResForwarder) replacement).getId();
			return repRes.getText(repId, def);
		}
		return super.getText(id, def);
	}

	/** @hide */
	@Override
	public CharSequence[] getTextArray(int id) throws NotFoundException {
		Object replacement = getReplacement(id);
		if (replacement instanceof CharSequence[]) {
			return (CharSequence[]) replacement;
		} else if (replacement instanceof XResForwarder) {
			Resources repRes = ((XResForwarder) replacement).getResources();
			int repId = ((XResForwarder) replacement).getId();
			return repRes.getTextArray(repId);
		}
		return super.getTextArray(id);
	}

	/** @hide */
	@Override
	public void getValue(int id, TypedValue outValue, boolean resolveRefs) throws NotFoundException {
		Object replacement = getReplacement(id);
		if (replacement instanceof XResForwarder) {
			Resources repRes = ((XResForwarder) replacement).getResources();
			int repId = ((XResForwarder) replacement).getId();
			repRes.getValue(repId, outValue, resolveRefs);
		} else {
			if (replacement != null) {
				XposedBridge.log("Replacement of resource ID #0x" + Integer.toHexString(id) + " escaped because of deprecated replacement. Please use XResForwarder instead.");
			}
			super.getValue(id, outValue, resolveRefs);
		}
	}

	/** @hide */
	@Override
	public void getValueForDensity(int id, int density, TypedValue outValue, boolean resolveRefs) throws NotFoundException {
		Object replacement = getReplacement(id);
		if (replacement instanceof XResForwarder) {
			Resources repRes = ((XResForwarder) replacement).getResources();
			int repId = ((XResForwarder) replacement).getId();
			repRes.getValueForDensity(repId, density, outValue, resolveRefs);
		} else {
			if (replacement != null) {
				XposedBridge.log("Replacement of resource ID #0x" + Integer.toHexString(id) + " escaped because of deprecated replacement. Please use XResForwarder instead.");
			}
			super.getValueForDensity(id, density, outValue, resolveRefs);
		}
	}

	/** @hide */
	@Override
	public XmlResourceParser getXml(int id) throws NotFoundException {
		Object replacement = getReplacement(id);
		if (replacement instanceof XResForwarder) {
			Resources repRes = ((XResForwarder) replacement).getResources();
			int repId = ((XResForwarder) replacement).getId();

			boolean loadedFromCache = isXmlCached(repRes, repId);
			XmlResourceParser result = repRes.getXml(repId);

			if (!loadedFromCache) {
				long parseState = getLongField(result, "mParseState");
				rewriteXmlReferencesNative(parseState, this, repRes);
			}

			return result;
		}
		return super.getXml(id);
	}

	private static boolean isXmlCached(Resources res, int id) {
		int[] mCachedXmlBlockIds = (int[]) getObjectField(getObjectField(res, "mResourcesImpl"), "mCachedXmlBlockCookies");
		synchronized (mCachedXmlBlockIds) {
			for (int cachedId : mCachedXmlBlockIds) {
				if (cachedId == id)
					return true;
			}
		}
		return false;
	}

	/**
	 * Used to replace reference IDs in XMLs.
	 *
	 * When resource requests are forwarded to modules, the may include references to resources with the same
	 * name as in the original resources, but the IDs generated by aapt will be different. rewriteXmlReferencesNative
	 * walks through all references and calls this function to find out the original ID, which it then writes to
	 * the compiled XML file in the memory.
	 */
	private static int translateResId(int id, XResources origRes, Resources repRes) {
		try {
			String entryName = repRes.getResourceEntryName(id);
			String entryType = repRes.getResourceTypeName(id);
			String origPackage = origRes.mPackageName;
			int origResId = 0;
			try {
				// look for a resource with the same name and type in the original package
				origResId = origRes.getIdentifier(entryName, entryType, origPackage);
			} catch (NotFoundException ignored) {}

			boolean repResDefined = false;
			try {
				final TypedValue tmpValue = new TypedValue();
				repRes.getValue(id, tmpValue, false);
				// if a resource has not been defined (i.e. only a resource ID has been created), it will equal "false"
				// this means a boolean "false" value is not detected of it is directly referenced in an XML file
				repResDefined = !(tmpValue.type == TypedValue.TYPE_INT_BOOLEAN && tmpValue.data == 0);
			} catch (NotFoundException ignored) {}

			if (!repResDefined && origResId == 0 && !entryType.equals("id")) {
				XposedBridge.log(entryType + "/" + entryName + " is neither defined in module nor in original resources");
				return 0;
			}

			// exists only in module, so create a fake resource id
			if (origResId == 0)
				origResId = getFakeResId(repRes, id);

			// IDs will never be loaded, no need to set a replacement
			if (repResDefined && !entryType.equals("id"))
				origRes.setReplacement(origResId, new XResForwarder(repRes, id));

			return origResId;
		} catch (Exception e) {
			XposedBridge.log(e);
			return id;
		}
	}

	/**
	 * Generates a fake resource ID.
	 *
	 * <p>The parameter is just hashed, it doesn't have a deeper meaning. However, it's recommended
	 * to use values with a low risk for conflicts, such as a full resource name. Calling this
	 * method multiple times will return the same ID.
	 *
	 * @param resName A used for hashing, see above.
	 * @return The fake resource ID.
	 */
	public static int getFakeResId(String resName) {
		return 0x7e000000 | (resName.hashCode() & 0x00ffffff);
	}

	/**
	 * Generates a fake resource ID.
	 *
	 * <p>This variant uses the result of {@link #getResourceName} to create the hash that the ID is
	 * based on. The given resource doesn't need to match the {@link XResources} instance for which
	 * the fake resource ID is going to be used.
	 *
	 * @param res The {@link android.content.res.Resources} object to be used for hashing.
	 * @param id The resource ID to be used for hashing.
	 * @return The fake resource ID.
	 */
	public static int getFakeResId(Resources res, int id) {
		return getFakeResId(res.getResourceName(id));
	}

	/**
	 * Makes any individual resource available from another {@link android.content.res.Resources}
	 * instance available in this {@link XResources} instance.
	 *
	 * <p>This method combines calls to {@link #getFakeResId(Resources, int)} and
	 * {@link #setReplacement(int, Object)} to generate a fake resource ID and set up a replacement
	 * for it which forwards to the given resource.
	 *
	 * <p>The returned ID can only be used to retrieve the resource, it won't work for methods like
	 * {@link #getResourceName} etc.
	 *
	 * @param res The target {@link android.content.res.Resources} instance.
	 * @param id The target resource ID.
	 * @return The fake resource ID (see above).
	 */
	public int addResource(Resources res, int id) {
		int fakeId = getFakeResId(res, id);
		synchronized (sReplacements) {
			if (sReplacements.indexOfKey(fakeId) < 0)
				setReplacement(fakeId, new XResForwarder(res, id));
		}
		return fakeId;
	}

	/**
	 * Similar to {@link #translateResId}, but used to determine the original ID of attribute names.
	 */
	private static int translateAttrId(String attrName, XResources origRes) {
		String origPackage = origRes.mPackageName;
		int origAttrId = 0;
		try {
			origAttrId = origRes.getIdentifier(attrName, "attr", origPackage);
		} catch (NotFoundException e) {
			XposedBridge.log("Attribute " + attrName + " not found in original resources");
		}
		return origAttrId;
	}

	// =======================================================
	//   XTypedArray class
	// =======================================================
	/**
	 * {@link android.content.res.TypedArray} replacement that replaces values on-the-fly.
	 * Mainly used when inflating layouts.
	 * @hide
	 */
	public static class XTypedArray extends XTypedArraySuperClass {

        public XTypedArray(Resources resources) {
            super(resources);
        }

        /** Dummy, will never be called (objects are transferred to this class only). */
//		private XTypedArray() {
//			super(null, null, null, 0);
//			throw new UnsupportedOperationException();
//		}

		@Override
		public boolean getBoolean(int index, boolean defValue) {
			Object replacement = ((XResources) getResources()).getReplacement(getResourceId(index, 0));
			if (replacement instanceof Boolean) {
				return (Boolean) replacement;
			} else if (replacement instanceof XResForwarder) {
				Resources repRes = ((XResForwarder) replacement).getResources();
				int repId = ((XResForwarder) replacement).getId();
				return repRes.getBoolean(repId);
			}
			return super.getBoolean(index, defValue);
		}

		@Override
		public int getColor(int index, int defValue) {
			Object replacement = ((XResources) getResources()).getReplacement(getResourceId(index, 0));
			if (replacement instanceof Integer) {
				return (Integer) replacement;
			} else if (replacement instanceof XResForwarder) {
				Resources repRes = ((XResForwarder) replacement).getResources();
				int repId = ((XResForwarder) replacement).getId();
				return repRes.getColor(repId);
			}
			return super.getColor(index, defValue);
		}

		@Override
		public ColorStateList getColorStateList(int index) {
			Object replacement = ((XResources) getResources()).getReplacement(getResourceId(index, 0));
			if (replacement instanceof ColorStateList) {
				return (ColorStateList) replacement;
			} else if (replacement instanceof Integer) {
				int color = (Integer) replacement;
				synchronized (sColorStateListCache) {
					ColorStateList result = sColorStateListCache.get(color);
					if (result == null) {
						result = ColorStateList.valueOf(color);
						sColorStateListCache.put(color, result);
					}
					return result;
				}
			} else if (replacement instanceof XResForwarder) {
				Resources repRes = ((XResForwarder) replacement).getResources();
				int repId = ((XResForwarder) replacement).getId();
				return repRes.getColorStateList(repId);
			}
			return super.getColorStateList(index);
		}

		@Override
		public float getDimension(int index, float defValue) {
			Object replacement = ((XResources) getResources()).getReplacement(getResourceId(index, 0));
			if (replacement instanceof XResForwarder) {
				Resources repRes = ((XResForwarder) replacement).getResources();
				int repId = ((XResForwarder) replacement).getId();
				return repRes.getDimension(repId);
			}
			return super.getDimension(index, defValue);
		}

		@Override
		public int getDimensionPixelOffset(int index, int defValue) {
			Object replacement = ((XResources) getResources()).getReplacement(getResourceId(index, 0));
			if (replacement instanceof XResForwarder) {
				Resources repRes = ((XResForwarder) replacement).getResources();
				int repId = ((XResForwarder) replacement).getId();
				return repRes.getDimensionPixelOffset(repId);
			}
			return super.getDimensionPixelOffset(index, defValue);
		}

		@Override
		public int getDimensionPixelSize(int index, int defValue) {
			Object replacement = ((XResources) getResources()).getReplacement(getResourceId(index, 0));
			if (replacement instanceof XResForwarder) {
				Resources repRes = ((XResForwarder) replacement).getResources();
				int repId = ((XResForwarder) replacement).getId();
				return repRes.getDimensionPixelSize(repId);
			}
			return super.getDimensionPixelSize(index, defValue);
		}

		@Override
		public Drawable getDrawable(int index) {
			final int resId = getResourceId(index, 0);
			XResources xres = (XResources) getResources();
			Object replacement = xres.getReplacement(resId);
			if (replacement instanceof DrawableLoader) {
				try {
					Drawable result = ((DrawableLoader) replacement).newDrawable(xres, resId);
					if (result != null)
						return result;
				} catch (Throwable t) { XposedBridge.log(t); }
			} else if (replacement instanceof Integer) {
				return new ColorDrawable((Integer) replacement);
			} else if (replacement instanceof XResForwarder) {
				Resources repRes = ((XResForwarder) replacement).getResources();
				int repId = ((XResForwarder) replacement).getId();
				return repRes.getDrawable(repId);
			}
			return super.getDrawable(index);
		}

		@Override
		public float getFloat(int index, float defValue) {
			Object replacement = ((XResources) getResources()).getReplacement(getResourceId(index, 0));
			if (replacement instanceof XResForwarder) {
				Resources repRes = ((XResForwarder) replacement).getResources();
				int repId = ((XResForwarder) replacement).getId();
				// dimensions seem to be the only way to define floats by references
				return repRes.getDimension(repId);
			}
			return super.getFloat(index, defValue);
		}

		@Override
		public Typeface getFont(int index) {
			Object replacement = ((XResources) getResources()).getReplacement(getResourceId(index, 0));
			if (replacement instanceof Typeface) {
				return (Typeface) replacement;
			} else if (replacement instanceof XResForwarder) {
				Resources repRes = ((XResForwarder) replacement).getResources();
				int repId = ((XResForwarder) replacement).getId();
				return repRes.getFont(repId);
			}
			return super.getFont(index);
		}

		@Override
		public float getFraction(int index, int base, int pbase, float defValue) {
			Object replacement = ((XResources) getResources()).getReplacement(getResourceId(index, 0));
			if (replacement instanceof XResForwarder) {
				Resources repRes = ((XResForwarder) replacement).getResources();
				int repId = ((XResForwarder) replacement).getId();
				// dimensions seem to be the only way to define floats by references
				return repRes.getFraction(repId, base, pbase);
			}
			return super.getFraction(index, base, pbase, defValue);
		}

		@Override
		public int getInt(int index, int defValue) {
			Object replacement = ((XResources) getResources()).getReplacement(getResourceId(index, 0));
			if (replacement instanceof Integer) {
				return (Integer) replacement;
			} else if (replacement instanceof XResForwarder) {
				Resources repRes = ((XResForwarder) replacement).getResources();
				int repId = ((XResForwarder) replacement).getId();
				return repRes.getInteger(repId);
			}
			return super.getInt(index, defValue);
		}

		@Override
		public int getInteger(int index, int defValue) {
			Object replacement = ((XResources) getResources()).getReplacement(getResourceId(index, 0));
			if (replacement instanceof Integer) {
				return (Integer) replacement;
			} else if (replacement instanceof XResForwarder) {
				Resources repRes = ((XResForwarder) replacement).getResources();
				int repId = ((XResForwarder) replacement).getId();
				return repRes.getInteger(repId);
			}
			return super.getInteger(index, defValue);
		}

		@Override
		public int getLayoutDimension(int index, int defValue) {
			Object replacement = ((XResources) getResources()).getReplacement(getResourceId(index, 0));
			if (replacement instanceof XResForwarder) {
				Resources repRes = ((XResForwarder) replacement).getResources();
				int repId = ((XResForwarder) replacement).getId();
				return repRes.getDimensionPixelSize(repId);
			}
			return super.getLayoutDimension(index, defValue);
		}

		@Override
		public int getLayoutDimension(int index, String name) {
			Object replacement = ((XResources) getResources()).getReplacement(getResourceId(index, 0));
			if (replacement instanceof XResForwarder) {
				Resources repRes = ((XResForwarder) replacement).getResources();
				int repId = ((XResForwarder) replacement).getId();
				return repRes.getDimensionPixelSize(repId);
			}
			return super.getLayoutDimension(index, name);
		}

		@Override
		public String getString(int index) {
			Object replacement = ((XResources) getResources()).getReplacement(getResourceId(index, 0));
			if (replacement instanceof CharSequence) {
				return replacement.toString();
			} else if (replacement instanceof XResForwarder) {
				Resources repRes = ((XResForwarder) replacement).getResources();
				int repId = ((XResForwarder) replacement).getId();
				return repRes.getString(repId);
			}
			return super.getString(index);
		}

		@Override
		public CharSequence getText(int index) {
			Object replacement = ((XResources) getResources()).getReplacement(getResourceId(index, 0));
			if (replacement instanceof CharSequence) {
				return (CharSequence) replacement;
			} else if (replacement instanceof XResForwarder) {
				Resources repRes = ((XResForwarder) replacement).getResources();
				int repId = ((XResForwarder) replacement).getId();
				return repRes.getText(repId);
			}
			return super.getText(index);
		}

		@Override
		public CharSequence[] getTextArray(int index) {
			Object replacement = ((XResources) getResources()).getReplacement(getResourceId(index, 0));
			if (replacement instanceof CharSequence[]) {
				return (CharSequence[]) replacement;
			} else if (replacement instanceof XResForwarder) {
				Resources repRes = ((XResForwarder) replacement).getResources();
				int repId = ((XResForwarder) replacement).getId();
				return repRes.getTextArray(repId);
			}
			return super.getTextArray(index);
		}

		@Override
		public boolean getValue(int index, TypedValue outValue) {
			var id = getResourceId(index, 0);
			Object replacement = ((XResources) getResources()).getReplacement(id);
			if (replacement instanceof XResForwarder) {
				Resources repRes = ((XResForwarder) replacement).getResources();
				int repId = ((XResForwarder) replacement).getId();
				repRes.getValue(repId, outValue, true);
				return outValue.type != TypedValue.TYPE_NULL;
			} else {
				if (replacement != null) {
					XposedBridge.log("Replacement of resource ID #0x" + Integer.toHexString(id) + " escaped because of deprecated replacement. Please use XResForwarder instead.");
				}
				return super.getValue(index, outValue);
			}
		}

		@Override
		public TypedValue peekValue(int index) {
			var id = getResourceId(index, 0);
			Object replacement = ((XResources) getResources()).getReplacement(id);
			if (replacement instanceof XResForwarder) {
				if (getBooleanField(this, "mRecycled")) {
					throw new RuntimeException("Cannot make calls to a recycled instance!");
				}
				final TypedValue value = (TypedValue) getObjectField(this, "mValue");
				Resources repRes = ((XResForwarder) replacement).getResources();
				int repId = ((XResForwarder) replacement).getId();
				repRes.getValue(repId, value, true);
				return value;
			} else {
				if (replacement != null) {
					XposedBridge.log("Replacement of resource ID #0x" + Integer.toHexString(id) + " escaped because of deprecated replacement. Please use XResForwarder instead.");
				}
				return super.peekValue(index);
			}
		}
	}


	// =======================================================
	//   DrawableLoader class
	// =======================================================
	/**
	 * Callback for drawable replacements. Instances of this class can passed to
	 * {@link #setReplacement(String, String, String, Object)} and its variants.
	 *
	 * <p class="caution">Make sure to always return new {@link Drawable} instances, as drawables
	 * usually can't be reused.
	 */
	@SuppressWarnings("UnusedParameters")
	public static abstract class DrawableLoader {
		/**
		 * Constructor.
		 */
		public DrawableLoader() {}

		/**
		 * Called when the hooked drawable resource has been requested.
		 *
		 * @param res The {@link XResources} object in which the hooked drawable resides.
		 * @param id The resource ID which has been requested.
		 * @return The {@link Drawable} which should be used as replacement. {@code null} is ignored.
		 * @throws Throwable Everything the callback throws is caught and logged.
		 */
		public abstract Drawable newDrawable(XResources res, int id) throws Throwable;

		/**
		 * Like {@link #newDrawable}, but called for {@link #getDrawableForDensity}. The default
		 * implementation is to use the result of {@link #newDrawable}.
		 *
		 * @param res The {@link XResources} object in which the hooked drawable resides.
		 * @param id The resource ID which has been requested.
		 * @param density The desired screen density indicated by the resource as found in
		 *                {@link DisplayMetrics}.
		 * @return The {@link Drawable} which should be used as replacement. {@code null} is ignored.
		 * @throws Throwable Everything the callback throws is caught and logged.
		 */
		public Drawable newDrawableForDensity(XResources res, int id, int density) throws Throwable {
			return newDrawable(res, id);
		}
	}


	// =======================================================
	//   DimensionReplacement class
	// =======================================================
	/**
	 * Callback for dimension replacements. Instances of this class can passed to
	 * {@link #setReplacement(String, String, String, Object)} and its variants.
	 */
	public static class DimensionReplacement {
		private final float mValue;
		private final int mUnit;

		/**
		 * Creates an instance that can be used for {@link #setReplacement(String, String, String, Object)}
		 * to replace a dimension resource.
		 *
		 * @param value The value of the replacement, in the unit specified with the next parameter.
		 * @param unit One of the {@code COMPLEX_UNIT_*} constants in {@link TypedValue}.
		 */
		public DimensionReplacement(float value, int unit) {
			mValue = value;
			mUnit = unit;
		}

		/** Called by {@link android.content.res.Resources#getDimension}. */
		public float getDimension(DisplayMetrics metrics) {
			return TypedValue.applyDimension(mUnit, mValue, metrics);
		}

		/** Called by {@link android.content.res.Resources#getDimensionPixelOffset}. */
		public int getDimensionPixelOffset(DisplayMetrics metrics) {
			return (int) TypedValue.applyDimension(mUnit, mValue, metrics);
		}

		/** Called by {@link android.content.res.Resources#getDimensionPixelSize}. */
		public int getDimensionPixelSize(DisplayMetrics metrics) {
			final float f = TypedValue.applyDimension(mUnit, mValue, metrics);
			final int res = (int)(f+0.5f);
			if (res != 0) return res;
			if (mValue == 0) return 0;
			if (mValue > 0) return 1;
			return -1;
		}
	}

	// =======================================================
	//   INFLATING LAYOUTS
	// =======================================================

	private class XMLInstanceDetails {
		public final ResourceNames resNames;
		public final String variant;
		public final CopyOnWriteSortedSet<XC_LayoutInflated> callbacks;
		public final XResources res = XResources.this;

		private XMLInstanceDetails(ResourceNames resNames, String variant, CopyOnWriteSortedSet<XC_LayoutInflated> callbacks) {
			this.resNames = resNames;
			this.variant = variant;
			this.callbacks = callbacks;
		}
	}

	/**
	 * Hook the inflation of a layout.
	 *
	 * @param id The ID of the resource which should be replaced.
	 * @param callback The callback to be executed when the layout has been inflated.
	 * @return An object which can be used to remove the callback again.
	 */
	public XC_LayoutInflated.Unhook hookLayout(int id, XC_LayoutInflated callback) {
		return hookLayoutInternal(mResDir, id, getResourceNames(id), callback);
	}

	/**
	 * Hook the inflation of a layout.
	 *
	 * @deprecated Use {@link #hookLayout(String, String, String, XC_LayoutInflated)} instead.
	 *
	 * @param fullName The full resource name, e.g. {@code com.android.systemui:layout/statusbar}.
	 *                 See {@link #getResourceName}.
	 * @param callback The callback to be executed when the layout has been inflated.
	 * @return An object which can be used to remove the callback again.
	 */
	@Deprecated
	public XC_LayoutInflated.Unhook hookLayout(String fullName, XC_LayoutInflated callback) {
		int id = getIdentifier(fullName, null, null);
		if (id == 0)
			throw new NotFoundException(fullName);
		return hookLayout(id, callback);
	}

	/**
	 * Hook the inflation of a layout.
	 *
	 * @param pkg The package name, e.g. {@code com.android.systemui}.
	 *            See {@link #getResourcePackageName}.
	 * @param type The type name, e.g. {@code layout}.
	 *            See {@link #getResourceTypeName}.
	 * @param name The entry name, e.g. {@code statusbar}.
	 *            See {@link #getResourceEntryName}.
	 * @param callback The callback to be executed when the layout has been inflated.
	 * @return An object which can be used to remove the callback again.
	 */
	public XC_LayoutInflated.Unhook hookLayout(String pkg, String type, String name, XC_LayoutInflated callback) {
		int id = getIdentifier(name, type, pkg);
		if (id == 0)
			throw new NotFoundException(pkg + ":" + type + "/" + name);
		return hookLayout(id, callback);
	}

	/**
	 * Hook the inflation of an Android framework layout (in the {@code android} package).
	 * See {@link #hookSystemWideLayout(String, String, String, XC_LayoutInflated)}.
	 *
	 * @param id The ID of the resource which should be replaced.
	 * @param callback The callback to be executed when the layout has been inflated.
	 * @return An object which can be used to remove the callback again.
	 */
	public static XC_LayoutInflated.Unhook hookSystemWideLayout(int id, XC_LayoutInflated callback) {
		if (id >= 0x7f000000)
			throw new IllegalArgumentException("ids >= 0x7f000000 are app specific and cannot be set for the framework");
		return hookLayoutInternal(null, id, getSystemResourceNames(id), callback);
	}

	/**
	 * Hook the inflation of an Android framework layout (in the {@code android} package).
	 * See {@link #hookSystemWideLayout(String, String, String, XC_LayoutInflated)}.
	 *
	 * @deprecated Use {@link #hookSystemWideLayout(String, String, String, XC_LayoutInflated)} instead.
	 *
	 * @param fullName The full resource name, e.g. {@code android:layout/simple_list_item_1}.
	 *                 See {@link #getResourceName}.
	 * @param callback The callback to be executed when the layout has been inflated.
	 * @return An object which can be used to remove the callback again.
	 */
	@Deprecated
	public static XC_LayoutInflated.Unhook hookSystemWideLayout(String fullName, XC_LayoutInflated callback) {
		int id = getSystem().getIdentifier(fullName, null, null);
		if (id == 0)
			throw new NotFoundException(fullName);
		return hookSystemWideLayout(id, callback);
	}

	/**
	 * Hook the inflation of an Android framework layout (in the {@code android} package).
	 *
	 * <p>Some layouts are part of the Android framework and can be used in any app. They're
	 * accessible via {@link android.R.layout android.R.layout} and are not bound to a specific
	 * {@link android.content.res.Resources} instance. Such resources can be replaced in
	 * {@link IXposedHookZygoteInit#initZygote initZygote()} for all apps. As there is no
	 * {@link XResources} object easily available in that scope, this static method can be used
	 * to hook layouts.
	 *
	 * @param pkg The package name, e.g. {@code android}.
	 *            See {@link #getResourcePackageName}.
	 * @param type The type name, e.g. {@code layout}.
	 *            See {@link #getResourceTypeName}.
	 * @param name The entry name, e.g. {@code simple_list_item_1}.
	 *            See {@link #getResourceEntryName}.
	 * @param callback The callback to be executed when the layout has been inflated.
	 * @return An object which can be used to remove the callback again.
	 */
	public static XC_LayoutInflated.Unhook hookSystemWideLayout(String pkg, String type, String name, XC_LayoutInflated callback) {
		int id = getSystem().getIdentifier(name, type, pkg);
		if (id == 0)
			throw new NotFoundException(pkg + ":" + type + "/" + name);
		return hookSystemWideLayout(id, callback);
	}

	private static XC_LayoutInflated.Unhook hookLayoutInternal(String resDir, int id, ResourceNames resNames, XC_LayoutInflated callback) {
		if (id == 0)
			throw new IllegalArgumentException("id 0 is not an allowed resource identifier");

		if (resDir == null) {
			try {
				XposedInit.hookResources();
			} catch (Throwable throwable) {
				throw new IllegalStateException("Failed to initialize resources hook", throwable);
			}
		}

		HashMap<String, CopyOnWriteSortedSet<XC_LayoutInflated>> inner;
		synchronized (sLayoutCallbacks) {
			inner = sLayoutCallbacks.get(id);
			if (inner == null) {
				inner = new HashMap<>();
				sLayoutCallbacks.put(id, inner);
			}
		}

		CopyOnWriteSortedSet<XC_LayoutInflated> callbacks;
		synchronized (inner) {
			callbacks = inner.get(resDir);
			if (callbacks == null) {
				callbacks = new CopyOnWriteSortedSet<>();
				inner.put(resDir, callbacks);
			}
		}

		callbacks.add(callback);

		putResourceNames(resDir, resNames);

		return callback.new Unhook(resDir, id);
	}

	/** @hide */
	public static void unhookLayout(String resDir, int id, XC_LayoutInflated callback) {
		HashMap<String, CopyOnWriteSortedSet<XC_LayoutInflated>> inner;
		synchronized (sLayoutCallbacks) {
			inner = sLayoutCallbacks.get(id);
			if (inner == null)
				return;
		}

		CopyOnWriteSortedSet<XC_LayoutInflated> callbacks;
		synchronized (inner) {
			callbacks = inner.get(resDir);
			if (callbacks == null)
				return;
		}

		callbacks.remove(callback);
	}
}
