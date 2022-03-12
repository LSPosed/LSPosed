package android.os;

public interface Parcelable {
    interface Creator<T>{
        public T createFromParcel(Parcel source);
        public T[] newArray(int size);
    }
    void writeToParcel(Parcel dest, int flags);
    int describeContents();
}
