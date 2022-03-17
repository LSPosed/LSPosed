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

package de.robv.android.xposed;

import de.robv.android.xposed.callbacks.XCallback;

/**
 * A special case of {@link XC_MethodHook} which completely replaces the original method.
 */
public abstract class XC_MethodReplacement extends XC_MethodHook {
    /**
     * Creates a new callback with default priority.
     */
    public XC_MethodReplacement() {
        super();
    }

    /**
     * Creates a new callback with a specific priority.
     *
     * @param priority See {@link XCallback#priority}.
     */
    public XC_MethodReplacement(int priority) {
        super(priority);
    }

    /**
     * @hide
     */
    @Override
    protected final void beforeHookedMethod(MethodHookParam param) throws Throwable {
        try {
            Object result = replaceHookedMethod(param);
            param.setResult(result);
        } catch (Throwable t) {
            XposedBridge.log(t);
            param.setThrowable(t);
        }
    }

    /**
     * @hide
     */
    @Override
    @SuppressWarnings("EmptyMethod")
    protected final void afterHookedMethod(MethodHookParam param) throws Throwable {
    }

    /**
     * Shortcut for replacing a method completely. Whatever is returned/thrown here is taken
     * instead of the result of the original method (which will not be called).
     *
     * <p>Note that implementations shouldn't call {@code super(param)}, it's not necessary.
     *
     * @param param Information about the method call.
     * @throws Throwable Anything that is thrown by the callback will be passed on to the original caller.
     */
    @SuppressWarnings("UnusedParameters")
    protected abstract Object replaceHookedMethod(MethodHookParam param) throws Throwable;

    /**
     * Predefined callback that skips the method without replacements.
     */
    public static final XC_MethodReplacement DO_NOTHING = new XC_MethodReplacement(PRIORITY_HIGHEST * 2) {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            return null;
        }
    };

    /**
     * Creates a callback which always returns a specific value.
     *
     * @param result The value that should be returned to callers of the hooked method.
     */
    public static XC_MethodReplacement returnConstant(final Object result) {
        return returnConstant(PRIORITY_DEFAULT, result);
    }

    /**
     * Like {@link #returnConstant(Object)}, but allows to specify a priority for the callback.
     *
     * @param priority See {@link XCallback#priority}.
     * @param result   The value that should be returned to callers of the hooked method.
     */
    public static XC_MethodReplacement returnConstant(int priority, final Object result) {
        return new XC_MethodReplacement(priority) {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                return result;
            }
        };
    }

}
