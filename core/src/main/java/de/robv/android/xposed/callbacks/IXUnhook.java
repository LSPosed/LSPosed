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

import de.robv.android.xposed.IXposedHookZygoteInit;

/**
 * Interface for objects that can be used to remove callbacks.
 *
 * <p class="warning">Just like hooking methods etc., unhooking applies only to the current process.
 * In other process (or when the app is removed from memory and then restarted), the hook will still
 * be active. The Zygote process (see {@link IXposedHookZygoteInit}) is an exception, the hook won't
 * be inherited by any future processes forked from it in the future.
 *
 * @param <T> The class of the callback.
 */
public interface IXUnhook<T> {
    /**
     * Returns the callback that has been registered.
     */
    T getCallback();

    /**
     * Removes the callback.
     */
    void unhook();
}
