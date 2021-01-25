package org.meowcat.edxposed.manager.util.json;


import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
public class XposedTab implements Parcelable {

    public static final Creator<XposedTab> CREATOR = new Creator<XposedTab>() {
        @Override
        public XposedTab createFromParcel(Parcel in) {
            return new XposedTab(in);
        }

        @Override
        public XposedTab[] newArray(int size) {
            return new XposedTab[size];
        }
    };

    public List<Integer> sdks = new ArrayList<>();
    public String name;
    public String author;
    public String description;
    public String support;
    public String notice;
    public boolean stable;
    public boolean official;
    public List<XposedZip> installers = new ArrayList<>();
    public List<XposedZip> uninstallers = new ArrayList<>();
//    private HashMap<String, String> compatibility = new HashMap<>();
//    private HashMap<String, String> incompatibility = new HashMap<>();

//    public XposedTab() {
//    }

    private XposedTab(Parcel in) {
        name = in.readString();
        author = in.readString();
        description = in.readString();
        support = in.readString();
        notice = in.readString();
        stable = in.readByte() != 0;
        official = in.readByte() != 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(author);
        dest.writeString(description);
        dest.writeString(support);
        dest.writeString(notice);
        dest.writeByte((byte) (stable ? 1 : 0));
        dest.writeByte((byte) (official ? 1 : 0));
    }
}