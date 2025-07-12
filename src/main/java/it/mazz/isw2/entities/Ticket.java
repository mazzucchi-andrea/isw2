package it.mazz.isw2.entities;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class Ticket {
    private final List<Commit> commits = new LinkedList<>();
    private String key;
    private Version openingVersion;
    private List<Version> affectedVersions = new LinkedList<>();
    private Version fixedVersion;
    private Version injectedVersion;
    private Date created;
    private Date resolved;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Version getOpeningVersion() {
        return openingVersion;
    }

    public void setOpeningVersion(Version openingVersion) {
        this.openingVersion = openingVersion;
    }

    public List<Version> getAffectedVersions() {
        return affectedVersions;
    }

    public void setAffectedVersions(List<Version> affectedVersions) {
        this.affectedVersions = affectedVersions;
    }

    public Version getFixedVersion() {
        return fixedVersion;
    }

    public void setFixedVersion(Version fixedVersion) {
        this.fixedVersion = fixedVersion;
    }

    public Version getInjectedVersion() {
        return injectedVersion;
    }

    public void setInjectedVersion(Version injectedVersion) {
        this.injectedVersion = injectedVersion;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getResolved() {
        return resolved;
    }

    public void setResolved(Date resolved) {
        this.resolved = resolved;
    }

    public List<Commit> getCommits() {
        return commits;
    }

    public void addCommit(Commit commit) {
        this.commits.add(commit);
    }

    public void addAffectedVersion(Version version) {
        this.affectedVersions.add(version);
    }
}
