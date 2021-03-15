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
import java.util.ArrayList;
import java.util.List;

public class Release implements Serializable, Parcelable {

    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("url")
    @Expose
    private String url;
    @SerializedName("description")
    @Expose
    private String description;
    @SerializedName("descriptionHTML")
    @Expose
    private String descriptionHTML;
    @SerializedName("createdAt")
    @Expose
    private String createdAt;
    @SerializedName("publishedAt")
    @Expose
    private String publishedAt;
    @SerializedName("updatedAt")
    @Expose
    private String updatedAt;
    @SerializedName("tagName")
    @Expose
    private String tagName;
    @SerializedName("isPrerelease")
    @Expose
    private Boolean isPrerelease;
    @SerializedName("releaseAssets")
    @Expose
    private List<ReleaseAsset> releaseAssets = new ArrayList<>();
    public final static Creator<Release> CREATOR = new Creator<Release>() {

        public Release createFromParcel(Parcel in) {
            return new Release(in);
        }

        public Release[] newArray(int size) {
            return (new Release[size]);
        }

    };
    private final static long serialVersionUID = 1047772731795034659L;

    protected Release(Parcel in) {
        this.name = ((String) in.readValue((String.class.getClassLoader())));
        this.url = ((String) in.readValue((String.class.getClassLoader())));
        this.description = ((String) in.readValue((String.class.getClassLoader())));
        this.descriptionHTML = ((String) in.readValue((String.class.getClassLoader())));
        this.createdAt = ((String) in.readValue((String.class.getClassLoader())));
        this.publishedAt = ((String) in.readValue((String.class.getClassLoader())));
        this.updatedAt = ((String) in.readValue((String.class.getClassLoader())));
        this.tagName = ((String) in.readValue((String.class.getClassLoader())));
        this.isPrerelease = ((Boolean) in.readValue((Boolean.class.getClassLoader())));
        in.readList(this.releaseAssets, (ReleaseAsset.class.getClassLoader()));
    }

    public Release() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescriptionHTML() {
        return descriptionHTML;
    }

    public void setDescriptionHTML(String descriptionHTML) {
        this.descriptionHTML = descriptionHTML;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(String publishedAt) {
        this.publishedAt = publishedAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public Boolean getIsPrerelease() {
        return isPrerelease;
    }

    public void setIsPrerelease(Boolean isPrerelease) {
        this.isPrerelease = isPrerelease;
    }

    public List<ReleaseAsset> getReleaseAssets() {
        return releaseAssets;
    }

    public void setReleaseAssets(List<ReleaseAsset> releaseAssets) {
        this.releaseAssets = releaseAssets;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(name);
        dest.writeValue(url);
        dest.writeValue(description);
        dest.writeValue(descriptionHTML);
        dest.writeValue(createdAt);
        dest.writeValue(publishedAt);
        dest.writeValue(updatedAt);
        dest.writeValue(tagName);
        dest.writeValue(isPrerelease);
        dest.writeList(releaseAssets);
    }

    public int describeContents() {
        return 0;
    }

}
