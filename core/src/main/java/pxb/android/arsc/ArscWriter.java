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
package pxb.android.arsc;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import pxb.android.ResConst;
import pxb.android.StringItem;
import pxb.android.StringItems;
import pxb.android.axml.Util;

/**
 * Write pkgs to an arsc file
 * 
 * @see ArscParser
 * @author bob
 * 
 */
public class ArscWriter implements ResConst {
    private static class PkgCtx {
        Map<String, StringItem> keyNames = new HashMap<String, StringItem>();
        StringItems keyNames0 = new StringItems();
        public int keyStringOff;
        int offset;
        Pkg pkg;
        int pkgSize;
        List<StringItem> typeNames = new ArrayList<StringItem>();

        StringItems typeNames0 = new StringItems();
        int typeStringOff;

        public void addKeyName(String name) {
            if (keyNames.containsKey(name)) {
                return;
            }
            StringItem stringItem = new StringItem(name);
            keyNames.put(name, stringItem);
            keyNames0.add(stringItem);
        }

        public void addTypeName(int id, String name) {
            while (typeNames.size() <= id) {
                typeNames.add(null);
            }

            StringItem item = typeNames.get(id);
            if (item == null) {
                typeNames.set(id, new StringItem(name));
            } else {
                throw new RuntimeException();
            }
        }
    }

    private static void D(String fmt, Object... args) {

    }

    private List<PkgCtx> ctxs = new ArrayList<PkgCtx>(5);
    private List<Pkg> pkgs;
    private Map<String, StringItem> strTable = new TreeMap<String, StringItem>();
    private StringItems strTable0 = new StringItems();

    public ArscWriter(List<Pkg> pkgs) {
        this.pkgs = pkgs;
    }

    public static void main(String... args) throws IOException {
        if (args.length < 2) {
            System.err.println("asrc-write-test in.arsc out.arsc");
            return;
        }
        byte[] data = Util.readFile(new File(args[0]));
        List<Pkg> pkgs = new ArscParser(data).parse();
        // ArscDumper.dump(pkgs);
        byte[] data2 = new ArscWriter(pkgs).toByteArray();
        // ArscDumper.dump(new ArscParser(data2).parse());
        Util.writeFile(data2, new File(args[1]));
    }

    private void addString(String str) {
        if (strTable.containsKey(str)) {
            return;
        }
        StringItem stringItem = new StringItem(str);
        strTable.put(str, stringItem);
        strTable0.add(stringItem);
    }

    private int count() {

        int size = 0;

        size += 8 + 4;// chunk, pkgcount
        {
            int stringSize = strTable0.getSize();
            if (stringSize % 4 != 0) {
                stringSize += 4 - stringSize % 4;
            }
            size += 8 + stringSize;// global strings
        }
        for (PkgCtx ctx : ctxs) {
            ctx.offset = size;
            int pkgSize = 0;
            pkgSize += 8 + 4 + 256;// chunk,pid+name
            pkgSize += 4 * 4;

            ctx.typeStringOff = pkgSize;
            {
                int stringSize = ctx.typeNames0.getSize();
                if (stringSize % 4 != 0) {
                    stringSize += 4 - stringSize % 4;
                }
                pkgSize += 8 + stringSize;// type names
            }

            ctx.keyStringOff = pkgSize;

            {
                int stringSize = ctx.keyNames0.getSize();
                if (stringSize % 4 != 0) {
                    stringSize += 4 - stringSize % 4;
                }
                pkgSize += 8 + stringSize;// key names
            }

            for (Type type : ctx.pkg.types.values()) {
                type.wPosition = size + pkgSize;
                pkgSize += 8 + 4 + 4 + 4 * type.specs.length; // trunk,id,entryCount,
                                                              // configs

                for (Config config : type.configs) {
                    config.wPosition = pkgSize + size;
                    int configBasePostion = pkgSize;
                    pkgSize += 8 + 4 + 4 + 4; // trunk,id,entryCount,entriesStart
                    int size0 = config.id.length;
                    if (size0 % 4 != 0) {
                        size0 += 4 - size0 % 4;
                    }
                    pkgSize += size0;// config

                    if (pkgSize - configBasePostion > 0x0038) {
                        throw new RuntimeException("config id  too big");
                    } else {
                        pkgSize = configBasePostion + 0x0038;
                    }

                    pkgSize += 4 * config.entryCount;// offset
                    config.wEntryStart = pkgSize - configBasePostion;
                    int entryBase = pkgSize;
                    for (ResEntry e : config.resources.values()) {
                        e.wOffset = pkgSize - entryBase;
                        pkgSize += 8;// size,flag,keyString
                        if (e.value instanceof BagValue) {
                            BagValue big = (BagValue) e.value;
                            pkgSize += 8 + big.map.size() * 12;
                        } else {
                            pkgSize += 8;
                        }
                    }
                    config.wChunkSize = pkgSize - configBasePostion;
                }
            }
            ctx.pkgSize = pkgSize;
            size += pkgSize;
        }

        return size;
    }

