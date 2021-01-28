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

import static pxb.android.ResConst.RES_STRING_POOL_TYPE;
import static pxb.android.ResConst.RES_XML_CDATA_TYPE;
import static pxb.android.ResConst.RES_XML_END_ELEMENT_TYPE;
import static pxb.android.ResConst.RES_XML_END_NAMESPACE_TYPE;
import static pxb.android.ResConst.RES_XML_RESOURCE_MAP_TYPE;
import static pxb.android.ResConst.RES_XML_START_ELEMENT_TYPE;
import static pxb.android.ResConst.RES_XML_START_NAMESPACE_TYPE;
import static pxb.android.ResConst.RES_XML_TYPE;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import pxb.android.StringItem;
import pxb.android.StringItems;

/**
 * a class to write android axml
 * 
 * @author <a href="mailto:pxb1988@gmail.com">Panxiaobo</a>
 */
public class AxmlWriter extends AxmlVisitor {
    static final Comparator<Attr> ATTR_CMP = new Comparator<Attr>() {

        @Override
        public int compare(Attr a, Attr b) {
            int x = a.resourceId - b.resourceId;
            if (x == 0) {
                x = a.name.data.compareTo(b.name.data);
                if (x == 0) {
                    boolean aNsIsnull = a.ns == null;
                    boolean bNsIsnull = b.ns == null;
                    if (aNsIsnull) {
                        if (bNsIsnull) {
                            x = 0;
                        } else {
                            x = -1;
                        }
                    } else {
                        if (bNsIsnull) {
                            x = 1;
                        } else {
                            x = a.ns.data.compareTo(b.ns.data);
                        }
                    }

                }
            }
            return x;
        }
    };

    static class Attr {

        public int index;
        public StringItem name;
        public StringItem ns;
        public int resourceId;
        public int type;
        public Object value;
        public StringItem raw;

        public Attr(StringItem ns, StringItem name, int resourceId) {
            super();
            this.ns = ns;
            this.name = name;
            this.resourceId = resourceId;
        }

        public void prepare(AxmlWriter axmlWriter) {
            ns = axmlWriter.updateNs(ns);
            if (this.name != null) {
                if (resourceId != -1) {
                    this.name = axmlWriter.updateWithResourceId(this.name, this.resourceId);
                } else {
                    this.name = axmlWriter.update(this.name);
                }
            }
            if (value instanceof StringItem) {
                value = axmlWriter.update((StringItem) value);
            }
            if (raw != null) {
                raw = axmlWriter.update(raw);
            }
        }

    }

    static class NodeImpl extends NodeVisitor {
        private Set<Attr> attrs = new TreeSet<Attr>(ATTR_CMP);
        private List<NodeImpl> children = new ArrayList<NodeImpl>();
        private int line;
        private StringItem name;
        private StringItem ns;
        private StringItem text;
        private int textLineNumber;
        Attr id;
        Attr style;
        Attr clz;

        public NodeImpl(String ns, String name) {
            super(null);
            this.ns = ns == null ? null : new StringItem(ns);
            this.name = name == null ? null : new StringItem(name);
        }

        @Override
        public void attr(String ns, String name, int resourceId, int type, Object value) {
            if (name == null) {
                throw new RuntimeException("name can't be null");
            }
            Attr a = new Attr(ns == null ? null : new StringItem(ns), new StringItem(name), resourceId);
            a.type = type;

            if (value instanceof ValueWrapper) {
                ValueWrapper valueWrapper = (ValueWrapper) value;
                if (valueWrapper.raw != null) {
                    a.raw = new StringItem(valueWrapper.raw);
                }
                a.value = valueWrapper.ref;
                switch (valueWrapper.type) {
                case ValueWrapper.CLASS:
                    clz = a;
                    break;
                case ValueWrapper.ID:
                    id = a;
                    break;
                case ValueWrapper.STYLE:
                    style = a;
                    break;
                }
            } else if (type == TYPE_STRING) {
                StringItem raw = new StringItem((String) value);
                a.raw = raw;
                a.value = raw;

            } else {
                a.raw = null;
                a.value = value;
            }

            attrs.add(a);
        }

