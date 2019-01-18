/*
 * Copyright (C) 2017 The Android Open Source Project
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
package external.com.android.dx.cf.attrib;

import external.com.android.dx.cf.code.BootstrapMethodsList;

/**
 * Attribute class for standard {@code AttBootstrapMethods} attributes.
 */
public class AttBootstrapMethods extends BaseAttribute {
    /** {@code non-null;} attribute name for attributes of this type */
    public static final String ATTRIBUTE_NAME = "BootstrapMethods";

    private static final int ATTRIBUTE_HEADER_BYTES = 8;
    private static final int BOOTSTRAP_METHOD_BYTES = 4;
    private static final int BOOTSTRAP_ARGUMENT_BYTES = 2;

    private final BootstrapMethodsList bootstrapMethods;

    private final int byteLength;

    public AttBootstrapMethods(BootstrapMethodsList bootstrapMethods) {
        super(ATTRIBUTE_NAME);
        this.bootstrapMethods = bootstrapMethods;

        int bytes = ATTRIBUTE_HEADER_BYTES + bootstrapMethods.size() * BOOTSTRAP_METHOD_BYTES;
        for (int i = 0; i < bootstrapMethods.size(); ++i) {
            int numberOfArguments = bootstrapMethods.get(i).getBootstrapMethodArguments().size();
            bytes += numberOfArguments * BOOTSTRAP_ARGUMENT_BYTES;
        }
        this.byteLength = bytes;
    }

    @Override
    public int byteLength() {
        return byteLength;
    }

    /**
     * Get the bootstrap methods present in attribute.
     * @return bootstrap methods list
     */
    public BootstrapMethodsList getBootstrapMethods() {
        return bootstrapMethods;
    }
}
