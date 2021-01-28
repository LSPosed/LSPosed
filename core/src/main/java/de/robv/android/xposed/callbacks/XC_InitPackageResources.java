package de.robv.android.xposed.callbacks;

import android.content.res.XResources;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.XposedBridge.CopyOnWriteSortedSet;

/**
 * This class is only used for internal purposes, except for the {@link InitPackageResourcesParam}
 * subclass.
 */
public abstract class XC_InitPackageResources extends XCallback implements IXposedHookInitPackageResources {
	/**
	 * Creates a new callback with default priority.
	 * @hide
	 */
	@SuppressWarnings("deprecation")
	public XC_InitPackageResources() {
		super();
	}

	/**
	 * Creates a new callback with a specific priority.
	 *
	 * @param priority See {@link XCallback#priority}.
	 * @hide
	 */
	public XC_InitPackageResources(int priority) {
		super(priority);
	}

	/**
	 * Wraps information about the resources being initialized.
	 */
	public static final class InitPackageResourcesParam extends XCallback.Param {
		/** @hide */
		public InitPackageResourcesParam(CopyOnWriteSortedSet<XC_InitPackageResources> callbacks) {
			super(callbacks);
		}

		/** The name of the package for which resources are being loaded. */
		public String packageName;

		/**
		 * Reference to the resources that can be used for calls to
		 * {@link XResources#setReplacement(String, String, String, Object)}.
		 */
		public XResources res;
	}

	/** @hide */
	@Override
	protected void call(Param param) throws Throwable {
		if (param instanceof InitPackageResourcesParam)
			handleInitPackageResources((InitPackageResourcesParam) param);
	}
}
