/*
 * Copyright (C) 2011 The Android Open Source Project
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

package external.com.android.dx;

import external.com.android.dex.DexFormat;
import external.com.android.dx.dex.DexOptions;
import external.com.android.dx.dex.code.DalvCode;
import external.com.android.dx.dex.code.PositionList;
import external.com.android.dx.dex.code.RopTranslator;
import external.com.android.dx.dex.file.ClassDefItem;
import external.com.android.dx.dex.file.DexFile;
import external.com.android.dx.dex.file.EncodedField;
import external.com.android.dx.dex.file.EncodedMethod;
import external.com.android.dx.rop.code.AccessFlags;
import external.com.android.dx.rop.code.LocalVariableInfo;
import external.com.android.dx.rop.code.RopMethod;
import external.com.android.dx.rop.cst.CstString;
import external.com.android.dx.rop.cst.CstType;
import external.com.android.dx.rop.type.StdTypeList;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static external.com.android.dx.rop.code.AccessFlags.ACC_CONSTRUCTOR;
import static java.lang.reflect.Modifier.*;

/**
 * Generates a <strong>D</strong>alvik <strong>EX</strong>ecutable (dex)
 * file for execution on Android. Dex files define classes and interfaces,
 * including their member methods and fields, executable code, and debugging
 * information. They also define annotations, though this API currently has no
 * facility to create a dex file that contains annotations.
 *
 * <p>This library is intended to satisfy two use cases:
 * <ul>
 *   <li><strong>For runtime code generation.</strong> By embedding this library
 *       in your Android application, you can dynamically generate and load
 *       executable code. This approach takes advantage of the fact that the
 *       host environment and target environment are both Android.
 *   <li><strong>For compile time code generation.</strong> You may use this
 *       library as a part of a compiler that targets Android. In this scenario
 *       the generated dex file must be installed on an Android device before it
 *       can be executed.
 * </ul>
 *
 * <h3>Example: Fibonacci</h3>
 * To illustrate how this API is used, we'll use DexMaker to generate a class
 * equivalent to the following Java source: <pre> {@code
 *
 * package com.publicobject.fib;
 *
 * public class Fibonacci {
 *   public static int fib(int i) {
 *     if (i < 2) {
 *       return i;
 *     }
 *     return fib(i - 1) + fib(i - 2);
 *   }
 * }}</pre>
 *
 * <p>We start by creating a {@link TypeId} to identify the generated {@code
 * Fibonacci} class. DexMaker identifies types by their internal names like
 * {@code Ljava/lang/Object;} rather than their Java identifiers like {@code
 * java.lang.Object}. <pre>   {@code
 *
 *   TypeId<?> fibonacci = TypeId.get("Lcom/google/dexmaker/examples/Fibonacci;");
 * }</pre>
 *
 * <p>Next we declare the class. It allows us to specify the type's source file
 * for stack traces, its modifiers, its superclass, and the interfaces it
 * implements. In this case, {@code Fibonacci} is a public class that extends
 * from {@code Object}: <pre>   {@code
 *
 *   String fileName = "Fibonacci.generated";
 *   DexMaker dexMaker = new DexMaker();
 *   dexMaker.declare(fibonacci, fileName, Modifier.PUBLIC, TypeId.OBJECT);
 * }</pre>
 * It is illegal to declare members of a class without also declaring the class
 * itself.
 *
 * <p>To make it easier to go from our Java method to dex instructions, we'll
 * manually translate it to pseudocode fit for an assembler. We need to replace
 * control flow like {@code if()} blocks and {@code for()} loops with labels and
 * branches. We'll also avoid performing multiple operations in one statement,
 * using local variables to hold intermediate values as necessary:
 * <pre>   {@code
 *
 *   int constant1 = 1;
 *   int constant2 = 2;
 *   if (i < constant2) goto baseCase;
 *   int a = i - constant1;
 *   int b = i - constant2;
 *   int c = fib(a);
 *   int d = fib(b);
 *   int result = c + d;
 *   return result;
 * baseCase:
 *   return i;
 * }</pre>
 *
 * <p>We look up the {@code MethodId} for the method on the declaring type. This
 * takes the method's return type (possibly {@link TypeId#VOID}), its name and
 * its parameters types. Next we declare the method, specifying its modifiers by
 * bitwise ORing constants from {@link java.lang.reflect.Modifier}. The declare
 * call returns a {@link Code} object, which we'll use to define the method's
 * instructions. <pre>   {@code
 *
 *   MethodId<?, Integer> fib = fibonacci.getMethod(TypeId.INT, "fib", TypeId.INT);
 *   Code code = dexMaker.declare(fib, Modifier.PUBLIC | Modifier.STATIC);
 * }</pre>
 *
 * <p>One limitation of {@code DexMaker}'s API is that it requires all local
 * variables to be created before any instructions are emitted. Use {@link
 * Code#newLocal newLocal()} to create a new local variable. The method's
 * parameters are exposed as locals using {@link Code#getParameter
 * getParameter()}. For non-static methods the {@code this} pointer is exposed
 * using {@link Code#getThis getThis()}. Here we declare all of the local
 * variables that we'll need for our {@code fib()} method: <pre>   {@code
 *
 *   Local<Integer> i = code.getParameter(0, TypeId.INT);
 *   Local<Integer> constant1 = code.newLocal(TypeId.INT);
 *   Local<Integer> constant2 = code.newLocal(TypeId.INT);
 *   Local<Integer> a = code.newLocal(TypeId.INT);
 *   Local<Integer> b = code.newLocal(TypeId.INT);
 *   Local<Integer> c = code.newLocal(TypeId.INT);
 *   Local<Integer> d = code.newLocal(TypeId.INT);
 *   Local<Integer> result = code.newLocal(TypeId.INT);
 * }</pre>
 *
 * <p>Notice that {@link Local} has a type parameter of {@code Integer}. This is
 * useful for generating code that works with existing types like {@code String}
 * and {@code Integer}, but it can be a hindrance when generating code that
 * involves new types. For this reason you may prefer to use raw types only and
 * add {@code @SuppressWarnings("unsafe")} on your calling code. This will yield
 * the same result but you won't get IDE support if you make a type error.
 *
 * <p>We're ready to start defining our method's instructions. The {@link Code}
 * class catalogs the available instructions and their use. <pre>   {@code
 *
 *   code.loadConstant(constant1, 1);
 *   code.loadConstant(constant2, 2);
 *   Label baseCase = new Label();
 *   code.compare(Comparison.LT, baseCase, i, constant2);
 *   code.op(BinaryOp.SUBTRACT, a, i, constant1);
 *   code.op(BinaryOp.SUBTRACT, b, i, constant2);
 *   code.invokeStatic(fib, c, a);
 *   code.invokeStatic(fib, d, b);
 *   code.op(BinaryOp.ADD, result, c, d);
 *   code.returnValue(result);
 *   code.mark(baseCase);
 *   code.returnValue(i);
 * }</pre>
 *
 * <p>We're done defining the dex file. We just need to write it to the
 * filesystem or load it into the current process. For this example we'll load
 * the generated code into the current process. This only works when the current
 * process is running on Android. We use {@link #generateAndLoad
 * generateAndLoad()} which takes the class loader that will be used as our
 * generated code's parent class loader. It also requires a directory where
 * temporary files can be written. <pre>   {@code
 *
 *   ClassLoader loader = dexMaker.generateAndLoad(
 *       FibonacciMaker.class.getClassLoader(), getDataDirectory());
 * }</pre>
 * Finally we'll use reflection to lookup our generated class on its class
 * loader and invoke its {@code fib()} method: <pre>   {@code
 *
 *   Class<?> fibonacciClass = loader.loadClass("com.google.dexmaker.examples.Fibonacci");
 *   Method fibMethod = fibonacciClass.getMethod("fib", int.class);
 *   System.out.println(fibMethod.invoke(null, 8));
 * }</pre>
 */
