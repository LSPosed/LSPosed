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

package de.robv.android.xposed.services;

import java.io.InputStream;

/**
 * Holder for the result of a {@link BaseService#readFile} or  {@link BaseService#statFile} call.
 */
public final class FileResult {
    /**
     * File content, might be {@code null} if the file wasn't read.
     */
    public final byte[] content;
    /**
     * File input stream, might be {@code null} if the file wasn't read.
     */
    public final InputStream stream;
    /**
     * File size.
     */
    public final long size;
    /**
     * File last modification time.
     */
    public final long mtime;

    /*package*/ FileResult(long size, long mtime) {
        this.content = null;
        this.stream = null;
        this.size = size;
        this.mtime = mtime;
    }

    /*package*/ FileResult(byte[] content, long size, long mtime) {
        this.content = content;
        this.stream = null;
        this.size = size;
        this.mtime = mtime;
    }

    /*package*/ FileResult(InputStream stream, long size, long mtime) {
        this.content = null;
        this.stream = stream;
        this.size = size;
        this.mtime = mtime;
    }

    /**
     * @hide
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        if (content != null) {
            sb.append("content.length: ");
            sb.append(content.length);
            sb.append(", ");
        }
        if (stream != null) {
            sb.append("stream: ");
            sb.append(stream.toString());
            sb.append(", ");
        }
        sb.append("size: ");
        sb.append(size);
        sb.append(", mtime: ");
        sb.append(mtime);
        sb.append("}");
        return sb.toString();
    }
}
