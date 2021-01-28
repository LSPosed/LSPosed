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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import pxb.android.ResConst;
import pxb.android.StringItems;

/**
 * 
 * Read the resources.arsc inside an Android apk.
 * 
 * Usage:
 * 
 * <pre>
 * byte[] oldArscFile= ... ; //
 * List&lt;Pkg&gt; pkgs = new ArscParser(oldArscFile).parse(); // read the file
 * modify(pkgs); // do what you want here
 * byte[] newArscFile = new ArscWriter(pkgs).toByteArray(); // build a new file
 * </pre>
 * 
 * The format of arsc is described here (gingerbread)
 * <ul>
 * <li>frameworks/base/libs/utils/ResourceTypes.cpp</li>
 * <li>frameworks/base/include/utils/ResourceTypes.h</li>
 * </ul>
 * and the cmd line <code>aapt d resources abc.apk</code> is also good for debug
 * (available in android sdk)
 * 
 * <p>
 * Todos:
 * <ul>
 * TODO add support to read styled strings
 * </ul>
 * 
 * <p>
 * Thanks to the the following projects
 * <ul>
 * <li>android4me https://code.google.com/p/android4me/</li>
 * <li>Apktool https://code.google.com/p/android-apktool</li>
 * <li>Android http://source.android.com/</li>
 * </ul>
 * 
 * @author bob
 * 
 */
public class ArscParser implements ResConst {

    /* pkg */class Chunk {

        public final int headSize;
        public final int location;
        public final int size;
        public final int type;

        public Chunk() {
            location = in.position();
            type = in.getShort() & 0xFFFF;
            headSize = in.getShort() & 0xFFFF;
            size = in.getInt();
        }
    }

    /**
     * If set, this resource has been declared public, so libraries are allowed
     * to reference it.
     */
    static final int ENGRY_FLAG_PUBLIC = 0x0002;

    /**
     * If set, this is a complex entry, holding a set of name/value mappings. It
     * is followed by an array of ResTable_map structures.
     */
    final static short ENTRY_FLAG_COMPLEX = 0x0001;
    public static final int TYPE_STRING = 0x03;

    private int fileSize = -1;
    private ByteBuffer in;
    private String[] keyNamesX;
    private Pkg pkg;
    private List<Pkg> pkgs = new ArrayList<Pkg>();
    private String[] strings;
    private String[] typeNamesX;

