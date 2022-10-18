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

import androidx.annotation.Nullable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class OnlineModule {

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
    @SerializedName("latestRelease")
    @Expose
    private String latestRelease;
    @SerializedName("latestReleaseTime")
    @Expose
    private String latestReleaseTime;
    @SerializedName("latestBetaRelease")
    @Expose
    private String latestBetaRelease;
    @SerializedName("latestBetaReleaseTime")
    @Expose
    private String latestBetaReleaseTime;
    @SerializedName("latestSnapshotRelease")
    @Expose
    private String latestSnapshotRelease;
    @SerializedName("latestSnapshotReleaseTime")
    @Expose
    private String latestSnapshotReleaseTime;
    @SerializedName("releases")
    @Expose
    private List<Release> releases = new ArrayList<>();
    @SerializedName("betaReleases")
    @Expose
    private final List<Release> betaReleases = new ArrayList<>();
    @SerializedName("snapshotReleases")
    @Expose
    private final List<Release> snapshotReleases = new ArrayList<>();
    @SerializedName("readme")
    @Expose
    private String readme;
    @SerializedName("readmeHTML")
    @Expose
    private String readmeHTML;
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

    @Nullable
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Nullable
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Nullable
    public String getHomepageUrl() {
        return homepageUrl;
    }

    public void setHomepageUrl(String homepageUrl) {
        this.homepageUrl = homepageUrl;
    }

    @Nullable
    public List<Collaborator> getCollaborators() {
        return collaborators;
    }

    public void setCollaborators(List<Collaborator> collaborators) {
        this.collaborators = collaborators;
    }

    @Nullable
    public List<Release> getReleases() {
        return releases;
    }

    @Nullable
    public String getLatestReleaseTime() {
        return latestReleaseTime;
    }

    public void setReleases(List<Release> releases) {
        this.releases = releases;
    }

    @Nullable
    public String getReadme() {
        return readme;
    }

    public void setReadme(String readme) {
        this.readme = readme;
    }

    @Nullable
    public String getReadmeHTML() {
        return readmeHTML;
    }

    public void setReadmeHTML(String readmeHTML) {
        this.readmeHTML = readmeHTML;
    }

    @Nullable
    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    @Nullable
    public List<String> getScope() {
        return scope;
    }

    public void setScope(List<String> scope) {
        this.scope = scope;
    }

    @Nullable
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

    @Nullable
    public List<Object> getAdditionalAuthors() {
        return additionalAuthors;
    }

    public void setAdditionalAuthors(List<Object> additionalAuthors) {
        this.additionalAuthors = additionalAuthors;
    }

    @Nullable
    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Nullable
    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Nullable
    public Integer getStargazerCount() {
        return stargazerCount;
    }

    public void setStargazerCount(Integer stargazerCount) {
        this.stargazerCount = stargazerCount;
    }

    @Nullable
    public String getLatestRelease() {
        return latestRelease;
    }

    public void setLatestRelease(String latestRelease) {
        this.latestRelease = latestRelease;
    }

    @Nullable
    public String getLatestBetaRelease() {
        return latestBetaRelease;
    }

    @Nullable
    public String getLatestBetaReleaseTime() {
        return latestBetaReleaseTime;
    }

    @Nullable
    public String getLatestSnapshotRelease() {
        return latestSnapshotRelease;
    }

    @Nullable
    public String getLatestSnapshotReleaseTime() {
        return latestSnapshotReleaseTime;
    }

    @Nullable
    public List<Release> getBetaReleases() {
        return betaReleases;
    }

    @Nullable
    public List<Release> getSnapshotReleases() {
        return snapshotReleases;
    }
}
