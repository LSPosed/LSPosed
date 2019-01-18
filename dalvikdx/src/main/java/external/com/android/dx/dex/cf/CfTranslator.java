/*
 * Copyright (C) 2007 The Android Open Source Project
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

package external.com.android.dx.dex.cf;

import external.com.android.dex.util.ExceptionWithContext;
import external.com.android.dx.cf.code.BootstrapMethodsList;
import external.com.android.dx.cf.code.ConcreteMethod;
import external.com.android.dx.cf.code.Ropper;
import external.com.android.dx.cf.direct.DirectClassFile;
import external.com.android.dx.cf.iface.Field;
import external.com.android.dx.cf.iface.FieldList;
import external.com.android.dx.cf.iface.Method;
import external.com.android.dx.cf.iface.MethodList;
import external.com.android.dx.command.dexer.DxContext;
import external.com.android.dx.dex.DexOptions;
import external.com.android.dx.dex.code.DalvCode;
import external.com.android.dx.dex.code.PositionList;
import external.com.android.dx.dex.code.RopTranslator;
import external.com.android.dx.dex.file.CallSiteIdsSection;
import external.com.android.dx.dex.file.ClassDefItem;
import external.com.android.dx.dex.file.DexFile;
import external.com.android.dx.dex.file.EncodedField;
import external.com.android.dx.dex.file.EncodedMethod;
import external.com.android.dx.dex.file.FieldIdsSection;
import external.com.android.dx.dex.file.MethodHandlesSection;
import external.com.android.dx.dex.file.MethodIdsSection;
import external.com.android.dx.rop.annotation.Annotations;
import external.com.android.dx.rop.annotation.AnnotationsList;
import external.com.android.dx.rop.code.AccessFlags;
import external.com.android.dx.rop.code.DexTranslationAdvice;
import external.com.android.dx.rop.code.LocalVariableExtractor;
import external.com.android.dx.rop.code.LocalVariableInfo;
import external.com.android.dx.rop.code.RopMethod;
import external.com.android.dx.rop.code.TranslationAdvice;
import external.com.android.dx.rop.cst.Constant;
import external.com.android.dx.rop.cst.ConstantPool;
import external.com.android.dx.rop.cst.CstBaseMethodRef;
import external.com.android.dx.rop.cst.CstBoolean;
import external.com.android.dx.rop.cst.CstByte;
import external.com.android.dx.rop.cst.CstCallSite;
import external.com.android.dx.rop.cst.CstCallSiteRef;
import external.com.android.dx.rop.cst.CstChar;
import external.com.android.dx.rop.cst.CstEnumRef;
import external.com.android.dx.rop.cst.CstFieldRef;
import external.com.android.dx.rop.cst.CstInteger;
import external.com.android.dx.rop.cst.CstInterfaceMethodRef;
import external.com.android.dx.rop.cst.CstInvokeDynamic;
import external.com.android.dx.rop.cst.CstMethodHandle;
import external.com.android.dx.rop.cst.CstMethodRef;
import external.com.android.dx.rop.cst.CstShort;
import external.com.android.dx.rop.cst.CstString;
import external.com.android.dx.rop.cst.CstType;
import external.com.android.dx.rop.cst.TypedConstant;
import external.com.android.dx.rop.type.Type;
import external.com.android.dx.rop.type.TypeList;
import external.com.android.dx.ssa.Optimizer;

/**
 * Static method that turns {@code byte[]}s containing Java
 * classfiles into {@link ClassDefItem} instances.
 */
public class CfTranslator {
    /** set to {@code true} to enable development-time debugging code */
    private static final boolean DEBUG = false;

    /**
     * This class is uninstantiable.
     */
    private CfTranslator() {
        // This space intentionally left blank.
    }

    /**
     * Takes a {@code byte[]}, interprets it as a Java classfile, and
     * translates it into a {@link ClassDefItem}.
     *
     * @param context {@code non-null;} the state global to this invocation.
     * @param cf {@code non-null;} the class file
     * @param bytes {@code non-null;} contents of the file
     * @param cfOptions options for class translation
     * @param dexOptions options for dex output
     * @param dexFile {@code non-null;} dex output
     * @return {@code non-null;} the translated class
     */
    public static ClassDefItem translate(DxContext context, DirectClassFile cf, byte[] bytes,
            CfOptions cfOptions, DexOptions dexOptions, DexFile dexFile) {
        try {
            return translate0(context, cf, bytes, cfOptions, dexOptions, dexFile);
        } catch (RuntimeException ex) {
            String msg = "...while processing " + cf.getFilePath();
            throw ExceptionWithContext.withContext(ex, msg);
        }
    }