    public ArscParser(byte[] b) {
        this.in = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);
    }

    public List<Pkg> parse() throws IOException {
        if (fileSize < 0) {
            Chunk head = new Chunk();
            if (head.type != RES_TABLE_TYPE) {
                throw new RuntimeException();
            }
            fileSize = head.size;
            in.getInt();// packagecount
        }
        while (in.hasRemaining()) {
            Chunk chunk = new Chunk();
            switch (chunk.type) {
            case RES_STRING_POOL_TYPE:
                strings = StringItems.read(in);
                break;
            case RES_TABLE_PACKAGE_TYPE:
                readPackage(in);
            }
            in.position(chunk.location + chunk.size);
        }
        return pkgs;
    }

    // private void readConfigFlags() {
    // int size = in.getInt();
    // if (size < 28) {
    // throw new RuntimeException();
    // }
    // short mcc = in.getShort();
    // short mnc = in.getShort();
    //
    // char[] language = new char[] { (char) in.get(), (char) in.get() };
    // char[] country = new char[] { (char) in.get(), (char) in.get() };
    //
    // byte orientation = in.get();
    // byte touchscreen = in.get();
    // short density = in.getShort();
    //
    // byte keyboard = in.get();
    // byte navigation = in.get();
    // byte inputFlags = in.get();
    // byte inputPad0 = in.get();
    //
    // short screenWidth = in.getShort();
    // short screenHeight = in.getShort();
    //
    // short sdkVersion = in.getShort();
    // short minorVersion = in.getShort();
    //
    // byte screenLayout = 0;
    // byte uiMode = 0;
    // short smallestScreenWidthDp = 0;
    // if (size >= 32) {
    // screenLayout = in.get();
    // uiMode = in.get();
    // smallestScreenWidthDp = in.getShort();
    // }
    //
    // short screenWidthDp = 0;
    // short screenHeightDp = 0;
    //
    // if (size >= 36) {
    // screenWidthDp = in.getShort();
    // screenHeightDp = in.getShort();
    // }
    //
    // short layoutDirection = 0;
    // if (size >= 38 && sdkVersion >= 17) {
    // layoutDirection = in.getShort();
    // }
    //
    // }

    private void readEntry(Config config, ResSpec spec) {
        int size = in.getShort();

        int flags = in.getShort(); // ENTRY_FLAG_PUBLIC
        int keyStr = in.getInt();
        spec.updateName(keyNamesX[keyStr]);

        ResEntry resEntry = new ResEntry(flags, spec);

        if (0 != (flags & ENTRY_FLAG_COMPLEX)) {

            int parent = in.getInt();
            int count = in.getInt();
            BagValue bag = new BagValue(parent);
            for (int i = 0; i < count; i++) {
                Map.Entry<Integer, Value> entry = new AbstractMap.SimpleEntry(in.getInt(), readValue());
                bag.map.add(entry);
            }
            resEntry.value = bag;
        } else {
            resEntry.value = readValue();
        }
        config.resources.put(spec.id, resEntry);
    }

    private void readPackage(ByteBuffer in) throws IOException {
        int pid = in.getInt() % 0xFF;

        String name;
        {
            int nextPisition = in.position() + 128 * 2;
            StringBuilder sb = new StringBuilder(32);
            for (int i = 0; i < 128; i++) {
                int s = in.getShort();
                if (s == 0) {
                    break;
                } else {
                    sb.append((char) s);
                }
            }
            name = sb.toString();
            in.position(nextPisition);
        }

        pkg = new Pkg(pid, name);
        pkgs.add(pkg);

        int typeStringOff = in.getInt();
        int typeNameCount = in.getInt();
        int keyStringOff = in.getInt();
        int specNameCount = in.getInt();

        {
            Chunk chunk = new Chunk();
            if (chunk.type != RES_STRING_POOL_TYPE) {
                throw new RuntimeException();
            }
            typeNamesX = StringItems.read(in);
            in.position(chunk.location + chunk.size);
        }
        {
            Chunk chunk = new Chunk();
            if (chunk.type != RES_STRING_POOL_TYPE) {
                throw new RuntimeException();
            }
            keyNamesX = StringItems.read(in);
            in.position(chunk.location + chunk.size);
        }

        out: while (in.hasRemaining()) {
            Chunk chunk = new Chunk();
            switch (chunk.type) {
            case RES_TABLE_TYPE_SPEC_TYPE: {
                int tid = in.get() & 0xFF;
                in.get(); // res0
                in.getShort();// res1
                int entryCount = in.getInt();

                Type t = pkg.getType(tid, typeNamesX[tid - 1], entryCount);
                for (int i = 0; i < entryCount; i++) {
                    t.getSpec(i).flags = in.getInt();
                }
            }
                break;
            case RES_TABLE_TYPE_TYPE: {
                int tid = in.get() & 0xFF;
                in.get(); // res0
                in.getShort();// res1
                int entryCount = in.getInt();
                Type t = pkg.getType(tid, typeNamesX[tid - 1], entryCount);
                int entriesStart = in.getInt();

                int p = in.position();
                int size = in.getInt();
                // readConfigFlags();
                byte[] data = new byte[size];
                in.position(p);
                in.get(data);
                Config config = new Config(data, entryCount);

                in.position(chunk.location + chunk.headSize);

                int[] entrys = new int[entryCount];
                for (int i = 0; i < entryCount; i++) {
                    entrys[i] = in.getInt();
                }
                for (int i = 0; i < entrys.length; i++) {
                    if (entrys[i] != -1) {
                        in.position(chunk.location + entriesStart + entrys[i]);
                        ResSpec spec = t.getSpec(i);
                        readEntry(config, spec);
                    }
                }

                t.addConfig(config);
            }
                break;
            default:
                break out;
            }
            in.position(chunk.location + chunk.size);
        }
    }

    private Object readValue() {
        int size1 = in.getShort();// 8
        int zero = in.get();// 0
        int type = in.get() & 0xFF; // TypedValue.*
        int data = in.getInt();
        String raw = null;
        if (type == TYPE_STRING) {
            raw = strings[data];
        }
        return new Value(type, data, raw);
    }
}
