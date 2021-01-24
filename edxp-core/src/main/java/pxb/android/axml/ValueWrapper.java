package pxb.android.axml;

public class ValueWrapper {

    public static final int ID = 1;
    public static final int STYLE = 2;
    public static final int CLASS = 3;
    public final int type;
    public final String raw;
    public final int ref;

    private ValueWrapper(int type, int ref, String raw) {
        super();
        this.type = type;
        this.raw = raw;
        this.ref = ref;
    }

    public ValueWrapper replaceRaw(String raw) {
        return new ValueWrapper(type, ref, raw);
    }

    public static ValueWrapper wrapId(int ref, String raw) {
        return new ValueWrapper(ID, ref, raw);
    }

    public static ValueWrapper wrapStyle(int ref, String raw) {
        return new ValueWrapper(STYLE, ref, raw);
    }

    public static ValueWrapper wrapClass(int ref, String raw) {
        return new ValueWrapper(CLASS, ref, raw);
    }
}
