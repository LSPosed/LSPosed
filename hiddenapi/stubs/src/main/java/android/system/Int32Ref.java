package android.system;

import java.util.Objects;

public class Int32Ref {
    public int value;

    public Int32Ref(int value) {
        this.value = value;
    }

    @Override public String toString() {
        return Objects.toString(this);
    }
}
