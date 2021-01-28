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

public class ResEntry {
    public final int flag;

    public final ResSpec spec;
    /**
     * {@link BagValue} or {@link Value}
     */
    public Object value;

    /* package */int wOffset;

    public ResEntry(int flag, ResSpec spec) {
        super();
        this.flag = flag;
        this.spec = spec;
    }

}