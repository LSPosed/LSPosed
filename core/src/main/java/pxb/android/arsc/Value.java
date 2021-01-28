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

public class Value {
    public final int data;
    public String raw;
    public final int type;

    public Value(int type, int data, String raw) {
        super();
        this.type = type;
        this.data = data;
        this.raw = raw;
    }

    public String toString() {
        if (type == 0x03) {
            return raw;
        }
        return String.format("{t=0x%02x d=0x%08x}", type, data);
    }

}
