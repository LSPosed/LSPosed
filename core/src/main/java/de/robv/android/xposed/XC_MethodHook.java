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

import java.lang.reflect.Executable;
import java.lang.reflect.Member;
import java.util.HashMap;

import de.robv.android.xposed.callbacks.IXUnhook;
import de.robv.android.xposed.callbacks.XCallback;

/**
 * Callback class for method hooks.
 *
 * <p>Usually, anonymous subclasses of this class are created which override
 * {@link #beforeHookedMethod} and/or {@link #afterHookedMethod}.
 */
public abstract class XC_MethodHook extends XCallback {
    /**
     * Creates a new callback with default priority.
     */
    @SuppressWarnings("deprecation")
    public XC_MethodHook() {
        super();
    }

    /**
     * Creates a new callback with a specific priority.
     *
     * <p class="note">Note that {@link #afterHookedMethod} will be called in reversed order, i.e.
     * the callback with the highest priority will be called last. This way, the callback has the
     * final control over the return value. {@link #beforeHookedMethod} is called as usual, i.e.
     * highest priority first.
     *
     * @param priority See {@link XCallback#priority}.
     */
    public XC_MethodHook(int priority) {
        super(priority);
    }

    /**
     * Called before the invocation of the method.
     *
     * <p>You can use {@link MethodHookParam#setResult} and {@link MethodHookParam#setThrowable}
     * to prevent the original method from being called.
     *
     * <p>Note that implementations shouldn't call {@code super(param)}, it's not necessary.
     *
     * @param param Information about the method call.
     * @throws Throwable Everything the callback throws is caught and logged.
     */
    protected void beforeHookedMethod(MethodHookParam<?> param) throws Throwable {
    }

    public void callBeforeHookedMethod(MethodHookParam<?> param) throws Throwable {
        beforeHookedMethod(param);
    }

    /**
     * Called after the invocation of the method.
     *
     * <p>You can use {@link MethodHookParam#setResult} and {@link MethodHookParam#setThrowable}
     * to modify the return value of the original method.
     *
     * <p>Note that implementations shouldn't call {@code super(param)}, it's not necessary.
     *
     * @param param Information about the method call.
     * @throws Throwable Everything the callback throws is caught and logged.
     */
    protected void afterHookedMethod(MethodHookParam<?> param) throws Throwable {
    }

    public void callAfterHookedMethod(MethodHookParam<?> param) throws Throwable {
        afterHookedMethod(param);
    }

    /**
     * Wraps information about the method call and allows to influence it.
     */
    public static final class MethodHookParam<T extends Executable> extends XCallback.Param {
        /**
         * @hide
         */
        @SuppressWarnings("deprecation")
        public MethodHookParam() {
            super();
        }

        /**
         * The hooked method/constructor.
         */
        public Member method;

        /**
         * The {@code this} reference for an instance method, or {@code null} for static methods.
         */
        public Object thisObject;

        /**
         * Arguments to the method call.
         */
        public Object[] args;

        public Object result = null;
        public Throwable throwable = null;
        public boolean returnEarly = false;

        private final HashMap<String, Object> extras = new HashMap<>();

        /**
         * Returns the result of the method call.
         */
        public Object getResult() {
            return result;
        }

        /**
         * Modify the result of the method call.
         *
         * <p>If called from {@link #beforeHookedMethod}, it prevents the call to the original method.
         */
        public void setResult(Object result) {
            this.result = result;
            this.throwable = null;
            this.returnEarly = true;
        }

        /**
         * Returns the {@link Throwable} thrown by the method, or {@code null}.
         */
        public Throwable getThrowable() {
            return throwable;
        }

        /**
         * Returns true if an exception was thrown by the method.
         */
        public boolean hasThrowable() {
            return throwable != null;
        }

        /**
         * Modify the exception thrown of the method call.
         *
         * <p>If called from {@link #beforeHookedMethod}, it prevents the call to the original method.
         */
        public void setThrowable(Throwable throwable) {
            this.throwable = throwable;
            this.result = null;
            this.returnEarly = true;
        }

        /**
         * Returns the result of the method call, or throws the Throwable caused by it.
         */
        public Object getResultOrThrowable() throws Throwable {
            if (throwable != null)
                throw throwable;
            return result;
        }
    }

    /**
     * An object with which the method/constructor can be unhooked.
     */
    public class Unhook implements IXUnhook<XC_MethodHook> {
        private final Member hookMethod;

        /*package*/ Unhook(Member hookMethod) {
            this.hookMethod = hookMethod;
        }

        /**
         * Returns the method/constructor that has been hooked.
         */
        public Member getHookedMethod() {
            return hookMethod;
        }

        @Override
        public XC_MethodHook getCallback() {
            return XC_MethodHook.this;
        }

        @SuppressWarnings("deprecation")
        @Override
        public void unhook() {
            XposedBridge.unhookMethod(hookMethod, XC_MethodHook.this);
        }

    }
}
