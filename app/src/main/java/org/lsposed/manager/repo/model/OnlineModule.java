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

public class OnlineModule implements Serializable, Parcelable {

    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("description")
    @Expose
    private String description;
    @SerializedName("url")
    @Expose
    private String url;
    @SerializedName("homepageUrl")
    @Expose
    private String homepageUrl;
    @SerializedName("collaborators")
    @Expose
    private List<Collaborator> collaborators = new ArrayList<>();
    @SerializedName("releases")
    @Expose
    private List<Release> releases = new ArrayList<>();
    @SerializedName("readme")
    @Expose
    private String readme;
    @SerializedName("summary")
    @Expose
    private String summary;
    @SerializedName("scope")
    @Expose
    private List<String> scope = new ArrayList<>();
    @SerializedName("sourceUrl")
    @Expose
    private String sourceUrl;
    @SerializedName("hide")
    @Expose
    private Boolean hide;
    @SerializedName("additionalAuthors")
    @Expose
    private List<Object> additionalAuthors = null;
    @SerializedName("updatedAt")
    @Expose
    private String updatedAt;
    @SerializedName("createdAt")
    @Expose
    private String createdAt;
    @SerializedName("stargazerCount")
    @Expose
    private Integer stargazerCount;
    public boolean releasesLoaded = false;
    public final static Creator<OnlineModule> CREATOR = new Creator<OnlineModule>() {

        public OnlineModule createFromParcel(Parcel in) {
            return new OnlineModule(in);
        }

        public OnlineModule[] newArray(int size) {
            return (new OnlineModule[size]);
        }

    };
    private final static long serialVersionUID = 3372849627722130087L;

    protected OnlineModule(Parcel in) {
        this.name = ((String) in.readValue((String.class.getClassLoader())));
        this.description = ((String) in.readValue((String.class.getClassLoader())));
        this.url = ((String) in.readValue((String.class.getClassLoader())));
        this.homepageUrl = ((String) in.readValue((String.class.getClassLoader())));
        in.readList(this.collaborators, (Collaborator.class.getClassLoader()));
        in.readList(this.releases, (Release.class.getClassLoader()));
        this.readme = ((String) in.readValue((String.class.getClassLoader())));
        this.summary = ((String) in.readValue((String.class.getClassLoader())));
        in.readList(this.scope, (String.class.getClassLoader()));
        this.sourceUrl = ((String) in.readValue((String.class.getClassLoader())));
        this.hide = ((Boolean) in.readValue((Boolean.class.getClassLoader())));
        in.readList(this.additionalAuthors, (Object.class.getClassLoader()));
        this.updatedAt = ((String) in.readValue((String.class.getClassLoader())));
        this.createdAt = ((String) in.readValue((String.class.getClassLoader())));
        this.stargazerCount = ((Integer) in.readValue((Integer.class.getClassLoader())));
    }

    public OnlineModule() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getHomepageUrl() {
        return homepageUrl;
    }

    public void setHomepageUrl(String homepageUrl) {
        this.homepageUrl = homepageUrl;
    }

    public List<Collaborator> getCollaborators() {
        return collaborators;
    }

    public void setCollaborators(List<Collaborator> collaborators) {
        this.collaborators = collaborators;
    }

    public List<Release> getReleases() {
        return releases;
    }

    public void setReleases(List<Release> releases) {
        this.releases = releases;
    }

    public String getReadme() {
        return readme;
    }

    public void setReadme(String readme) {
        this.readme = readme;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getScope() {
        return scope;
    }

    public void setScope(List<String> scope) {
        this.scope = scope;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public Boolean isHide() {
        return hide;
    }

    public void setHide(Boolean hide) {
        this.hide = hide;
    }

    public List<Object> getAdditionalAuthors() {
        return additionalAuthors;
    }

    public void setAdditionalAuthors(List<Object> additionalAuthors) {
        this.additionalAuthors = additionalAuthors;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getStargazerCount() {
        return stargazerCount;
    }

    public void setStargazerCount(Integer stargazerCount) {
        this.stargazerCount = stargazerCount;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(name);
        dest.writeValue(description);
        dest.writeValue(url);
        dest.writeValue(homepageUrl);
        dest.writeList(collaborators);
        dest.writeList(releases);
        dest.writeValue(readme);
        dest.writeValue(summary);
        dest.writeList(scope);
        dest.writeValue(sourceUrl);
        dest.writeValue(hide);
        dest.writeList(additionalAuthors);
        dest.writeValue(updatedAt);
        dest.writeValue(createdAt);
        dest.writeValue(stargazerCount);
    }

    public int describeContents() {
        return 0;
    }

}