public final class DexMaker {
    private final Map<TypeId<?>, TypeDeclaration> types = new LinkedHashMap<>();

    // Only warn about not being able to deal with blacklisted methods once. Often this is no
    // problem and warning on every class load is too spammy.
    private static boolean didWarnBlacklistedMethods;
    private static boolean didWarnNonBaseDexClassLoader;

    private ClassLoader sharedClassLoader;
    private DexFile outputDex;
    private boolean markAsTrusted;

    /**
     * Creates a new {@code DexMaker} instance, which can be used to create a
     * single dex file.
     */
    public DexMaker() {
    }

    TypeDeclaration getTypeDeclaration(TypeId<?> type) {
        TypeDeclaration result = types.get(type);
        if (result == null) {
            result = new TypeDeclaration(type);
            types.put(type, result);
        }
        return result;
    }

    /**
     * Declares {@code type}.
     *
     * @param flags a bitwise combination of {@link Modifier#PUBLIC}, {@link
     *     Modifier#FINAL} and {@link Modifier#ABSTRACT}.
     */
    public void declare(TypeId<?> type, String sourceFile, int flags,
                        TypeId<?> supertype, TypeId<?>... interfaces) {
        TypeDeclaration declaration = getTypeDeclaration(type);
        int supportedFlags = Modifier.PUBLIC | Modifier.FINAL | Modifier.ABSTRACT
                | AccessFlags.ACC_SYNTHETIC;
        if ((flags & ~supportedFlags) != 0) {
            throw new IllegalArgumentException("Unexpected flag: "
                    + Integer.toHexString(flags));
        }
        if (declaration.declared) {
            throw new IllegalStateException("already declared: " + type);
        }
        declaration.declared = true;
        declaration.flags = flags;
        declaration.supertype = supertype;
        declaration.sourceFile = sourceFile;
        declaration.interfaces = new TypeList(interfaces);
    }

