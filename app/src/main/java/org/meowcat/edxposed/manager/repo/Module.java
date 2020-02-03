package org.meowcat.edxposed.manager.repo;

import android.util.Pair;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Module {
    @SuppressWarnings("WeakerAccess")
    public final Repository repository;
    public final List<Pair<String, String>> moreInfo = new LinkedList<>();
    public final List<ModuleVersion> versions = new ArrayList<>();
    final List<String> screenshots = new ArrayList<>();
    public String packageName;
    public String name;
    public String summary;
    public String description;
    public boolean descriptionIsHtml = false;
    public String author;
    public String support;
    long created = -1;
    long updated = -1;

    Module(Repository repository) {
        this.repository = repository;
    }
}
