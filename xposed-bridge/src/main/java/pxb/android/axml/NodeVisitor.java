/*
 * Copyright (c) 2009-2013 Panxiaobo
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
package pxb.android.axml;

public abstract class NodeVisitor {

    public static final int TYPE_FIRST_INT = 0x10;
    public static final int TYPE_INT_BOOLEAN = 0x12;
    public static final int TYPE_INT_HEX = 0x11;
    public static final int TYPE_REFERENCE = 0x01;
    public static final int TYPE_STRING = 0x03;
    protected NodeVisitor nv;

    public NodeVisitor() {
        super();
    }

    public NodeVisitor(NodeVisitor nv) {
        super();
        this.nv = nv;
    }

    /**
     * add attribute to the node
     * 
     * @param ns
     * @param name
     * @param resourceId
     * @param type
     *            {@link #TYPE_STRING} or others
     * @param obj
     *            a string for {@link #TYPE_STRING} ,and Integer for others
     */
    public void attr(String ns, String name, int resourceId, int type, Object obj) {
        if (nv != null) {
            nv.attr(ns, name, resourceId, type, obj);
        }
    }

    /**
     * create a child node
     * 
     * @param ns
     * @param name
     * @return
     */
    public NodeVisitor child(String ns, String name) {
        if (nv != null) {
            return nv.child(ns, name);
        }
        return null;
    }

    /**
     * end the visit
     */
    public void end() {
        if (nv != null) {
            nv.end();
        }
    }

    /**
     * line number in the .xml
     * 
     * @param ln
     */
    public void line(int ln) {
        if (nv != null) {
            nv.line(ln);
        }
    }

    /**
     * the node text
     * 
     * @param value
     */
    public void text(int lineNumber, String value) {
        if (nv != null) {
            nv.text(lineNumber, value);
        }
    }
}
