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

public class ResSpec {
    public int flags;
    public final int id;
    public String name;

    public ResSpec(int id) {
        super();
        this.id = id;
    }

    public void updateName(String s) {
        String name = this.name;
        if (name == null) {
            this.name = s;
        } else if (!s.equals(name)) {
            throw new RuntimeException();
        }
    }
}