    /**
     * Performs the main act of translation. This method is separated
     * from {@link #translate} just to keep things a bit simpler in
     * terms of exception handling.
     *
     *
     * @param context {@code non-null;} the state global to this invocation.
     * @param cf {@code non-null;} the class file
     * @param bytes {@code non-null;} contents of the file
     * @param cfOptions options for class translation
     * @param dexOptions options for dex output
     * @param dexFile {@code non-null;} dex output
     * @return {@code non-null;} the translated class
     */
    private static ClassDefItem translate0(DxContext context, DirectClassFile cf, byte[] bytes,
            CfOptions cfOptions, DexOptions dexOptions, DexFile dexFile) {

        context.optimizerOptions.loadOptimizeLists(cfOptions.optimizeListFile,
                cfOptions.dontOptimizeListFile);

        // Build up a class to output.

        CstType thisClass = cf.getThisClass();
        int classAccessFlags = cf.getAccessFlags() & ~AccessFlags.ACC_SUPER;
        CstString sourceFile = (cfOptions.positionInfo == PositionList.NONE) ? null :
            cf.getSourceFile();
        ClassDefItem out =
            new ClassDefItem(thisClass, classAccessFlags,
                    cf.getSuperclass(), cf.getInterfaces(), sourceFile);

        Annotations classAnnotations =
            AttributeTranslator.getClassAnnotations(cf, cfOptions);
        if (classAnnotations.size() != 0) {
            out.setClassAnnotations(classAnnotations, dexFile);
        }

        FieldIdsSection fieldIdsSection = dexFile.getFieldIds();
        MethodIdsSection methodIdsSection = dexFile.getMethodIds();
        MethodHandlesSection methodHandlesSection = dexFile.getMethodHandles();
        CallSiteIdsSection callSiteIds = dexFile.getCallSiteIds();
        processFields(cf, out, dexFile);
        processMethods(context, cf, cfOptions, dexOptions, out, dexFile);

        // intern constant pool method, field and type references
        ConstantPool constantPool = cf.getConstantPool();
        int constantPoolSize = constantPool.size();

        for (int i = 0; i < constantPoolSize; i++) {
            Constant constant = constantPool.getOrNull(i);
            if (constant instanceof CstMethodRef) {
                methodIdsSection.intern((CstBaseMethodRef) constant);
            } else if (constant instanceof CstInterfaceMethodRef) {
                methodIdsSection.intern(((CstInterfaceMethodRef) constant).toMethodRef());
            } else if (constant instanceof CstFieldRef) {
                fieldIdsSection.intern((CstFieldRef) constant);
            } else if (constant instanceof CstEnumRef) {
                fieldIdsSection.intern(((CstEnumRef) constant).getFieldRef());
            } else if (constant instanceof CstMethodHandle) {
                methodHandlesSection.intern((CstMethodHandle) constant);
            } else if (constant instanceof CstInvokeDynamic) {
                CstInvokeDynamic cstInvokeDynamic = (CstInvokeDynamic) constant;
                int index = cstInvokeDynamic.getBootstrapMethodIndex();
                BootstrapMethodsList.Item bootstrapMethod = cf.getBootstrapMethods().get(index);
                CstCallSite callSite =
                        CstCallSite.make(bootstrapMethod.getBootstrapMethodHandle(),
                                         cstInvokeDynamic.getNat(),
                                         bootstrapMethod.getBootstrapMethodArguments());
                cstInvokeDynamic.setDeclaringClass(cf.getThisClass());
                cstInvokeDynamic.setCallSite(callSite);
                for (CstCallSiteRef ref : cstInvokeDynamic.getReferences()) {
                    callSiteIds.intern(ref);
                }
            }
        }

        return out;
    }

