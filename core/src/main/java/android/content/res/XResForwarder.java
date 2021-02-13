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

/**
 * Instances of this class can be used for {@link XResources#setReplacement(String, String, String, Object)}
 * and its variants. They forward the resource request to a different {@link android.content.res.Resources}
 * instance with a possibly different ID.
 *
 * <p>Usually, instances aren't created directly but via {@link XModuleResources#fwd}.
 */
public class XResForwarder {
	private final Resources res;
	private final int id;

	/**
	 * Creates a new instance.
	 *
	 * @param res The target {@link android.content.res.Resources} instance to forward requests to.
	 * @param id The target resource ID.
	 */
	public XResForwarder(Resources res, int id) {
		this.res = res;
		this.id = id;
	}

	/** Returns the target {@link android.content.res.Resources} instance. */
	public Resources getResources() {
		return res;
	}

	/** Returns the target resource ID. */
	public int getId() {
		return id;
	}
}
