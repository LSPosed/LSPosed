/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2020 EdXposed Contributors
 * Copyright (C) 2021 LSPosed Contributors
 */

package org.lsposed.manager.repo.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class ReleaseAsset implements Serializable, Parcelable {

    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("contentType")
    @Expose
    private String contentType;
    @SerializedName("downloadUrl")
    @Expose
    private String downloadUrl;
    public final static Creator<ReleaseAsset> CREATOR = new Creator<ReleaseAsset>() {

        public ReleaseAsset createFromParcel(Parcel in) {
            return new ReleaseAsset(in);
        }

        public ReleaseAsset[] newArray(int size) {
            return (new ReleaseAsset[size]);
        }

    };
    private final static long serialVersionUID = -4273789818349239422L;

    protected ReleaseAsset(Parcel in) {
        this.name = ((String) in.readValue((String.class.getClassLoader())));
        this.contentType = ((String) in.readValue((String.class.getClassLoader())));
        this.downloadUrl = ((String) in.readValue((String.class.getClassLoader())));
    }

    public ReleaseAsset() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(name);
        dest.writeValue(contentType);
        dest.writeValue(downloadUrl);
    }

    public int describeContents() {
        return 0;
    }

}