        @Override
        public NodeVisitor child(String ns, String name) {
            NodeImpl child = new NodeImpl(ns, name);
            this.children.add(child);
            return child;
        }

        @Override
        public void end() {
        }

        @Override
        public void line(int ln) {
            this.line = ln;
        }

        public int prepare(AxmlWriter axmlWriter) {
            ns = axmlWriter.updateNs(ns);
            name = axmlWriter.update(name);

            int attrIndex = 0;
            for (Attr attr : attrs) {
                attr.index = attrIndex++;
                attr.prepare(axmlWriter);
            }

            text = axmlWriter.update(text);
            int size = 24 + 36 + attrs.size() * 20;// 24 for end tag,36+x*20 for
            // start tag
            for (NodeImpl child : children) {
                size += child.prepare(axmlWriter);
            }
            if (text != null) {
                size += 28;
            }
            return size;
        }

        @Override
        public void text(int ln, String value) {
            this.text = new StringItem(value);
            this.textLineNumber = ln;
        }

        void write(ByteBuffer out) throws IOException {
            // start tag
            out.putInt(RES_XML_START_ELEMENT_TYPE | (0x0010 << 16));
            out.putInt(36 + attrs.size() * 20);
            out.putInt(line);
            out.putInt(0xFFFFFFFF);
            out.putInt(ns != null ? this.ns.index : -1);
            out.putInt(name.index);
            out.putInt(0x00140014);// TODO
            out.putShort((short) this.attrs.size());
            out.putShort((short) (id == null ? 0 : id.index + 1));
            out.putShort((short) (clz == null ? 0 : clz.index + 1));
            out.putShort((short) (style == null ? 0 : style.index + 1));
            for (Attr attr : attrs) {
                out.putInt(attr.ns == null ? -1 : attr.ns.index);
                out.putInt(attr.name.index);
                out.putInt(attr.raw != null ? attr.raw.index : -1);
                out.putInt((attr.type << 24) | 0x000008);
                Object v = attr.value;
                if (v instanceof StringItem) {
                    out.putInt(((StringItem) attr.value).index);
                } else if (v instanceof Boolean) {
                    out.putInt(Boolean.TRUE.equals(v) ? -1 : 0);
                } else if (v instanceof Float) {
                  out.putInt(Float.floatToIntBits((float) v));
                } else {
                    out.putInt((Integer) attr.value);
                }
            }

            if (this.text != null) {
                out.putInt(RES_XML_CDATA_TYPE | (0x0010 << 16));
                out.putInt(28);
                out.putInt(textLineNumber);
                out.putInt(0xFFFFFFFF);
                out.putInt(text.index);
                out.putInt(0x00000008);
                out.putInt(0x00000000);
            }

            // children
            for (NodeImpl child : children) {
                child.write(out);
            }

            // end tag
            out.putInt(RES_XML_END_ELEMENT_TYPE | (0x0010 << 16));
            out.putInt(24);
            out.putInt(-1);
            out.putInt(0xFFFFFFFF);
            out.putInt(ns != null ? this.ns.index : -1);
            out.putInt(name.index);
        }
    }

    static class Ns {
        int ln;
        StringItem prefix;
        StringItem uri;

        public Ns(StringItem prefix, StringItem uri, int ln) {
            super();
            this.prefix = prefix;
            this.uri = uri;
            this.ln = ln;
        }
    }

    private List<NodeImpl> firsts = new ArrayList<NodeImpl>(3);

    private Map<String, Ns> nses = new HashMap<String, Ns>();

    private List<StringItem> otherString = new ArrayList<StringItem>();

    private Map<String, StringItem> resourceId2Str = new HashMap<String, StringItem>();

    private List<Integer> resourceIds = new ArrayList<Integer>();

    private List<StringItem> resourceString = new ArrayList<StringItem>();

    private StringItems stringItems = new StringItems();

    // TODO add style support
    // private List<StringItem> styleItems = new ArrayList();

    @Override
    public NodeVisitor child(String ns, String name) {
        NodeImpl first = new NodeImpl(ns, name);
        this.firsts.add(first);
        return first;
    }

