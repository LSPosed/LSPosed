/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package external.com.android.dx.dex.file;

import external.com.android.dx.rop.cst.Constant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * File header section of a {@code .dex} file.
 */
public final class HeaderSection extends UniformItemSection {
    /** {@code non-null;} the list of the one item in the section */
    private final List<HeaderItem> list;

    /**
     * Constructs an instance. The file offset is initially unknown.
     *
     * @param file {@code non-null;} file that this instance is part of
     */
    public HeaderSection(DexFile file) {
        super(null, file, 4);

        HeaderItem item = new HeaderItem();
        item.setIndex(0);

        this.list = Collections.singletonList(item);
    }

    /** {@inheritDoc} */
    @Override
    public IndexedItem get(Constant cst) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Collection<? extends Item> items() {
        return list;
    }

    /** {@inheritDoc} */
    @Override
    protected void orderItems() {
        // Nothing to do here.
    }
}