    /**
     * Declares a method or constructor.
     *
     * @param flags a bitwise combination of {@link Modifier#PUBLIC}, {@link
     *     Modifier#PRIVATE}, {@link Modifier#PROTECTED}, {@link Modifier#STATIC},
     *     {@link Modifier#FINAL} and {@link Modifier#SYNCHRONIZED}.
     *     <p><strong>Warning:</strong> the {@link Modifier#SYNCHRONIZED} flag
     *     is insufficient to generate a synchronized method. You must also use
     *     {@link Code#monitorEnter} and {@link Code#monitorExit} to acquire
     *     a monitor.
     */
    public Code declare(MethodId<?, ?> method, int flags) {
        TypeDeclaration typeDeclaration = getTypeDeclaration(method.declaringType);
        if (typeDeclaration.methods.containsKey(method)) {
            throw new IllegalStateException("already declared: " + method);
        }

        int supportedFlags = Modifier.ABSTRACT | Modifier.NATIVE | Modifier.PUBLIC | Modifier.PRIVATE
                | Modifier.PROTECTED | Modifier.STATIC | Modifier.FINAL | Modifier.SYNCHRONIZED | Modifier.TRANSIENT
                | AccessFlags.ACC_SYNTHETIC | AccessFlags.ACC_BRIDGE;
        if ((flags & ~supportedFlags) != 0) {
            throw new IllegalArgumentException("Unexpected flag: "
                    + Integer.toHexString(flags));
        }

        // replace the SYNCHRONIZED flag with the DECLARED_SYNCHRONIZED flag
        if ((flags & Modifier.SYNCHRONIZED) != 0) {
            flags = (flags & ~Modifier.SYNCHRONIZED) | AccessFlags.ACC_DECLARED_SYNCHRONIZED;
        }

        if (method.isConstructor() || method.isStaticInitializer()) {
            flags |= ACC_CONSTRUCTOR;
        }

        MethodDeclaration methodDeclaration = new MethodDeclaration(method, flags);
        typeDeclaration.methods.put(method, methodDeclaration);
        return methodDeclaration.code;
    }

    /**
     * Declares a field.
     *
     * @param flags a bitwise combination of {@link Modifier#PUBLIC}, {@link
     *     Modifier#PRIVATE}, {@link Modifier#PROTECTED}, {@link Modifier#STATIC},
     *     {@link Modifier#FINAL}, {@link Modifier#VOLATILE}, and {@link
     *     Modifier#TRANSIENT}.
     * @param staticValue a constant representing the initial value for the
     *     static field, possibly null. This must be null if this field is
     *     non-static.
     */
    public void declare(FieldId<?, ?> fieldId, int flags, Object staticValue) {
        TypeDeclaration typeDeclaration = getTypeDeclaration(fieldId.declaringType);
        if (typeDeclaration.fields.containsKey(fieldId)) {
            throw new IllegalStateException("already declared: " + fieldId);
        }

        int supportedFlags = Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED
                | Modifier.STATIC | Modifier.FINAL | Modifier.VOLATILE | Modifier.TRANSIENT
                | AccessFlags.ACC_SYNTHETIC;
        if ((flags & ~supportedFlags) != 0) {
            throw new IllegalArgumentException("Unexpected flag: "
                    + Integer.toHexString(flags));
        }

        if ((flags & Modifier.STATIC) == 0 && staticValue != null) {
            throw new IllegalArgumentException("staticValue is non-null, but field is not static");
        }

        FieldDeclaration fieldDeclaration = new FieldDeclaration(fieldId, flags, staticValue);
        typeDeclaration.fields.put(fieldId, fieldDeclaration);
    }

