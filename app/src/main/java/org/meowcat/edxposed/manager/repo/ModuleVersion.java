package org.meowcat.edxposed.manager.repo;

public class ModuleVersion {
    public final Module module;
    public String name;
    public int code;
    public String downloadLink;
    public String md5sum;
    public String changelog;
    public boolean changelogIsHtml = false;
    public ReleaseType relType = ReleaseType.STABLE;
    public long uploaded = -1;

    /* package */ ModuleVersion(Module module) {
        this.module = module;
    }
}
