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

public class Type {
    public List<Config> configs = new ArrayList<Config>();
    public int id;
    public String name;
    public ResSpec[] specs;
    /* package */int wPosition;

    public void addConfig(Config config) {
        if (config.entryCount != specs.length) {
            throw new RuntimeException();
        }
        configs.add(config);
    }

    public ResSpec getSpec(int resId) {
        ResSpec res = specs[resId];
        if (res == null) {
            res = new ResSpec(resId);
            specs[resId] = res;
        }
        return res;
    }

}