    /**
     * Generates a dex file and returns its bytes.
     */
    public byte[] generate() {
        if (outputDex == null) {
            DexOptions options = new DexOptions();
            options.minSdkVersion = DexFormat.API_NO_EXTENDED_OPCODES;
            outputDex = new DexFile(options);
        }

        for (TypeDeclaration typeDeclaration : types.values()) {
            outputDex.add(typeDeclaration.toClassDefItem());
        }

        try {
            return outputDex.toDex(null, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Generate a file name for the jar by taking a checksum of MethodIds and
    // parent class types.
    private String generateFileName() {
        int checksum = 1;

        Set<TypeId<?>> typesKeySet = types.keySet();
        Iterator<TypeId<?>> it = typesKeySet.iterator();
        int[] checksums = new int[typesKeySet.size()];
        int i = 0;

        while (it.hasNext()) {
            TypeId<?> typeId = it.next();
            TypeDeclaration decl = getTypeDeclaration(typeId);
            Set<MethodId> methodSet = decl.methods.keySet();
            if (decl.supertype != null) {
                int sum = 31 * decl.supertype.hashCode() + decl.interfaces.hashCode();
                checksums[i++] = 31 * sum + methodSet.hashCode();
            }
        }
        Arrays.sort(checksums);

        for (int sum : checksums) {
            checksum *= 31;
            checksum += sum;
        }

        return "Generated_" + checksum +".jar";
    }

    /**
     * Set shared class loader to use.
     *
     * <p>If a class wants to call package private methods of another class they need to share a
     * class loader. One common case for this requirement is a mock class wanting to mock package
     * private methods of the original class.
     *
     * <p>If the classLoader is not a subclass of {@code dalvik.system.BaseDexClassLoader} this
     * option is ignored.
     *
     * @param classLoader the class loader the new class should be loaded by
     */
    public void setSharedClassLoader(ClassLoader classLoader) {
        this.sharedClassLoader = classLoader;
    }

    public void markAsTrusted() {
        this.markAsTrusted = true;
    }

    private ClassLoader generateClassLoader(File result, File dexCache, ClassLoader parent) {
        try {
            boolean shareClassLoader = sharedClassLoader != null;

            ClassLoader preferredClassLoader = null;
            if (parent != null) {
                preferredClassLoader = parent;
            } else if (sharedClassLoader != null) {
                preferredClassLoader = sharedClassLoader;
            }

            Class baseDexClassLoaderClass = Class.forName("dalvik.system.BaseDexClassLoader");

            if (shareClassLoader) {
                if (!baseDexClassLoaderClass.isAssignableFrom(preferredClassLoader.getClass())) {
                    if (!preferredClassLoader.getClass().getName().equals(
                            "java.lang.BootClassLoader")) {
                        if (!didWarnNonBaseDexClassLoader) {
                            System.err.println("Cannot share classloader as shared classloader '"
                                    + preferredClassLoader + "' is not a subclass of '"
                                    + baseDexClassLoaderClass
                                    + "'");
                            didWarnNonBaseDexClassLoader = true;
                        }
                    }

                    shareClassLoader = false;
                }
            }

            // Try to load the class so that it can call hidden APIs. This is required for spying
            // on system classes as real-methods of these classes might call blacklisted APIs
            if (markAsTrusted) {
                try {
                    if (shareClassLoader) {
                        preferredClassLoader.getClass().getMethod("addDexPath", String.class,
                                Boolean.TYPE).invoke(preferredClassLoader, result.getPath(), true);
                        return preferredClassLoader;
                    } else {
                        return (ClassLoader) baseDexClassLoaderClass
                                .getConstructor(String.class, File.class, String.class,
                                        ClassLoader.class, Boolean.TYPE)
                                .newInstance(result.getPath(), dexCache.getAbsoluteFile(), null,
                                        preferredClassLoader, true);
                    }
                } catch (InvocationTargetException e) {
                    if (e.getCause() instanceof SecurityException) {
                        if (!didWarnBlacklistedMethods) {
                            System.err.println("Cannot allow to call blacklisted super methods. "
                                    + "This might break spying on system classes." + e.getCause());
                            didWarnBlacklistedMethods = true;
                        }
                    } else {
                        throw e;
                    }
                }
            }

            if (shareClassLoader) {
                preferredClassLoader.getClass().getMethod("addDexPath", String.class).invoke(
                        preferredClassLoader, result.getPath());
                return preferredClassLoader;
            } else {
                return (ClassLoader) Class.forName("dalvik.system.DexClassLoader")
                        .getConstructor(String.class, String.class, String.class, ClassLoader.class)
                        .newInstance(result.getPath(), dexCache.getAbsolutePath(), null,
                                preferredClassLoader);
            }
        } catch (ClassNotFoundException e) {
            throw new UnsupportedOperationException("load() requires a Dalvik VM", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getCause());
        } catch (InstantiationException e) {
            throw new AssertionError();
        } catch (NoSuchMethodException e) {
            throw new AssertionError();
        } catch (IllegalAccessException e) {
            throw new AssertionError();
        }
    }

    public ClassLoader loadClassDirect(ClassLoader parent, File dexCache, String dexFileName) {
        File result = new File(dexCache, dexFileName);
        // Check that the file exists. If it does, return a DexClassLoader and skip all
        // the dex bytecode generation.
        if (result.exists()) {
            return generateClassLoader(result, dexCache, parent);
        } else {
            return null;
        }
    }

    public ClassLoader generateAndLoad(ClassLoader parent, File dexCache) throws IOException {
        return generateAndLoad(parent, dexCache, generateFileName(), false);
    }

    public ClassLoader generateAndLoad(ClassLoader parent, File dexCache, String dexFileName) throws IOException {
        return generateAndLoad(parent, dexCache, dexFileName, false);
    }

    /**
     * Generates a dex file and loads its types into the current process.
     *
     * <h3>Picking a dex cache directory</h3>
     * The {@code dexCache} should be an application-private directory. If
     * you pass a world-writable directory like {@code /sdcard} a malicious app
     * could inject code into your process. Most applications should use this:
     * <pre>   {@code
     *
     *     File dexCache = getApplicationContext().getDir("dx", Context.MODE_PRIVATE);
     * }</pre>
     * If the {@code dexCache} is null, this method will consult the {@code
     * dexmaker.dexcache} system property. If that exists, it will be used for
     * the dex cache. If it doesn't exist, this method will attempt to guess
     * the application's private data directory as a last resort. If that fails,
     * this method will fail with an unchecked exception. You can avoid the
     * exception by either providing a non-null value or setting the system
     * property.
     *
     * @param parent the parent ClassLoader to be used when loading our
     *     generated types (if set, overrides
     *     {@link #setSharedClassLoader(ClassLoader) shared class loader}.
     * @param dexCache the destination directory where generated and optimized
     *     dex files will be written. If null, this class will try to guess the
     *     application's private data dir.
     */
    public ClassLoader generateAndLoad(ClassLoader parent, File dexCache, String dexFileName, boolean deleteOld) throws IOException {
        if (dexCache == null) {
            String property = System.getProperty("dexmaker.dexcache");
            if (property != null) {
                dexCache = new File(property);
            } else {
                dexCache = new AppDataDirGuesser().guess();
                if (dexCache == null) {
                    throw new IllegalArgumentException("dexcache == null (and no default could be"
                            + " found; consider setting the 'dexmaker.dexcache' system property)");
                }
            }
        }

        File result = new File(dexCache, dexFileName);

        if (result.exists()) {
            if (deleteOld) {
                try {
                    deleteOldDex(result);
                } catch (Throwable throwable) {
                }
            } else {
                return generateClassLoader(result, dexCache, parent);
            }
        }

        /*
         * This implementation currently dumps the dex to the filesystem. It
         * jars the emitted .dex for the benefit of Gingerbread and earlier
         * devices, which can't load .dex files directly.
         *
         * TODO: load the dex from memory where supported.
         */

        File parentDir = result.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }

        result.createNewFile();

        JarOutputStream jarOut =
            new JarOutputStream(new BufferedOutputStream(new FileOutputStream(result)));
        try {
            JarEntry entry = new JarEntry(DexFormat.DEX_IN_JAR_NAME);
            byte[] dex = generate();
            entry.setSize(dex.length);
            jarOut.putNextEntry(entry);
            try {
                jarOut.write(dex);
            } finally {
                jarOut.closeEntry();
            }
        } finally {
            jarOut.close();
        }

        return generateClassLoader(result, dexCache, parent);
    }

    public void deleteOldDex(File dexFile) {
        dexFile.delete();
        String dexDir = dexFile.getParent();
        File oatDir = new File(dexDir, "/oat/");
        File oatDirArm = new File(oatDir, "/arm/");
        File oatDirArm64 = new File(oatDir, "/arm64/");
        if (!oatDir.exists())
            return;
        String nameStart = dexFile.getName().replaceAll(".jar", "");
        doDeleteOatFiles(oatDir, nameStart);
        doDeleteOatFiles(oatDirArm, nameStart);
        doDeleteOatFiles(oatDirArm64, nameStart);
    }

    private void doDeleteOatFiles(File dir, String nameStart) {
        if (!dir.exists())
            return;
        File[] oats = dir.listFiles();
        if (oats == null)
            return;
        for (File oatFile:oats) {
            if (oatFile.isFile() && oatFile.getName().startsWith(nameStart))
                oatFile.delete();
        }
    }

    DexFile getDexFile() {
        if (outputDex == null) {
            DexOptions options = new DexOptions();
            options.minSdkVersion = DexFormat.API_NO_EXTENDED_OPCODES;
            outputDex = new DexFile(options);
        }
        return outputDex;
    }

    static class TypeDeclaration {
        private final TypeId<?> type;

        /** declared state */
        private boolean declared;
        private int flags;
        private TypeId<?> supertype;
        private String sourceFile;
        private TypeList interfaces;
        private ClassDefItem classDefItem;

        private final Map<FieldId, FieldDeclaration> fields = new LinkedHashMap<>();
        private final Map<MethodId, MethodDeclaration> methods = new LinkedHashMap<>();

        TypeDeclaration(TypeId<?> type) {
            this.type = type;
        }

        ClassDefItem toClassDefItem() {
            if (!declared) {
                throw new IllegalStateException("Undeclared type " + type + " declares members: "
                        + fields.keySet() + " " + methods.keySet());
            }

            DexOptions dexOptions = new DexOptions();
            dexOptions.minSdkVersion = DexFormat.API_NO_EXTENDED_OPCODES;

            CstType thisType = type.constant;

            if (classDefItem == null) {
                classDefItem = new ClassDefItem(thisType, flags, supertype.constant,
                        interfaces.ropTypes, new CstString(sourceFile));

                for (MethodDeclaration method : methods.values()) {
                    EncodedMethod encoded = method.toEncodedMethod(dexOptions);
                    if (method.isDirect()) {
                        classDefItem.addDirectMethod(encoded);
                    } else {
                        classDefItem.addVirtualMethod(encoded);
                    }
                }
                for (FieldDeclaration field : fields.values()) {
                    EncodedField encoded = field.toEncodedField();
                    if (field.isStatic()) {
                        classDefItem.addStaticField(encoded, Constants.getConstant(field.staticValue));
                    } else {
                        classDefItem.addInstanceField(encoded);
                    }
                }
            }

            return classDefItem;
        }
    }

    static class FieldDeclaration {
        final FieldId<?, ?> fieldId;
        private final int accessFlags;
        private final Object staticValue;

        FieldDeclaration(FieldId<?, ?> fieldId, int accessFlags, Object staticValue) {
            if ((accessFlags & STATIC) == 0 && staticValue != null) {
                throw new IllegalArgumentException("instance fields may not have a value");
            }
            this.fieldId = fieldId;
            this.accessFlags = accessFlags;
            this.staticValue = staticValue;
        }

        EncodedField toEncodedField() {
            return new EncodedField(fieldId.constant, accessFlags);
        }

        public boolean isStatic() {
            return (accessFlags & STATIC) != 0;
        }
    }

    static class MethodDeclaration {
        final MethodId<?, ?> method;
        private final int flags;
        private final Code code;

        public MethodDeclaration(MethodId<?, ?> method, int flags) {
            this.method = method;
            this.flags = flags;
            this.code = new Code(this);
        }

        boolean isStatic() {
            return (flags & STATIC) != 0;
        }

        boolean isDirect() {
            return (flags & (STATIC | PRIVATE | ACC_CONSTRUCTOR)) != 0;
        }

        EncodedMethod toEncodedMethod(DexOptions dexOptions) {
            if ((flags & ABSTRACT) != 0 || (flags & NATIVE) != 0) {
                return new EncodedMethod(method.constant, flags, null, StdTypeList.EMPTY);
            }
			
            RopMethod ropMethod = new RopMethod(code.toBasicBlocks(), 0);
            LocalVariableInfo locals = null;
            DalvCode dalvCode = RopTranslator.translate(
                    ropMethod, PositionList.NONE, locals, code.paramSize(), dexOptions);
            return new EncodedMethod(method.constant, flags, dalvCode, StdTypeList.EMPTY);
        }
    }
}
