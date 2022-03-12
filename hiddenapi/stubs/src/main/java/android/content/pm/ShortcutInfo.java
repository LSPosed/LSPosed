package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;

public class ShortcutInfo implements Parcelable {
    public static final Creator<ShortcutInfo> CREATOR = new Creator<>() {
        @Override
        public ShortcutInfo createFromParcel(Parcel in) {
            throw new IllegalArgumentException("STUB");
        }

        @Override
        public ShortcutInfo[] newArray(int size) {
            throw new IllegalArgumentException("STUB");
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        throw new IllegalArgumentException("STUB");
    }

    @Override
    public int describeContents() {
        throw new IllegalArgumentException("STUB");
    }
}
