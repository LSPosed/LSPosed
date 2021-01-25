package org.meowcat.edxposed.manager.util.light;

import android.util.Log;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Java reflection helper optimized for hacking non-public APIs.
 * The core design philosophy behind is compile-time consistency enforcement.
 * <p>
 * It's suggested to declare all hacks in a centralized point, typically as static fields in a class.
 * Then call it during application initialization, thus they are verified all together in an early stage.
 * If any assertion failed, a fall-back strategy is suggested.
 *
 * <p>https://gist.github.com/oasisfeng/75d3774ca5441372f049818de4d52605
 *
 * @author Oasis
 * @see Demo
 */
@SuppressWarnings({"Convert2Lambda", "WeakerAccess", "unused"})
class Hack {

    public static Class<?> ANY_TYPE = $.class;
    private static final HackedClass<?> FALLBACK = new HackedClass<>(ANY_TYPE);
    private static AssertionFailureHandler sFailureHandler;

    private Hack() {
    }

    public static <T> HackedClass<T> into(final @NonNull Class<T> clazz) {
        return new HackedClass<>(clazz);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T> HackedClass<T> into(final String class_name) {
        try {
            return new HackedClass(Class.forName(class_name));
        } catch (final ClassNotFoundException e) {
            fail(new AssertionException(e));
            return new HackedClass(ANY_TYPE);        // Use AnyType as a lazy trick to make fallback working and avoid null.
        }
    }

    @SuppressWarnings("unchecked")
    public static <C> HackedClass<C> onlyIf(final boolean condition, final Hacking<HackedClass<C>> hacking) {
        if (condition) return hacking.hack();
        return (HackedClass<C>) FALLBACK;
    }

    public static ConditionalHack onlyIf(final boolean condition) {
        return condition ? new ConditionalHack() {
            @Override
            public <T> HackedClass<T> into(@NonNull final Class<T> clazz) {
                return Hack.into(clazz);
            }

            @Override
            public <T> HackedClass<T> into(final String class_name) {
                return Hack.into(class_name);
            }
        } : new ConditionalHack() {
            @SuppressWarnings("unchecked")
            @Override
            public <T> HackedClass<T> into(@NonNull final Class<T> clazz) {
                return (HackedClass<T>) FALLBACK;
            }

            @SuppressWarnings("unchecked")
            @Override
            public <T> HackedClass<T> into(final String class_name) {
                return (HackedClass<T>) FALLBACK;
            }
        };
    }

    private static void fail(final AssertionException e) {
        if (sFailureHandler != null) sFailureHandler.onAssertionFailure(e);
    }

    /**
     * Specify a handler to deal with assertion failure, and decide whether the failure should be thrown.
     */
    public static AssertionFailureHandler setAssertionFailureHandler(final AssertionFailureHandler handler) {
        final AssertionFailureHandler old = sFailureHandler;
        sFailureHandler = handler;
        return old;
    }

    /**
     * Use {@link Hack#setAssertionFailureHandler(AssertionFailureHandler) } to set the global handler
     */
    public interface AssertionFailureHandler {
        void onAssertionFailure(AssertionException failure);
    }

    public interface HackedField<C, T> {
        T get(C instance);

        void set(C instance, T value);

        HackedTargetField<T> on(C target);

        Class<T> getType();

        boolean isAbsent();
    }

    public interface HackedTargetField<T> {
        T get();

        void set(T value);

        Class<T> getType();

        boolean isAbsent();
    }

    public interface HackedInvokable<R, C, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable> {
        @CheckResult
        <TT1 extends Throwable> HackedInvokable<R, C, TT1, T2, T3> throwing(Class<TT1> type);

        @CheckResult
        <TT1 extends Throwable, TT2 extends Throwable> HackedInvokable<R, C, TT1, TT2, T3> throwing(Class<TT1> type1, Class<TT2> type2);

        @CheckResult
        <TT1 extends Throwable, TT2 extends Throwable, TT3 extends Throwable> HackedInvokable<R, C, TT1, TT2, TT3> throwing(Class<TT1> type1, Class<TT2> type2, Class<TT3> type3);

        @Nullable
        HackedMethod0<R, C, T1, T2, T3> withoutParams();

        @Nullable
        <A1> HackedMethod1<R, C, T1, T2, T3, A1> withParam(Class<A1> type);

        @Nullable
        <A1, A2> HackedMethod2<R, C, T1, T2, T3, A1, A2> withParams(Class<A1> type1, Class<A2> type2);

        @Nullable
        <A1, A2, A3> HackedMethod3<R, C, T1, T2, T3, A1, A2, A3> withParams(Class<A1> type1, Class<A2> type2, Class<A3> type3);

        @Nullable
        <A1, A2, A3, A4> HackedMethod4<R, C, T1, T2, T3, A1, A2, A3, A4> withParams(Class<A1> type1, Class<A2> type2, Class<A3> type3, Class<A4> type4);

        @Nullable
        <A1, A2, A3, A4, A5> HackedMethod5<R, C, T1, T2, T3, A1, A2, A3, A4, A5> withParams(Class<A1> type1, final Class<A2> type2, final Class<A3> type3, final Class<A4> type4, final Class<A5> type5);

        @Nullable
        HackedMethodN<R, C, T1, T2, T3> withParams(Class<?>... types);
    }

    public interface NonNullHackedInvokable<R, C, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable> extends HackedInvokable<R, C, T1, T2, T3> {
        @CheckResult
        <TT1 extends Throwable> NonNullHackedInvokable<R, C, TT1, T2, T3> throwing(Class<TT1> type);

        @CheckResult
        <TT1 extends Throwable, TT2 extends Throwable> NonNullHackedInvokable<R, C, TT1, TT2, T3> throwing(Class<TT1> type1, Class<TT2> type2);

        @CheckResult
        <TT1 extends Throwable, TT2 extends Throwable, TT3 extends Throwable> NonNullHackedInvokable<R, C, TT1, TT2, TT3> throwing(Class<TT1> type1, Class<TT2> type2, Class<TT3> type3);

        @NonNull
        HackedMethod0<R, C, T1, T2, T3> withoutParams();

        @NonNull
        <A1> HackedMethod1<R, C, T1, T2, T3, A1> withParam(Class<A1> type);

        @NonNull
        <A1, A2> HackedMethod2<R, C, T1, T2, T3, A1, A2> withParams(Class<A1> type1, Class<A2> type2);

        @NonNull
        <A1, A2, A3> HackedMethod3<R, C, T1, T2, T3, A1, A2, A3> withParams(Class<A1> type1, Class<A2> type2, Class<A3> type3);

        @NonNull
        <A1, A2, A3, A4> HackedMethod4<R, C, T1, T2, T3, A1, A2, A3, A4> withParams(Class<A1> type1, Class<A2> type2, Class<A3> type3, Class<A4> type4);

        @NonNull
        <A1, A2, A3, A4, A5> HackedMethod5<R, C, T1, T2, T3, A1, A2, A3, A4, A5> withParams(Class<A1> type1, final Class<A2> type2, final Class<A3> type3, final Class<A4> type4, final Class<A5> type5);

        @NonNull
        HackedMethodN<R, C, T1, T2, T3> withParams(Class<?>... types);
    }

    public interface HackedMethod<R, C, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable> extends HackedInvokable<R, C, T1, T2, T3> {
        /**
         * Optional
         */
        @CheckResult
        <RR> HackedMethod<RR, C, T1, T2, T3> returning(Class<RR> type);

        /**
         * Fallback to the given value if this field is unavailable at runtime. (Optional)
         */
        @CheckResult
        NonNullHackedMethod<R, C, T1, T2, T3> fallbackReturning(R return_value);

        @CheckResult
        <TT1 extends Throwable> HackedMethod<R, C, TT1, T2, T3> throwing(Class<TT1> type);

        @CheckResult
        <TT1 extends Throwable, TT2 extends Throwable> HackedMethod<R, C, TT1, TT2, T3> throwing(Class<TT1> type1, Class<TT2> type2);

        @CheckResult
        <TT1 extends Throwable, TT2 extends Throwable, TT3 extends Throwable> HackedMethod<R, C, TT1, TT2, TT3> throwing(Class<TT1> type1, Class<TT2> type2, Class<TT3> type3);

        @CheckResult
        HackedMethod<R, C, Exception, T2, T3> throwing(Class<?>... types);
    }

    @SuppressWarnings("NullableProblems")    // Force to NonNull
    public interface NonNullHackedMethod<R, C, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable> extends HackedMethod<R, C, T1, T2, T3>, NonNullHackedInvokable<R, C, T1, T2, T3> {
        /**
         * Optional
         */
        @CheckResult
        <RR> HackedMethod<RR, C, T1, T2, T3> returning(Class<RR> type);

        @CheckResult
        <TT1 extends Throwable> NonNullHackedMethod<R, C, TT1, T2, T3> throwing(Class<TT1> type);

        @CheckResult
        <TT1 extends Throwable, TT2 extends Throwable> NonNullHackedMethod<R, C, TT1, TT2, T3> throwing(Class<TT1> type1, Class<TT2> type2);

        @CheckResult
        <TT1 extends Throwable, TT2 extends Throwable, TT3 extends Throwable> NonNullHackedMethod<R, C, TT1, TT2, TT3> throwing(Class<TT1> type1, Class<TT2> type2, Class<TT3> type3);
    }

    interface Invokable<C> {
        Object invoke(C target, Object[] args) throws InvocationTargetException, IllegalAccessException, InstantiationException;

        Class<?> getReturnType();
    }

    public interface Hacking<T> {
        T hack();
    }

    public interface ConditionalHack {
        /**
         * WARNING: Never use this method if the target class may not exist when the condition is not met, use {@link #onlyIf(boolean, Hacking)} instead.
         */
        <T> HackedClass<T> into(final @NonNull Class<T> clazz);

        <T> HackedClass<T> into(final String class_name);
    }

    private static class $ {
    }

    public static class AssertionException extends Throwable {

        private static final long serialVersionUID = 1L;
        private Class<?> mClass;
        private Field mHackedField;
        private Method mHackedMethod;
        private String mHackedFieldName;
        private String mHackedMethodName;
        private Class<?>[] mParamTypes;

        AssertionException(final String e) {
            super(e);
        }

        AssertionException(final Exception e) {
            super(e);
        }

        @Override
        public String toString() {
            return getCause() != null ? getClass().getName() + ": " + getCause() : super.toString();
        }

        public String getDebugInfo() {
            final StringBuilder info = new StringBuilder(getCause() != null ? getCause().toString() : super.toString());
            final Throwable cause = getCause();
            if (cause instanceof NoSuchMethodException) {
                info.append(" Potential candidates:");
                final int initial_length = info.length();
                final String name = getHackedMethodName();
                if (name != null) {
                    for (final Method method : getHackedClass().getDeclaredMethods())
                        if (method.getName().equals(name))            // Exact name match
                            info.append(' ').append(method);
                    if (info.length() == initial_length)
                        for (final Method method : getHackedClass().getDeclaredMethods())
                            if (method.getName().startsWith(name))    // Name prefix match
                                info.append(' ').append(method);
                    if (info.length() == initial_length)
                        for (final Method method : getHackedClass().getDeclaredMethods())
                            if (!method.getName().startsWith("-"))    // Dump all but generated methods
                                info.append(' ').append(method);
                } else
                    for (final Constructor<?> constructor : getHackedClass().getDeclaredConstructors())
                        info.append(' ').append(constructor);
            } else if (cause instanceof NoSuchFieldException) {
                info.append(" Potential candidates:");
                final int initial_length = info.length();
                final String name = getHackedFieldName();
                final Field[] fields = getHackedClass().getDeclaredFields();
                for (final Field field : fields)
                    if (field.getName().equals(name))                // Exact name match
                        info.append(' ').append(field);
                if (info.length() == initial_length) for (final Field field : fields)
                    if (field.getName().startsWith(name))            // Name prefix match
                        info.append(' ').append(field);
                if (info.length() == initial_length) for (final Field field : fields)
                    if (!field.getName().startsWith("$"))            // Dump all but generated fields
                        info.append(' ').append(field);
            }
            return info.toString();
        }

        public Class<?> getHackedClass() {
            return mClass;
        }

        AssertionException setHackedClass(final Class<?> hacked_class) {
            mClass = hacked_class;
            return this;
        }

        public Method getHackedMethod() {
            return mHackedMethod;
        }

        AssertionException setHackedMethod(final Method method) {
            mHackedMethod = method;
            return this;
        }

        public String getHackedMethodName() {
            return mHackedMethodName;
        }

        AssertionException setHackedMethodName(final String method) {
            mHackedMethodName = method;
            return this;
        }

        public Class<?>[] getParamTypes() {
            return mParamTypes;
        }

        AssertionException setParamTypes(final Class<?>[] param_types) {
            mParamTypes = param_types;
            return this;
        }

        public Field getHackedField() {
            return mHackedField;
        }

        AssertionException setHackedField(final Field field) {
            mHackedField = field;
            return this;
        }

        public String getHackedFieldName() {
            return mHackedFieldName;
        }

        AssertionException setHackedFieldName(final String field) {
            mHackedFieldName = field;
            return this;
        }
    }

    public static class FieldToHack<C> {

        protected final Class<C> mClass;
        protected final String mName;
        protected final int mModifiers;

        /**
         * @param modifiers the modifiers this field must have
         */
        protected FieldToHack(final Class<C> clazz, final String name, final int modifiers) {
            mClass = clazz;
            mName = name;
            mModifiers = modifiers;
        }

        protected @Nullable
        <T> Field findField(final @Nullable Class<T> type) {
            if (mClass == ANY_TYPE)
                return null;        // AnyType as a internal indicator for class not found.
            Field field = null;
            try {
                field = mClass.getDeclaredField(mName);
                if (Modifier.isStatic(mModifiers) != Modifier.isStatic(field.getModifiers())) {
                    fail(new AssertionException(field + (Modifier.isStatic(mModifiers) ? " is not static" : " is static")).setHackedFieldName(mName));
                    field = null;
                } else if (mModifiers > 0 && (field.getModifiers() & mModifiers) != mModifiers) {
                    fail(new AssertionException(field + " does not match modifiers: " + mModifiers).setHackedFieldName(mName));
                    field = null;
                } else if (!field.isAccessible()) field.setAccessible(true);
            } catch (final NoSuchFieldException e) {
                final AssertionException hae = new AssertionException(e);
                hae.setHackedClass(mClass);
                hae.setHackedFieldName(mName);
                fail(hae);
            }

            if (type != null && field != null && !type.isAssignableFrom(field.getType()))
                fail(new AssertionException(new ClassCastException(field + " is not of type " + type)).setHackedField(field));
            return field;
        }
    }

    public static class MemberFieldToHack<C> extends FieldToHack<C> {

        /**
         * @param modifiers the modifiers this field must have
         */
        private MemberFieldToHack(final Class<C> clazz, final String name, final int modifiers) {
            super(clazz, name, modifiers);
        }

        /**
         * Assert the field type.
         */
        public @Nullable
        <T> HackedField<C, T> ofType(final Class<T> type) {
            return ofType(type, false, null);
        }

        public @Nullable
        <T> HackedField<C, T> ofType(final String type_name) {
            try { //noinspection unchecked
                return ofType((Class<T>) Class.forName(type_name, false, mClass.getClassLoader()));
            } catch (final ClassNotFoundException e) {
                fail(new AssertionException(e));
                return null;
            }
        }

        public @NonNull
        HackedField<C, Byte> fallbackTo(final byte value) { //noinspection ConstantConditions
            return ofType(byte.class, true, value);
        }

        public @NonNull
        HackedField<C, Character> fallbackTo(final char value) { //noinspection ConstantConditions
            return ofType(char.class, true, value);
        }

        public @NonNull
        HackedField<C, Short> fallbackTo(final short value) { //noinspection ConstantConditions
            return ofType(short.class, true, value);
        }

        public @NonNull
        HackedField<C, Integer> fallbackTo(final int value) { //noinspection ConstantConditions
            return ofType(int.class, true, value);
        }

        public @NonNull
        HackedField<C, Long> fallbackTo(final long value) { //noinspection ConstantConditions
            return ofType(long.class, true, value);
        }

        public @NonNull
        HackedField<C, Boolean> fallbackTo(final boolean value) { //noinspection ConstantConditions
            return ofType(boolean.class, true, value);
        }

        public @NonNull
        HackedField<C, Float> fallbackTo(final float value) { //noinspection ConstantConditions
            return ofType(float.class, true, value);
        }

        public @NonNull
        HackedField<C, Double> fallbackTo(final double value) { //noinspection ConstantConditions
            return ofType(double.class, true, value);
        }

        /**
         * Fallback to the given value if this field is unavailable at runtime
         */
        public @NonNull
        <T> HackedField<C, T> fallbackTo(final T value) {
            @SuppressWarnings("unchecked") final Class<T> type = value == null ? null : (Class<T>) value.getClass();
            //noinspection ConstantConditions
            return ofType(type, true, value);
        }

        private <T> HackedField<C, T> ofType(final Class<T> type, final boolean fallback, final T fallback_value) {
            final Field field = findField(type);
            return field != null ? new HackedFieldImpl<C, T>(field) : fallback ? new FallbackField<C, T>(type, fallback_value) : null;
        }
    }

    public static class StaticFieldToHack<C> extends FieldToHack<C> {

        /**
         * @param modifiers the modifiers this field must have
         */
        private StaticFieldToHack(final Class<C> clazz, final String name, final int modifiers) {
            super(clazz, name, modifiers);
        }

        /**
         * Assert the field type.
         */
        public @Nullable
        <T> HackedTargetField<T> ofType(final Class<T> type) {
            return ofType(type, false, null);
        }

        public @Nullable
        <T> HackedTargetField<T> ofType(final String type_name) {
            try { //noinspection unchecked
                return ofType((Class<T>) Class.forName(type_name, false, mClass.getClassLoader()));
            } catch (final ClassNotFoundException e) {
                fail(new AssertionException(e));
                return null;
            }
        }

        /**
         * Fallback to the given value if this field is unavailable at runtime
         */
        public @NonNull
        <T> HackedTargetField<T> fallbackTo(final T value) {
            @SuppressWarnings("unchecked") final Class<T> type = value == null ? null : (Class<T>) value.getClass();
            //noinspection ConstantConditions
            return ofType(type, true, value);
        }

        private <T> HackedTargetField<T> ofType(final Class<T> type, final boolean fallback, final T fallback_value) {
            final Field field = findField(type);
            return field != null ? new HackedFieldImpl<C, T>(field).onTarget(null) : fallback ? new FallbackField<C, T>(type, fallback_value) : null;
        }
    }

    private static class HackedFieldImpl<C, T> implements HackedField<C, T> {

        private final @NonNull
        Field mField;

        HackedFieldImpl(final @NonNull Field field) {
            mField = field;
        }

        @Override
        public HackedTargetFieldImpl<T> on(final C target) {
            if (target == null) throw new IllegalArgumentException("target is null");
            return onTarget(target);
        }

        private HackedTargetFieldImpl<T> onTarget(final @Nullable C target) {
            return new HackedTargetFieldImpl<>(mField, target);
        }

        /**
         * Get current value of this field
         */
        @Override
        public T get(final C instance) {
            try {
                @SuppressWarnings("unchecked") final T value = (T) mField.get(instance);
                return value;
            } catch (final IllegalAccessException e) {
                return null;
            }    // Should never happen
        }

        /**
         * Set value of this field
         *
         * <p>No type enforced here since most type mismatch can be easily tested and exposed early.</p>
         */
        @Override
        public void set(final C instance, final T value) {
            try {
                mField.set(instance, value);
            } catch (final IllegalAccessException ignored) {
            }    // Should never happen
        }

        @Override
        @SuppressWarnings("unchecked")
        public @Nullable
        Class<T> getType() {
            return (Class<T>) mField.getType();
        }

        @Override
        public boolean isAbsent() {
            return false;
        }

        public @Nullable
        Field getField() {
            return mField;
        }
    }

    private static class FallbackField<C, T> implements HackedField<C, T>, HackedTargetField<T> {

        private final Class<T> mType;
        private final T mValue;

        private FallbackField(final Class<T> type, final T value) {
            mType = type;
            mValue = value;
        }

        @Override
        public T get(final C instance) {
            return mValue;
        }

        @Override
        public void set(final C instance, final T value) {
        }

        @Override
        public T get() {
            return mValue;
        }

        @Override
        public void set(final T value) {
        }

        @Override
        public HackedTargetField<T> on(final C target) {
            return this;
        }

        @Override
        public Class<T> getType() {
            return mType;
        }

        @Override
        public boolean isAbsent() {
            return true;
        }
    }

    public static class HackedTargetFieldImpl<T> implements HackedTargetField<T> {

        private final Field mField;
        private final Object mInstance;        // Instance type is already checked
        private @Nullable
        T mFallbackValue;

        HackedTargetFieldImpl(final Field field, final @Nullable Object instance) {
            mField = field;
            mInstance = instance;
        }

        @Override
        public T get() {
            if (mField == null) return mFallbackValue;
            try {
                @SuppressWarnings("unchecked") final T value = (T) mField.get(mInstance);
                return value;
            } catch (final IllegalAccessException e) {
                return null;
            }    // Should never happen
        }

        @Override
        public void set(final T value) {
            if (mField != null) try {
                mField.set(mInstance, value);
            } catch (final IllegalAccessException ignored) {
            }            // Should never happen
        }

        @Override
        @SuppressWarnings("unchecked")
        public @Nullable
        Class<T> getType() {
            return (Class<T>) mField.getType();
        }

        @Override
        public boolean isAbsent() {
            return mField == null;
        }
    }

    public static class CheckedHackedMethod<R, C, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable> {

        private final Invokable mInvokable;

        CheckedHackedMethod(final Invokable invokable) {
            mInvokable = invokable;
        }

        @SuppressWarnings("unchecked")
        public Class<R> getReturnType() {
            return (Class<R>) mInvokable.getReturnType();
        }

        protected HackInvocation<R, C, T1, T2, T3> invoke(final Object... args) {
            return new HackInvocation<>(mInvokable, args);
        }

        /**
         * Whether this hack is absent, thus will be fallen-back when invoked
         */
        public boolean isAbsent() {
            return mInvokable instanceof FallbackInvokable;
        }
    }

    public static class HackedMethod0<R, C, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable> extends CheckedHackedMethod<R, C, T1, T2, T3> {
        HackedMethod0(final Invokable invokable) {
            super(invokable);
        }

        public @CheckResult
        HackInvocation<R, C, T1, T2, T3> invoke() {
            return super.invoke();
        }
    }

    public static class HackedMethod1<R, C, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable, A1> extends CheckedHackedMethod<R, C, T1, T2, T3> {
        HackedMethod1(final Invokable invokable) {
            super(invokable);
        }

        public @CheckResult
        HackInvocation<R, C, T1, T2, T3> invoke(final A1 arg) {
            return super.invoke(arg);
        }
    }

    public static class HackedMethod2<R, C, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable, A1, A2> extends CheckedHackedMethod<R, C, T1, T2, T3> {
        HackedMethod2(final Invokable invokable) {
            super(invokable);
        }

        public @CheckResult
        HackInvocation<R, C, T1, T2, T3> invoke(final A1 arg1, final A2 arg2) {
            return super.invoke(arg1, arg2);
        }
    }

    public static class HackedMethod3<R, C, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable, A1, A2, A3> extends CheckedHackedMethod<R, C, T1, T2, T3> {
        HackedMethod3(final Invokable invokable) {
            super(invokable);
        }

        public @CheckResult
        HackInvocation<R, C, T1, T2, T3> invoke(final A1 arg1, final A2 arg2, final A3 arg3) {
            return super.invoke(arg1, arg2, arg3);
        }
    }

    public static class HackedMethod4<R, C, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable, A1, A2, A3, A4> extends CheckedHackedMethod<R, C, T1, T2, T3> {
        HackedMethod4(final Invokable invokable) {
            super(invokable);
        }

        public @CheckResult
        HackInvocation<R, C, T1, T2, T3> invoke(final A1 arg1, final A2 arg2, final A3 arg3, final A4 arg4) {
            return super.invoke(arg1, arg2, arg3, arg4);
        }
    }

    public static class HackedMethod5<R, C, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable, A1, A2, A3, A4, A5> extends CheckedHackedMethod<R, C, T1, T2, T3> {
        HackedMethod5(final Invokable invokable) {
            super(invokable);
        }

        public @CheckResult
        HackInvocation<R, C, T1, T2, T3> invoke(final A1 arg1, final A2 arg2, final A3 arg3, final A4 arg4, final A5 arg5) {
            return super.invoke(arg1, arg2, arg3, arg4, arg5);
        }
    }

    public static class HackedMethodN<R, C, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable> extends CheckedHackedMethod<R, C, T1, T2, T3> {
        HackedMethodN(final Invokable invokable) {
            super(invokable);
        }

        public @CheckResult
        HackInvocation<R, C, T1, T2, T3> invoke(final Object... args) {
            return super.invoke(args);
        }
    }

    public static class HackInvocation<R, C, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable> {

        private final Invokable invokable;
        private final Object[] args;

        HackInvocation(final Invokable invokable, final Object... args) {
            this.invokable = invokable;
            this.args = args;
        }

        public R on(final @NonNull C target) throws T1, T2, T3 {
            return onTarget(target);
        }

        public R statically() throws T1, T2, T3 {
            return onTarget(null);
        }

        private R onTarget(final C target) throws T1 { //noinspection TryWithIdenticalCatches
            try {
                @SuppressWarnings("unchecked") final R result = (R) invokable.invoke(target, args);
                return result;
            } catch (final IllegalAccessException e) {
                throw new RuntimeException(e);    // Should never happen
            } catch (final InstantiationException e) {
                throw new RuntimeException(e);
            } catch (final InvocationTargetException e) {
                final Throwable ex = e.getTargetException();
                //noinspection unchecked
                throw (T1) ex;
            }
        }
    }

    private static class HackedMethodImpl<R, C, T1 extends Throwable, T2 extends Throwable, T3 extends Throwable> implements NonNullHackedMethod<R, C, T1, T2, T3> {

        private static final Comparator<Class> CLASS_COMPARATOR = new Comparator<Class>() {
            @Override
            public int compare(final Class lhs, final Class rhs) {
                return lhs.toString().compareTo(rhs.toString());
            }

            @Override
            public boolean equals(final Object object) {
                return this == object;
            }
        };
        private final Class<C> mClass;
        private final @Nullable
        String mName;        // Null for constructor
        private final int mModifiers;
        private Class<?> mReturnType;
        private Class<?>[] mThrowTypes;
        private R mFallbackReturnValue;
        private boolean mHasFallback;

        HackedMethodImpl(final Class<?> clazz, @Nullable final String name, final int modifiers) {
            //noinspection unchecked, to be compatible with HackedClass.staticMethod()
            mClass = (Class<C>) clazz;
            mName = name;
            mModifiers = modifiers;
        }

        @Override
        public <RR> HackedMethod<RR, C, T1, T2, T3> returning(final Class<RR> type) {
            mReturnType = type;
            @SuppressWarnings("unchecked") final HackedMethod<RR, C, T1, T2, T3> casted = (HackedMethod<RR, C, T1, T2, T3>) this;
            return casted;
        }

        @Override
        public NonNullHackedMethod<R, C, T1, T2, T3> fallbackReturning(final R value) {
            mFallbackReturnValue = value;
            mHasFallback = true;
            return this;
        }

        @Override
        public <TT extends Throwable> NonNullHackedMethod<R, C, TT, T2, T3> throwing(final Class<TT> type) {
            mThrowTypes = new Class[]{type};
            @SuppressWarnings("unchecked") final NonNullHackedMethod<R, C, TT, T2, T3> casted = (NonNullHackedMethod<R, C, TT, T2, T3>) this;
            return casted;
        }

        @Override
        public <TT1 extends Throwable, TT2 extends Throwable> NonNullHackedMethod<R, C, TT1, TT2, T3>
        throwing(final Class<TT1> type1, final Class<TT2> type2) {
            mThrowTypes = new Class<?>[]{type1, type2};
            Arrays.sort(mThrowTypes, CLASS_COMPARATOR);
            @SuppressWarnings("unchecked") final NonNullHackedMethod<R, C, TT1, TT2, T3> cast = (NonNullHackedMethod<R, C, TT1, TT2, T3>) this;
            return cast;
        }

        @Override
        public <TT1 extends Throwable, TT2 extends Throwable, TT3 extends Throwable> NonNullHackedMethod<R, C, TT1, TT2, TT3>
        throwing(final Class<TT1> type1, final Class<TT2> type2, final Class<TT3> type3) {
            mThrowTypes = new Class<?>[]{type1, type2, type3};
            Arrays.sort(mThrowTypes, CLASS_COMPARATOR);
            @SuppressWarnings("unchecked") final NonNullHackedMethod<R, C, TT1, TT2, TT3> cast = (NonNullHackedMethod<R, C, TT1, TT2, TT3>) this;
            return cast;
        }

        @Override
        public HackedMethod<R, C, Exception, T2, T3> throwing(final Class<?>... types) {
            mThrowTypes = types;
            Arrays.sort(mThrowTypes, CLASS_COMPARATOR);
            @SuppressWarnings("unchecked") final HackedMethod<R, C, Exception, T2, T3> cast = (HackedMethod<R, C, Exception, T2, T3>) this;
            return cast;
        }

        @NonNull
        @SuppressWarnings("ConstantConditions")
        @Override
        public HackedMethod0<R, C, T1, T2, T3> withoutParams() {
            final Invokable<C> invokable = findInvokable();
            return invokable == null ? null : new HackedMethod0<R, C, T1, T2, T3>(invokable);
        }

        @NonNull
        @SuppressWarnings("ConstantConditions")
        @Override
        public <A1> HackedMethod1<R, C, T1, T2, T3, A1> withParam(final Class<A1> type) {
            final Invokable invokable = findInvokable(type);
            return invokable == null ? null : new HackedMethod1<R, C, T1, T2, T3, A1>(invokable);
        }

        @NonNull
        @SuppressWarnings("ConstantConditions")
        @Override
        public <A1, A2> HackedMethod2<R, C, T1, T2, T3, A1, A2> withParams(final Class<A1> type1, final Class<A2> type2) {
            final Invokable invokable = findInvokable(type1, type2);
            return invokable == null ? null : new HackedMethod2<R, C, T1, T2, T3, A1, A2>(invokable);
        }

        @NonNull
        @SuppressWarnings("ConstantConditions")
        @Override
        public <A1, A2, A3> HackedMethod3<R, C, T1, T2, T3, A1, A2, A3> withParams(final Class<A1> type1, final Class<A2> type2, final Class<A3> type3) {
            final Invokable invokable = findInvokable(type1, type2, type3);
            return invokable == null ? null : new HackedMethod3<R, C, T1, T2, T3, A1, A2, A3>(invokable);
        }

        @NonNull
        @SuppressWarnings("ConstantConditions")
        @Override
        public <A1, A2, A3, A4> HackedMethod4<R, C, T1, T2, T3, A1, A2, A3, A4> withParams(final Class<A1> type1, final Class<A2> type2, final Class<A3> type3, final Class<A4> type4) {
            final Invokable invokable = findInvokable(type1, type2, type3, type4);
            return invokable == null ? null : new HackedMethod4<R, C, T1, T2, T3, A1, A2, A3, A4>(invokable);
        }

        @NonNull
        @SuppressWarnings("ConstantConditions")
        @Override
        public <A1, A2, A3, A4, A5> HackedMethod5<R, C, T1, T2, T3, A1, A2, A3, A4, A5> withParams(final Class<A1> type1, final Class<A2> type2, final Class<A3> type3, final Class<A4> type4, final Class<A5> type5) {
            final Invokable invokable = findInvokable(type1, type2, type3, type4, type5);
            return invokable == null ? null : new HackedMethod5<R, C, T1, T2, T3, A1, A2, A3, A4, A5>(invokable);
        }

        @NonNull
        @SuppressWarnings("ConstantConditions")
        @Override
        public HackedMethodN<R, C, T1, T2, T3> withParams(final Class<?>... types) {
            final Invokable invokable = findInvokable(types);
            return invokable == null ? null : new HackedMethodN<R, C, T1, T2, T3>(invokable);
        }

        private @Nullable
        Invokable<C> findInvokable(final Class<?>... param_types) {
            if (mClass == ANY_TYPE)        // AnyType as a internal indicator for class not found.
                return mHasFallback ? new FallbackInvokable<C>(mFallbackReturnValue) : null;

            final int modifiers;
            Invokable<C> invokable;
            final AccessibleObject accessible;
            final Class<?>[] ex_types;
            try {
                if (mName != null) {
                    final Method candidate = mClass.getDeclaredMethod(mName, param_types);
                    Method method = candidate;
                    ex_types = candidate.getExceptionTypes();
                    modifiers = method.getModifiers();
                    if (Modifier.isStatic(mModifiers) != Modifier.isStatic(candidate.getModifiers())) {
                        fail(new AssertionException(candidate + (Modifier.isStatic(mModifiers) ? " is not static" : "is static")).setHackedMethod(method));
                        method = null;
                    }
                    if (mReturnType != null && mReturnType != ANY_TYPE && !candidate.getReturnType().equals(mReturnType)) {
                        fail(new AssertionException("Return type mismatch: " + candidate));
                        method = null;
                    }
                    if (method != null) {
                        invokable = new InvokableMethod<>(method);
                        accessible = method;
                    } else {
                        invokable = null;
                        accessible = null;
                    }
                } else {
                    final Constructor<C> ctor = mClass.getDeclaredConstructor(param_types);
                    modifiers = ctor.getModifiers();
                    invokable = new InvokableConstructor<>(ctor);
                    accessible = ctor;
                    ex_types = ctor.getExceptionTypes();
                }
            } catch (final NoSuchMethodException e) {
                fail(new AssertionException(e).setHackedClass(mClass).setHackedMethodName(mName).setParamTypes(param_types));
                return mHasFallback ? new FallbackInvokable<C>(mFallbackReturnValue) : null;
            }

            if (mModifiers > 0 && (modifiers & mModifiers) != mModifiers)
                fail(new AssertionException(invokable + " does not match modifiers: " + mModifiers).setHackedMethodName(mName));

            if (mThrowTypes == null && ex_types.length > 0 || mThrowTypes != null && ex_types.length == 0) {
                fail(new AssertionException("Checked exception(s) not match: " + invokable));
                if (ex_types.length > 0)
                    invokable = null;        // No need to fall-back if expected checked exceptions are absent.
            } else if (mThrowTypes != null) {
                Arrays.sort(ex_types, CLASS_COMPARATOR);
                if (!Arrays.equals(ex_types, mThrowTypes)) {    // TODO: Check derived relation of exceptions
                    fail(new AssertionException("Checked exception(s) not match: " + invokable));
                    invokable = null;
                }
            }

            if (invokable == null) {
                if (!mHasFallback) return null;
                return new FallbackInvokable<>(mFallbackReturnValue);
            }

            if (!accessible.isAccessible()) accessible.setAccessible(true);
            return invokable;
        }
    }

    private static class InvokableMethod<C> implements Invokable<C> {

        private final Method method;

        InvokableMethod(final Method method) {
            this.method = method;
        }

        public Object invoke(final C target, final Object[] args) throws IllegalAccessException,
                IllegalArgumentException, InvocationTargetException {
            return method.invoke(target, args);
        }

        @Override
        public Class<?> getReturnType() {
            return method.getReturnType();
        }

        @Override
        public String toString() {
            return method.toString();
        }
    }

    private static class InvokableConstructor<C> implements Invokable<C> {

        private final Constructor<C> constructor;

        InvokableConstructor(final Constructor<C> method) {
            this.constructor = method;
        }

        public Object invoke(final C target, final Object[] args) throws InstantiationException,
                IllegalAccessException, IllegalArgumentException, InvocationTargetException {
            return constructor.newInstance(args);
        }

        @Override
        public Class<?> getReturnType() {
            return constructor.getDeclaringClass();
        }

        @Override
        public String toString() {
            return constructor.toString();
        }
    }

    private static class FallbackInvokable<C> implements Invokable<C> {

        private final @Nullable
        Object mValue;

        FallbackInvokable(final @Nullable Object value) {
            mValue = value;
        }

        @Override
        public Object invoke(final C target, final Object[] args) throws InvocationTargetException, IllegalAccessException, InstantiationException {
            return mValue;
        }

        @Override
        public Class<?> getReturnType() {
            return mValue == null ? Object.class : mValue.getClass();
        }
    }

    public static class HackedClass<C> {

        private final Class<C> mClass;

        HackedClass(final Class<C> clazz) {
            mClass = clazz;
        }

        public @CheckResult
        <T> MemberFieldToHack<C> field(final @NonNull String name) {
            return new MemberFieldToHack<>(mClass, name, 0);
        }

        public @CheckResult
        <T> StaticFieldToHack<C> staticField(final @NonNull String name) {
            return new StaticFieldToHack<>(mClass, name, Modifier.STATIC);
        }

        public @CheckResult
        NonNullHackedMethod<Void, C, Unchecked, Unchecked, Unchecked> method(final String name) {
            return new HackedMethodImpl<>(mClass, name, 0);
        }

        public @CheckResult
        NonNullHackedMethod<Void, Void, Unchecked, Unchecked, Unchecked> staticMethod(final String name) {
            return new HackedMethodImpl<>(mClass, name, Modifier.STATIC);
        }

        public @CheckResult
        NonNullHackedInvokable<C, Void, Unchecked, Unchecked, Unchecked> constructor() {
            final HackedMethodImpl<C, Void, Unchecked, Unchecked, Unchecked> constructor = new HackedMethodImpl<>(mClass, null, 0);
            constructor.fallbackReturning(null);    // Always fallback to null.
            return constructor;
        }
    }

    /**
     * This is a simple demo for the common usage of {@link Hack}
     */
    @SuppressWarnings("unused")
    private static class Demo {

        static String sField;
        boolean mField;

        Demo(final int flags) {
        }

        static void demo() {
            final Demo demo = Hacks.Demo_ctor.invoke(0).statically();
            try {
                Hacks.Demo_methodThrows.invoke().on(demo);
            } catch (final InterruptedException | IOException e) {    // The checked exceptions declared by throwing() in hack definition.
                e.printStackTrace();
            }
            Hacks.Demo_staticMethod.invoke(1, "xx").statically();
        }

        static boolean staticMethod(final int a, final String c) {
            return false;
        }

        private void methodThrows() throws InterruptedException, IOException {
        }

        @SuppressWarnings({"FieldCanBeLocal", "UnnecessarilyQualifiedStaticUsage"})
        static class Hacks {

            static HackedMethod1<Demo, Void, Unchecked, Unchecked, Unchecked, Integer> Demo_ctor;
            static HackedMethod0<Void, Demo, InterruptedException, IOException, Unchecked> Demo_methodThrows;
            static HackedMethod2<Boolean, Void, Unchecked, Unchecked, Unchecked, Integer, String> Demo_staticMethod;
            static @Nullable
            HackedField<Demo, Boolean> Demo_mField;        // Optional hack may be null if assertion failed
            static @Nullable
            HackedTargetField<String> Demo_sField;

            static {
                Hack.setAssertionFailureHandler(new AssertionFailureHandler() {
                    @Override
                    public void onAssertionFailure(final AssertionException failure) {
                        Log.w("Demo", "Partially incompatible: " + failure.getDebugInfo());
                        // Report the incompatibility silently.
                        //...
                    }
                });
                Demo_ctor = Hack.into(Demo.class).constructor().withParam(int.class);
                // Method without fallback (will be null if absent)
                Demo_methodThrows = Hack.into(Demo.class).method("methodThrows").returning(Void.class).fallbackReturning(null)
                        .throwing(InterruptedException.class, IOException.class).withoutParams();
                // Method with fallback (will never be null)
                Demo_staticMethod = Hack.into(Demo.class).staticMethod("methodWith2Params").returning(boolean.class)
                        .fallbackReturning(false).withParams(int.class, String.class);
                Demo_mField = Hack.into(Demo.class).field("mField").fallbackTo(false);
                Demo_sField = Hack.into(Demo.class).staticField("sField").ofType(String.class);
            }
        }
    }

    /**
     * Placeholder for unchecked exception
     */
    public class Unchecked extends RuntimeException {
    }
}