    @Override
    public void end() {
    }

    @Override
    public void ns(String prefix, String uri, int ln) {
        nses.put(uri, new Ns(prefix == null ? null : new StringItem(prefix), new StringItem(uri), ln));
    }

    private int prepare() throws IOException {
        int size = 0;

        for (NodeImpl first : firsts) {
            size += first.prepare(this);
        }
        {
            int a = 0;
            for (Map.Entry<String, Ns> e : nses.entrySet()) {
                Ns ns = e.getValue();
                if (ns == null) {
                    ns = new Ns(null, new StringItem(e.getKey()), 0);
                    e.setValue(ns);
                }
                if (ns.prefix == null) {
                    ns.prefix = new StringItem(String.format("axml_auto_%02d", a++));
                }
                ns.prefix = update(ns.prefix);
                ns.uri = update(ns.uri);
            }
        }

        size += nses.size() * 24 * 2;

        this.stringItems.addAll(resourceString);
        resourceString = null;
        this.stringItems.addAll(otherString);
        otherString = null;
        this.stringItems.prepare();
        int stringSize = this.stringItems.getSize();
        if (stringSize % 4 != 0) {
            stringSize += 4 - stringSize % 4;
        }
        size += 8 + stringSize;
        size += 8 + resourceIds.size() * 4;
        return size;
    }

    public byte[] toByteArray() throws IOException {

        int size = 8 + prepare();
        ByteBuffer out = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);

        out.putInt(RES_XML_TYPE | (0x0008 << 16));
        out.putInt(size);

        int stringSize = this.stringItems.getSize();
        int padding = 0;
        if (stringSize % 4 != 0) {
            padding = 4 - stringSize % 4;
        }
        out.putInt(RES_STRING_POOL_TYPE | (0x001C << 16));
        out.putInt(stringSize + padding + 8);
        this.stringItems.write(out);
        out.put(new byte[padding]);

        out.putInt(RES_XML_RESOURCE_MAP_TYPE | (0x0008 << 16));
        out.putInt(8 + this.resourceIds.size() * 4);
        for (Integer i : resourceIds) {
            out.putInt(i);
        }

        Stack<Ns> stack = new Stack<Ns>();
        for (Map.Entry<String, Ns> e : this.nses.entrySet()) {
            Ns ns = e.getValue();
            stack.push(ns);
            out.putInt(RES_XML_START_NAMESPACE_TYPE | (0x0010 << 16));
            out.putInt(24);
            out.putInt(-1);
            out.putInt(0xFFFFFFFF);
            out.putInt(ns.prefix.index);
            out.putInt(ns.uri.index);
        }

        for (NodeImpl first : firsts) {
            first.write(out);
        }

        while (stack.size() > 0) {
            Ns ns = stack.pop();
            out.putInt(RES_XML_END_NAMESPACE_TYPE | (0x0010 << 16));
            out.putInt(24);
            out.putInt(ns.ln);
            out.putInt(0xFFFFFFFF);
            out.putInt(ns.prefix.index);
            out.putInt(ns.uri.index);
        }
        return out.array();
    }

    StringItem update(StringItem item) {
        if (item == null)
            return null;
        int i = this.otherString.indexOf(item);
        if (i < 0) {
            StringItem copy = new StringItem(item.data);
            this.otherString.add(copy);
            return copy;
        } else {
            return this.otherString.get(i);
        }
    }

    StringItem updateNs(StringItem item) {
        if (item == null) {
            return null;
        }
        String ns = item.data;
        if (!this.nses.containsKey(ns)) {
            this.nses.put(ns, null);
        }
        return update(item);
    }

    StringItem updateWithResourceId(StringItem name, int resourceId) {
        String key = name.data + resourceId;
        StringItem item = this.resourceId2Str.get(key);
        if (item != null) {
            return item;
        } else {
            StringItem copy = new StringItem(name.data);
            resourceIds.add(resourceId);
            resourceString.add(copy);
            resourceId2Str.put(key, copy);
            return copy;
        }
    }
}