    /**
     * Processes the fields of the given class.
     *
     * @param cf {@code non-null;} class being translated
     * @param out {@code non-null;} output class
     * @param dexFile {@code non-null;} dex output
     */
    private static void processFields(
            DirectClassFile cf, ClassDefItem out, DexFile dexFile) {
        CstType thisClass = cf.getThisClass();
        FieldList fields = cf.getFields();
        int sz = fields.size();

        for (int i = 0; i < sz; i++) {
            Field one = fields.get(i);
            try {
                CstFieldRef field = new CstFieldRef(thisClass, one.getNat());
                int accessFlags = one.getAccessFlags();

                if (AccessFlags.isStatic(accessFlags)) {
                    TypedConstant constVal = one.getConstantValue();
                    EncodedField fi = new EncodedField(field, accessFlags);
                    if (constVal != null) {
                        constVal = coerceConstant(constVal, field.getType());
                    }
                    out.addStaticField(fi, constVal);
                } else {
                    EncodedField fi = new EncodedField(field, accessFlags);
                    out.addInstanceField(fi);
                }

                Annotations annotations =
                    AttributeTranslator.getAnnotations(one.getAttributes());
                if (annotations.size() != 0) {
                    out.addFieldAnnotations(field, annotations, dexFile);
                }
                dexFile.getFieldIds().intern(field);
            } catch (RuntimeException ex) {
                String msg = "...while processing " + one.getName().toHuman() +
                    " " + one.getDescriptor().toHuman();
                throw ExceptionWithContext.withContext(ex, msg);
            }
        }
    }

    /**
     * Helper for {@link #processFields}, which translates constants into
     * more specific types if necessary.
     *
     * @param constant {@code non-null;} the constant in question
     * @param type {@code non-null;} the desired type
     */
    private static TypedConstant coerceConstant(TypedConstant constant,
            Type type) {
        Type constantType = constant.getType();

        if (constantType.equals(type)) {
            return constant;
        }

        switch (type.getBasicType()) {
            case Type.BT_BOOLEAN: {
                return CstBoolean.make(((CstInteger) constant).getValue());
            }
            case Type.BT_BYTE: {
                return CstByte.make(((CstInteger) constant).getValue());
            }
            case Type.BT_CHAR: {
                return CstChar.make(((CstInteger) constant).getValue());
            }
            case Type.BT_SHORT: {
                return CstShort.make(((CstInteger) constant).getValue());
            }
            default: {
                throw new UnsupportedOperationException("can't coerce " +
                        constant + " to " + type);
            }
        }
    }

