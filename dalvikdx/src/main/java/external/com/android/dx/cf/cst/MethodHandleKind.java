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

package external.com.android.dx.cf.cst;

/**
 * Method Handle kinds for {@code CONSTANT_MethodHandle_info} constants.
 */
public interface MethodHandleKind {
    /** A method handle that gets an instance field. */
    int REF_getField = 1;

    /** A method handle that gets a static field. */
    int REF_getStatic = 2;

    /** A method handle that sets an instance field. */
    int REF_putField = 3;

    /** A method handle that sets a static field. */
    int REF_putStatic = 4;

    /** A method handle for {@code invokevirtual}. */
    int REF_invokeVirtual = 5;

    /** A method handle for {@code invokestatic}. */
    int REF_invokeStatic = 6;

    /** A method handle for {@code invokespecial}. */
    int REF_invokeSpecial = 7;

    /** A method handle for invoking a constructor. */
    int REF_newInvokeSpecial = 8;

    /** A method handle for {@code invokeinterface}. */
    int REF_invokeInterface = 9;
}