    private List<PkgCtx> prepare() throws IOException {
        for (Pkg pkg : pkgs) {
            PkgCtx ctx = new PkgCtx();
            ctx.pkg = pkg;
            ctxs.add(ctx);

            for (Type type : pkg.types.values()) {
                ctx.addTypeName(type.id - 1, type.name);
                for (ResSpec spec : type.specs) {
                    ctx.addKeyName(spec.name);
                }
                for (Config config : type.configs) {
                    for (ResEntry e : config.resources.values()) {
                        Object object = e.value;
                        if (object instanceof BagValue) {
                            travelBagValue((BagValue) object);
                        } else {
                            travelValue((Value) object);
                        }
                    }
                }
            }
            ctx.keyNames0.prepare();
            ctx.typeNames0.addAll(ctx.typeNames);
            ctx.typeNames0.prepare();
        }
        strTable0.prepare();
        return ctxs;
    }

    public byte[] toByteArray() throws IOException {
        prepare();
        int size = count();
        ByteBuffer out = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
        write(out, size);
        return out.array();
    }

    private void travelBagValue(BagValue bag) {
        for (Map.Entry<Integer, Value> e : bag.map) {
            travelValue(e.getValue());
        }
    }

    private void travelValue(Value v) {
        if (v.raw != null) {
            addString(v.raw);
        }
    }

