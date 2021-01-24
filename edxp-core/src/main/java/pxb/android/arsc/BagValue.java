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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class BagValue {
    public List<Map.Entry<Integer, Value>> map = new ArrayList<Entry<Integer, Value>>();
    public final int parent;

    public BagValue(int parent) {
        this.parent = parent;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof BagValue))
            return false;
        BagValue other = (BagValue) obj;
        if (map == null) {
            if (other.map != null)
                return false;
        } else if (!map.equals(other.map))
            return false;
        if (parent != other.parent)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((map == null) ? 0 : map.hashCode());
        result = prime * result + parent;
        return result;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("{bag%08x", parent));
        for (Map.Entry<Integer, Value> e : map) {
            sb.append(",").append(String.format("0x%08x", e.getKey()));
            sb.append("=");
            sb.append(e.getValue());
        }

        return sb.append("}").toString();
    }
}
