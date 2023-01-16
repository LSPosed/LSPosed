package android.os;

public class ShellCallback implements Parcelable {
    public static final Parcelable.Creator<ShellCallback> CREATOR = new Creator<ShellCallback>() {
        @Override
        public ShellCallback createFromParcel(Parcel source) {
            throw new IllegalArgumentException("STUB");
        }

        @Override
        public ShellCallback[] newArray(int size) {
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
