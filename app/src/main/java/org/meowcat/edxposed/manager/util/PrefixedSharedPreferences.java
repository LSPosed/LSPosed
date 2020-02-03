package org.meowcat.edxposed.manager.util;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class PrefixedSharedPreferences implements SharedPreferences {
    private final SharedPreferences mBase;
    private final String mPrefix;

    public PrefixedSharedPreferences(SharedPreferences base, String prefix) {
        mBase = base;
        mPrefix = prefix + "_";
    }

    public static void injectToPreferenceManager(PreferenceManager manager, String prefix) {
        SharedPreferences prefixedPrefs = new PrefixedSharedPreferences(manager.getSharedPreferences(), prefix);

        try {
            Field fieldSharedPref = PreferenceManager.class.getDeclaredField("mSharedPreferences");
            fieldSharedPref.setAccessible(true);
            fieldSharedPref.set(manager, prefixedPrefs);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    public Map<String, ?> getAll() {
        Map<String, ?> baseResult = mBase.getAll();
        Map<String, Object> prefixedResult = new HashMap<String, Object>(baseResult);
        for (Entry<String, ?> entry : baseResult.entrySet()) {
            prefixedResult.put(mPrefix + entry.getKey(), entry.getValue());
        }
        return prefixedResult;
    }

    @Override
    public String getString(String key, String defValue) {
        return mBase.getString(mPrefix + key, defValue);
    }

    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
        return mBase.getStringSet(mPrefix + key, defValues);
    }

    @Override
    public int getInt(String key, int defValue) {
        return mBase.getInt(mPrefix + key, defValue);
    }

    @Override
    public long getLong(String key, long defValue) {
        return mBase.getLong(mPrefix + key, defValue);
    }

    @Override
    public float getFloat(String key, float defValue) {
        return mBase.getFloat(mPrefix + key, defValue);
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        return mBase.getBoolean(mPrefix + key, defValue);
    }

    @Override
    public boolean contains(String key) {
        return mBase.contains(mPrefix + key);
    }

    @SuppressLint("CommitPrefEdits")
    @Override
    public Editor edit() {
        return new EditorImpl(mBase.edit());
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        throw new UnsupportedOperationException("listeners are not supported in this implementation");
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        throw new UnsupportedOperationException("listeners are not supported in this implementation");
    }

    private class EditorImpl implements Editor {
        private final Editor mEditorBase;

        public EditorImpl(Editor base) {
            mEditorBase = base;
        }

        @Override
        public Editor putString(String key, String value) {
            mEditorBase.putString(mPrefix + key, value);
            return this;
        }

        @Override
        public Editor putStringSet(String key, Set<String> values) {
            mEditorBase.putStringSet(mPrefix + key, values);
            return this;
        }

        @Override
        public Editor putInt(String key, int value) {
            mEditorBase.putInt(mPrefix + key, value);
            return this;
        }

        @Override
        public Editor putLong(String key, long value) {
            mEditorBase.putLong(mPrefix + key, value);
            return this;
        }

        @Override
        public Editor putFloat(String key, float value) {
            mEditorBase.putFloat(mPrefix + key, value);
            return this;
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            mEditorBase.putBoolean(mPrefix + key, value);
            return this;
        }

        @Override
        public Editor remove(String key) {
            mEditorBase.remove(mPrefix + key);
            return this;
        }

        @Override
        public Editor clear() {
            mEditorBase.clear();
            return this;
        }

        @Override
        public boolean commit() {
            return mEditorBase.commit();
        }

        @Override
        public void apply() {
            mEditorBase.apply();
        }
    }
}