    private void write(ByteBuffer out, int size) throws IOException {
        out.putInt(RES_TABLE_TYPE | (0x000c << 16));
        out.putInt(size);
        out.putInt(ctxs.size());

        {
            int stringSize = strTable0.getSize();
            int padding = 0;
            if (stringSize % 4 != 0) {
                padding = 4 - stringSize % 4;
            }
            out.putInt(RES_STRING_POOL_TYPE | (0x001C << 16));
            out.putInt(stringSize + padding + 8);
            strTable0.write(out);
            out.put(new byte[padding]);
        }

        for (PkgCtx pctx : ctxs) {
            if (out.position() != pctx.offset) {
                throw new RuntimeException();
            }
            final int basePosition = out.position();
            out.putInt(RES_TABLE_PACKAGE_TYPE | (0x011c << 16));
            out.putInt(pctx.pkgSize);
            out.putInt(pctx.pkg.id);
            int p = out.position();
            out.put(pctx.pkg.name.getBytes("UTF-16LE"));
            out.position(p + 256);

            out.putInt(pctx.typeStringOff);
            out.putInt(pctx.typeNames0.size());

            out.putInt(pctx.keyStringOff);
            out.putInt(pctx.keyNames0.size());

            {
                if (out.position() - basePosition != pctx.typeStringOff) {
                    throw new RuntimeException();
                }
                int stringSize = pctx.typeNames0.getSize();
                int padding = 0;
                if (stringSize % 4 != 0) {
                    padding = 4 - stringSize % 4;
                }
                out.putInt(RES_STRING_POOL_TYPE | (0x001C << 16));
                out.putInt(stringSize + padding + 8);
                pctx.typeNames0.write(out);
                out.put(new byte[padding]);
            }

            {
                if (out.position() - basePosition != pctx.keyStringOff) {
                    throw new RuntimeException();
                }
                int stringSize = pctx.keyNames0.getSize();
                int padding = 0;
                if (stringSize % 4 != 0) {
                    padding = 4 - stringSize % 4;
                }
                out.putInt(RES_STRING_POOL_TYPE | (0x001C << 16));
                out.putInt(stringSize + padding + 8);
                pctx.keyNames0.write(out);
                out.put(new byte[padding]);
            }

            for (Type t : pctx.pkg.types.values()) {
                D("[%08x]write spec", out.position(), t.name);
                if (t.wPosition != out.position()) {
                    throw new RuntimeException();
                }
                out.putInt(RES_TABLE_TYPE_SPEC_TYPE | (0x0010 << 16));
                out.putInt(4 * 4 + 4 * t.specs.length);// size

                out.putInt(t.id);
                out.putInt(t.specs.length);
                for (ResSpec spec : t.specs) {
                    out.putInt(spec.flags);
                }

                for (Config config : t.configs) {
                    D("[%08x]write config", out.position());
                    int typeConfigPosition = out.position();
                    if (config.wPosition != typeConfigPosition) {
                        throw new RuntimeException();
                    }
                    out.putInt(RES_TABLE_TYPE_TYPE | (0x0038 << 16));
                    out.putInt(config.wChunkSize);// size

                    out.putInt(t.id);
                    out.putInt(t.specs.length);
                    out.putInt(config.wEntryStart);

                    D("[%08x]write config ids", out.position());
                    out.put(config.id);

                    int size0 = config.id.length;
                    int padding = 0;
                    if (size0 % 4 != 0) {
                        padding = 4 - size0 % 4;
                    }
                    out.put(new byte[padding]);

                    out.position(typeConfigPosition + 0x0038);

                    D("[%08x]write config entry offsets", out.position());
                    for (int i = 0; i < config.entryCount; i++) {
                        ResEntry entry = config.resources.get(i);
                        if (entry == null) {
                            out.putInt(-1);
                        } else {
                            out.putInt(entry.wOffset);
                        }
                    }

                    if (out.position() - typeConfigPosition != config.wEntryStart) {
                        throw new RuntimeException();
                    }
                    D("[%08x]write config entrys", out.position());
                    for (ResEntry e : config.resources.values()) {
                        D("[%08x]ResTable_entry", out.position());
                        boolean isBag = e.value instanceof BagValue;
                        out.putShort((short) (isBag ? 16 : 8));
                        int flag = e.flag;
                        if (isBag) { // add complex flag
                            flag |= ArscParser.ENTRY_FLAG_COMPLEX;
                        } else { // remove
                            flag &= ~ArscParser.ENTRY_FLAG_COMPLEX;
                        }
                        out.putShort((short) flag);
                        out.putInt(pctx.keyNames.get(e.spec.name).index);
                        if (isBag) {
                            BagValue bag = (BagValue) e.value;
                            out.putInt(bag.parent);
                            out.putInt(bag.map.size());
                            for (Map.Entry<Integer, Value> entry : bag.map) {
                                out.putInt(entry.getKey());
                                writeValue(entry.getValue(), out);
                            }
                        } else {
                            writeValue((Value) e.value, out);
                        }
                    }
                }
            }
        }
    }

    private void writeValue(Value value, ByteBuffer out) {
        out.putShort((short) 8);
        out.put((byte) 0);
        out.put((byte) value.type);
        if (value.type == ArscParser.TYPE_STRING) {
            out.putInt(strTable.get(value.raw).index);
        } else {
            out.putInt(value.data);
        }
    }

}
