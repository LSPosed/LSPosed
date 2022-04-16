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

import android.content.res.XResources;
import android.content.res.XResources.ResourceNames;
import android.view.View;

import de.robv.android.xposed.XposedBridge.CopyOnWriteSortedSet;

/**
 * Callback for hooking layouts. Such callbacks can be passed to {@link XResources#hookLayout}
 * and its variants.
 */
public abstract class XC_LayoutInflated extends XCallback implements Comparable<XC_LayoutInflated> {
    /**
     * Creates a new callback with default priority.
     */
    @SuppressWarnings("deprecation")
    public XC_LayoutInflated() {
        super();
    }

    /**
     * Creates a new callback with a specific priority.
     *
     * @param priority See {@link XCallback#priority}.
     */
    public XC_LayoutInflated(int priority) {
        super(priority);
    }

    /**
     * Wraps information about the inflated layout.
     */
    public static final class LayoutInflatedParam extends XCallback.Param {
        /**
         * @hide
         */
        public LayoutInflatedParam(CopyOnWriteSortedSet<XC_LayoutInflated> callbacks) {
            super(callbacks.getSnapshot(new XCallback[0]));
        }

        /**
         * The view that has been created from the layout.
         */
        public View view;

        /**
         * Container with the ID and name of the underlying resource.
         */
        public ResourceNames resNames;

        /**
         * Directory from which the layout was actually loaded (e.g. "layout-sw600dp").
         */
        public String variant;

        /**
         * Resources containing the layout.
         */
        public XResources res;
    }

    /** @hide */
    @Override
    public int compareTo(XC_LayoutInflated other) {
        if (this == other)
            return 0;

        // order descending by priority
        if (other.priority != this.priority)
            return other.priority - this.priority;
            // then randomly
        else if (System.identityHashCode(this) < System.identityHashCode(other))
            return -1;
        else
            return 1;
    }

    /**
     * @hide
     */
    @Override
    protected void call(Param param) throws Throwable {
        if (param instanceof LayoutInflatedParam)
            handleLayoutInflated((LayoutInflatedParam) param);
    }

    /**
     * This method is called when the hooked layout has been inflated.
     *
     * @param liparam Information about the layout and the inflated view.
     * @throws Throwable Everything the callback throws is caught and logged.
     */
    public abstract void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable;

    /**
     * An object with which the callback can be removed.
     */
    public class Unhook implements IXUnhook<XC_LayoutInflated> {
        private final String resDir;
        private final int id;

        /**
         * @hide
         */
        public Unhook(String resDir, int id) {
            this.resDir = resDir;
            this.id = id;
        }

        /**
         * Returns the resource ID of the hooked layout.
         */
        public int getId() {
            return id;
        }

        @Override
        public XC_LayoutInflated getCallback() {
            return XC_LayoutInflated.this;
        }

        @Override
        public void unhook() {
            XResources.unhookLayout(resDir, id, XC_LayoutInflated.this);
        }

    }
}
