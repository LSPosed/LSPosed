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

package external.com.android.dx.cf.iface;

import external.com.android.dx.util.ByteArray;

/**
 * Observer of parsing in action. This is used to supply feedback from
 * the various things that parse particularly to the dumping utilities.
 */
public interface ParseObserver {
    /**
     * Indicate that the level of indentation for a dump should increase
     * or decrease (positive or negative argument, respectively).
     *
     * @param indentDelta the amount to change indentation
     */
    public void changeIndent(int indentDelta);

    /**
     * Indicate that a particular member is now being parsed.
     *
     * @param bytes {@code non-null;} the source that is being parsed
     * @param offset offset into {@code bytes} for the start of the
     * member
     * @param name {@code non-null;} name of the member
     * @param descriptor {@code non-null;} descriptor of the member
     */
    public void startParsingMember(ByteArray bytes, int offset, String name,
                                   String descriptor);

    /**
     * Indicate that a particular member is no longer being parsed.
     *
     * @param bytes {@code non-null;} the source that was parsed
     * @param offset offset into {@code bytes} for the end of the
     * member
     * @param name {@code non-null;} name of the member
     * @param descriptor {@code non-null;} descriptor of the member
     * @param member {@code non-null;} the actual member that was parsed
     */
    public void endParsingMember(ByteArray bytes, int offset, String name,
                                 String descriptor, Member member);

    /**
     * Indicate that some parsing happened.
     *
     * @param bytes {@code non-null;} the source that was parsed
     * @param offset offset into {@code bytes} for what was parsed
     * @param len number of bytes parsed
     * @param human {@code non-null;} human form for what was parsed
     */
    public void parsed(ByteArray bytes, int offset, int len, String human);
}
