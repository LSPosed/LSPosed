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

package de.robv.android.xposed.callbacks;

import android.os.Bundle;

import org.lsposed.lspd.deopt.PrebuiltMethodsDeopter;

import java.io.Serializable;

import de.robv.android.xposed.XposedBridge;

/**
 * Base class for Xposed callbacks.
 * <p>
 * This class only keeps a priority for ordering multiple callbacks.
 * The actual (abstract) callback methods are added by subclasses.
 */
abstract public class XCallback {
    /**
     * Callback priority, higher number means earlier execution.
     *
     * <p>This is usually set to {@link #PRIORITY_DEFAULT}. However, in case a certain callback should
     * be executed earlier or later a value between {@link #PRIORITY_HIGHEST} and {@link #PRIORITY_LOWEST}
     * can be set instead. The values are just for orientation though, Xposed doesn't enforce any
     * boundaries on the priority values.
     */
    public final int priority;

    /**
     * @deprecated This constructor can't be hidden for technical reasons. Nevertheless, don't use it!
     */
    @Deprecated
    public XCallback() {
        this.priority = PRIORITY_DEFAULT;
    }

    /**
     * @hide
     */
    public XCallback(int priority) {
        this.priority = priority;
    }

    /**
     * Base class for Xposed callback parameters.
     */
    public static abstract class Param {
        /**
         * @hide
         */
        public final XCallback[] callbacks;
        private Bundle extra;

        /**
         * @deprecated This constructor can't be hidden for technical reasons. Nevertheless, don't use it!
         */
        @Deprecated
        protected Param() {
            callbacks = null;
        }

        /**
         * @hide
         */
        protected Param(XCallback[] callbacks) {
            this.callbacks = callbacks;
        }

        /**
         * This can be used to store any data for the scope of the callback.
         *
         * <p>Use this instead of instance variables, as it has a clear reference to e.g. each
         * separate call to a method, even when the same method is called recursively.
         *
         * @see #setObjectExtra
         * @see #getObjectExtra
         */
        public synchronized Bundle getExtra() {
            if (extra == null)
                extra = new Bundle();
            return extra;
        }

        /**
         * Returns an object stored with {@link #setObjectExtra}.
         */
        public Object getObjectExtra(String key) {
            Serializable o = getExtra().getSerializable(key);
            if (o instanceof SerializeWrapper)
                return ((SerializeWrapper) o).object;
            return null;
        }

        /**
         * Stores any object for the scope of the callback. For data types that support it, use
         * the {@link Bundle} returned by {@link #getExtra} instead.
         */
        public void setObjectExtra(String key, Object o) {
            getExtra().putSerializable(key, new SerializeWrapper(o));
        }

        private static class SerializeWrapper implements Serializable {
            private static final long serialVersionUID = 1L;
            private final Object object;

            public SerializeWrapper(Object o) {
                object = o;
            }
        }
    }

    /**
     * @hide
     */
    public static void callAll(Param param) {

        if (param instanceof XC_LoadPackage.LoadPackageParam) {
            // deopt methods in system apps or priv-apps, this would be not necessary
            // only if we found out how to recompile their apks
            XC_LoadPackage.LoadPackageParam lpp = (XC_LoadPackage.LoadPackageParam) param;
            PrebuiltMethodsDeopter.deoptMethods(lpp.packageName, lpp.classLoader);
        }

        if (param.callbacks == null)
            throw new IllegalStateException("This object was not created for use with callAll");

        for (int i = 0; i < param.callbacks.length; i++) {
            try {
                param.callbacks[i].call(param);
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        }
    }

    /**
     * @hide
     */
    protected void call(Param param) throws Throwable {
    }


    /**
     * The default priority, see {@link #priority}.
     */
    public static final int PRIORITY_DEFAULT = 50;

    /**
     * Execute this callback late, see {@link #priority}.
     */
    public static final int PRIORITY_LOWEST = -10000;

    /**
     * Execute this callback early, see {@link #priority}.
     */
    public static final int PRIORITY_HIGHEST = 10000;
}