    /**
     * Processes the methods of the given class.
     *
     * @param context {@code non-null;} the state global to this invocation.
     * @param cf {@code non-null;} class being translated
     * @param cfOptions {@code non-null;} options for class translation
     * @param dexOptions {@code non-null;} options for dex output
     * @param out {@code non-null;} output class
     * @param dexFile {@code non-null;} dex output
     */
    private static void processMethods(DxContext context, DirectClassFile cf, CfOptions cfOptions,
                                       DexOptions dexOptions, ClassDefItem out, DexFile dexFile) {
        CstType thisClass = cf.getThisClass();
        MethodList methods = cf.getMethods();
        int sz = methods.size();

        for (int i = 0; i < sz; i++) {
            Method one = methods.get(i);
            try {
                CstMethodRef meth = new CstMethodRef(thisClass, one.getNat());
                int accessFlags = one.getAccessFlags();
                boolean isStatic = AccessFlags.isStatic(accessFlags);
                boolean isPrivate = AccessFlags.isPrivate(accessFlags);
                boolean isNative = AccessFlags.isNative(accessFlags);
                boolean isAbstract = AccessFlags.isAbstract(accessFlags);
                boolean isConstructor = meth.isInstanceInit() ||
                    meth.isClassInit();
                DalvCode code;

                if (isNative || isAbstract) {
                    // There's no code for native or abstract methods.
                    code = null;
                } else {
                    ConcreteMethod concrete =
                        new ConcreteMethod(one, cf,
                                (cfOptions.positionInfo != PositionList.NONE),
                                cfOptions.localInfo);

                    TranslationAdvice advice;

                    advice = DexTranslationAdvice.THE_ONE;

                    RopMethod rmeth = Ropper.convert(concrete, advice, methods, dexOptions);
                    RopMethod nonOptRmeth = null;
                    int paramSize;

                    paramSize = meth.getParameterWordCount(isStatic);

                    String canonicalName
                            = thisClass.getClassType().getDescriptor()
                                + "." + one.getName().getString();

                    if (cfOptions.optimize &&
                            context.optimizerOptions.shouldOptimize(canonicalName)) {
                        if (DEBUG) {
                            System.err.println("Optimizing " + canonicalName);
                        }

                        nonOptRmeth = rmeth;
                        rmeth = Optimizer.optimize(rmeth,
                                paramSize, isStatic, cfOptions.localInfo, advice);

                        if (DEBUG) {
                            context.optimizerOptions.compareOptimizerStep(nonOptRmeth,
                                    paramSize, isStatic, cfOptions, advice, rmeth);
                        }

                        if (cfOptions.statistics) {
                            context.codeStatistics.updateRopStatistics(
                                    nonOptRmeth, rmeth);
                        }
                    }

                    LocalVariableInfo locals = null;

                    if (cfOptions.localInfo) {
                        locals = LocalVariableExtractor.extract(rmeth);
                    }

                    code = RopTranslator.translate(rmeth, cfOptions.positionInfo,
                            locals, paramSize, dexOptions);

                    if (cfOptions.statistics && nonOptRmeth != null) {
                        updateDexStatistics(context, cfOptions, dexOptions, rmeth, nonOptRmeth, locals,
                                paramSize, concrete.getCode().size());
                    }
                }

                // Preserve the synchronized flag as its "declared" variant...
                if (AccessFlags.isSynchronized(accessFlags)) {
                    accessFlags |= AccessFlags.ACC_DECLARED_SYNCHRONIZED;

                    /*
                     * ...but only native methods are actually allowed to be
                     * synchronized.
                     */
                    if (!isNative) {
                        accessFlags &= ~AccessFlags.ACC_SYNCHRONIZED;
                    }
                }

                if (isConstructor) {
                    accessFlags |= AccessFlags.ACC_CONSTRUCTOR;
                }

                TypeList exceptions = AttributeTranslator.getExceptions(one);
                EncodedMethod mi =
                    new EncodedMethod(meth, accessFlags, code, exceptions);

                if (meth.isInstanceInit() || meth.isClassInit() ||
                    isStatic || isPrivate) {
                    out.addDirectMethod(mi);
                } else {
                    out.addVirtualMethod(mi);
                }

                Annotations annotations =
                    AttributeTranslator.getMethodAnnotations(one);
                if (annotations.size() != 0) {
                    out.addMethodAnnotations(meth, annotations, dexFile);
                }

                AnnotationsList list =
                    AttributeTranslator.getParameterAnnotations(one);
                if (list.size() != 0) {
                    out.addParameterAnnotations(meth, list, dexFile);
                }
                dexFile.getMethodIds().intern(meth);
            } catch (RuntimeException ex) {
                String msg = "...while processing " + one.getName().toHuman() +
                    " " + one.getDescriptor().toHuman();
                throw ExceptionWithContext.withContext(ex, msg);
            }
        }
    }

    /**
     * Helper that updates the dex statistics.
     */
    private static void updateDexStatistics(DxContext context, CfOptions cfOptions, DexOptions dexOptions,
            RopMethod optRmeth, RopMethod nonOptRmeth,
            LocalVariableInfo locals, int paramSize, int originalByteCount) {
        /*
         * Run rop->dex again on optimized vs. non-optimized method to
         * collect statistics. We have to totally convert both ways,
         * since converting the "real" method getting added to the
         * file would corrupt it (by messing with its constant pool
         * indices).
         */

        DalvCode optCode = RopTranslator.translate(optRmeth,
                cfOptions.positionInfo, locals, paramSize, dexOptions);
        DalvCode nonOptCode = RopTranslator.translate(nonOptRmeth,
                cfOptions.positionInfo, locals, paramSize, dexOptions);

        /*
         * Fake out the indices, so code.getInsns() can work well enough
         * for the current purpose.
         */

        DalvCode.AssignIndicesCallback callback =
            new DalvCode.AssignIndicesCallback() {
                @Override
                public int getIndex(Constant cst) {
                    // Everything is at index 0!
                    return 0;
                }
            };

        optCode.assignIndices(callback);
        nonOptCode.assignIndices(callback);

        context.codeStatistics.updateDexStatistics(nonOptCode, optCode);
        context.codeStatistics.updateOriginalByteCount(originalByteCount);
    }
}
