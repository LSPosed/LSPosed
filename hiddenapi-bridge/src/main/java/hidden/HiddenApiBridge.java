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
 * Copyright (C) 2021 LSPosed Contributors
 */

package hidden;

import android.content.res.AssetManager;
import android.os.Binder;
import android.os.IBinder;

public class HiddenApiBridge {
    public static int AssetManager_addAssetPath(AssetManager am, String path) {
        return am.addAssetPath(path);
    }

    public static IBinder Binder_allowBlocking(IBinder binder) {
        return Binder.allowBlocking(binder);
    }
}
