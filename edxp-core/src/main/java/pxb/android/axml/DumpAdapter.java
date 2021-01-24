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

import java.util.HashMap;
import java.util.Map;

/**
 * dump axml to stdout
 * 
 * @author <a href="mailto:pxb1988@gmail.com">Panxiaobo</a>
 */
public class DumpAdapter extends AxmlVisitor {
    protected int deep;
    protected Map<String, String> nses;

    public DumpAdapter() {
        this(null);
    }

    public DumpAdapter(NodeVisitor nv) {
        this(nv, 0, new HashMap<String, String>());
    }

    public DumpAdapter(NodeVisitor nv, int x, Map<String, String> nses) {
        super(nv);
        this.deep = x;
        this.nses = nses;
    }

    @Override
    public void attr(String ns, String name, int resourceId, int type, Object obj) {
        for (int i = 0; i < deep; i++) {
            System.out.print("  ");
        }
        if (ns != null) {
            System.out.print(String.format("%s:", getPrefix(ns)));
        }
        System.out.print(name);
        if (resourceId != -1) {
            System.out.print(String.format("(%08x)", resourceId));
        }
        if (obj instanceof String) {
            System.out.print(String.format("=[%08x]\"%s\"", type, obj));
        } else if (obj instanceof Boolean) {
            System.out.print(String.format("=[%08x]\"%b\"", type, obj));
        } else if (obj instanceof ValueWrapper) {
            ValueWrapper w = (ValueWrapper) obj;
            System.out.print(String.format("=[%08x]@%08x, raw: \"%s\"", type, w.ref, w.raw));
        } else if (type == TYPE_REFERENCE) {
            System.out.print(String.format("=[%08x]@%08x", type, obj));
        } else {
            System.out.print(String.format("=[%08x]%08x", type, obj));
        }
        System.out.println();
        super.attr(ns, name, resourceId, type, obj);
    }

    @Override
    public NodeVisitor child(String ns, String name) {
        for (int i = 0; i < deep; i++) {
            System.out.print("  ");
        }
        System.out.print("<");
        if (ns != null) {
            System.out.print(getPrefix(ns) + ":");
        }
        System.out.println(name);
        NodeVisitor nv = super.child(ns, name);
        if (nv != null) {
            return new DumpAdapter(nv, deep + 1, nses);
        }
        return null;
    }

    protected String getPrefix(String uri) {
        if (nses != null) {
            String prefix = nses.get(uri);
            if (prefix != null) {
                return prefix;
            }
        }
        return uri;
    }

    @Override
    public void ns(String prefix, String uri, int ln) {
        System.out.println(prefix + "=" + uri);
        this.nses.put(uri, prefix);
        super.ns(prefix, uri, ln);
    }

    @Override
    public void text(int ln, String value) {
        for (int i = 0; i < deep + 1; i++) {
            System.out.print("  ");
        }
        System.out.print("T: ");
        System.out.println(value);
        super.text(ln, value);
    }

}
