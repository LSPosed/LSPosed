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

import external.com.android.dex.DexFormat;
import external.com.android.dex.DexIndexOverflowException;
import java.util.Formatter;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Member (field or method) refs list section of a {@code .dex} file.
 */
public abstract class MemberIdsSection extends UniformItemSection {

    /**
     * Constructs an instance. The file offset is initially unknown.
     *
     * @param name {@code null-ok;} the name of this instance, for annotation
     * purposes
     * @param file {@code non-null;} file that this instance is part of
     */
    public MemberIdsSection(String name, DexFile file) {
        super(name, file, 4);
    }

    /** {@inheritDoc} */
    @Override
    protected void orderItems() {
        int idx = 0;

        if (items().size() > DexFormat.MAX_MEMBER_IDX + 1) {
            throw new DexIndexOverflowException(getTooManyMembersMessage());
        }

        for (Object i : items()) {
            ((MemberIdItem) i).setIndex(idx);
            idx++;
        }
    }

    private String getTooManyMembersMessage() {
        Map<String, AtomicInteger> membersByPackage = new TreeMap<String, AtomicInteger>();
        for (Object member : items()) {
            String packageName = ((MemberIdItem) member).getDefiningClass().getPackageName();
            AtomicInteger count = membersByPackage.get(packageName);
            if (count == null) {
                count = new AtomicInteger();
                membersByPackage.put(packageName, count);
            }
            count.incrementAndGet();
        }

        Formatter formatter = new Formatter();
        try {
            String memberType = this instanceof MethodIdsSection ? "method" : "field";
            formatter.format("Too many %1$s references to fit in one dex file: %2$d; max is %3$d.%n" +
                            "You may try using multi-dex. If multi-dex is enabled then the list of " +
                            "classes for the main dex list is too large.%n" +
                    "References by package:",
                    memberType, items().size(), DexFormat.MAX_MEMBER_IDX + 1);
            for (Map.Entry<String, AtomicInteger> entry : membersByPackage.entrySet()) {
                formatter.format("%n%6d %s", entry.getValue().get(), entry.getKey());
            }
            return formatter.toString();
        } finally {
            formatter.close();
        }
    }

}
