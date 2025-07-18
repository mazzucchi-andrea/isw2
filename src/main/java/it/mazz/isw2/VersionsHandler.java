package it.mazz.isw2;

import it.mazz.isw2.entities.Commit;
import it.mazz.isw2.entities.Version;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class VersionsHandler {

    private static final List<Version> versions = new ArrayList<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(VersionsHandler.class);

    private VersionsHandler() {
    }

    public static void getVersionsFromJira(String projName) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        JSONObject jiraVersionsJson;
        JSONArray jiraVersionsArray;
        try {
            HttpResponse<JsonNode> resp = Unirest.get("https://issues.apache.org/jira/rest/api/2/project/" + projName + "/version").asJson();
            jiraVersionsJson = new JSONObject(resp.getBody().toString());
            jiraVersionsArray = jiraVersionsJson.optJSONArray("values");
        } catch (JSONException e) {
            LOGGER.error(e.getMessage());
            return;
        }

        int i = 0;
        int j = 1;
        while (i < jiraVersionsArray.length()) {
            JSONObject jiraVersion = jiraVersionsArray.getJSONObject(i);
            String name = (String) jiraVersion.get("name");
            boolean released = jiraVersion.getBoolean("released");
            Date releaseDate;
            try {
                releaseDate = sdf.parse(jiraVersion.get("releaseDate").toString());
            } catch (Exception e) {
                i++;
                continue;
            }
            Version version = new Version(j, name, released, releaseDate);
            versions.add(version);
            j++;
            i++;
        }
    }

    public static List<Version> getVersions() {
        return versions;
    }

    public static Version getVersionByDate(Date date) {
        for (Version v : versions) {
            if (date.before(v.getReleaseDate()))
                return v;
        }
        return null;
    }

    public static int getVersionsSize() {
        return versions.size();
    }

    public static void setReleaseCommit(Git git) {
        List<Ref> tags;
        try {
            tags = git.tagList().call();
        } catch (GitAPIException e) {
            LOGGER.error(e.getMessage());
            return;
        }

        try (Repository repository = git.getRepository()) {
            for (Ref tag : tags) {
                String tagName = tag.getName().replace("refs/tags/", "");
                ObjectId tagId = tag.getObjectId();
                ObjectId peeledId = repository.getRefDatabase().peel(tag).getPeeledObjectId();

                ObjectId commitId = (peeledId != null) ? peeledId : tagId;
                for (Version version : versions) {
                    try (RevWalk revWalk = new RevWalk(repository)) {
                        RevCommit commit = revWalk.parseCommit(commitId);
                        if (tagName.contains(version.getName()))
                            version.setSha(commit.getName());
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.warn(e.getMessage());
        }

        removeInvalidVersions();
    }

    public static void addCommitsToVersions(Repository repository, Git git) {
        try (RevWalk revWalk = new RevWalk(repository)) {
            for (Version version : versions) {
                RevCommit releaseCommit = revWalk.parseCommit(repository.resolve(version.getSha()));
                revWalk.markStart(releaseCommit);
                for (RevCommit revCommit : revWalk) {
                    Commit commit = new Commit(revCommit, repository, git);
                    if (commit.javaFileInCommit() && !isCommitInPreviousVersions(version, commit))
                        version.addCommit(commit);
                }
            }
        } catch (IOException e) {
            LOGGER.warn(e.getMessage());
        }
    }

    public static boolean isCommitInPreviousVersions(Version version, Commit commit) {
        for (int i = 0; i < version.getIncremental(); i++) {
            if (versions.get(i).containsCommit(commit))
                return true;
        }
        return false;
    }

    public static List<Version> getOlderVersions(double p) {
        List<Version> versionsSplit = new ArrayList<>();
        int size = versions.size();
        int toMaintain = (int) (size * p);
        for (Version version : versions) {
            if (versionsSplit.size() < toMaintain) {
                versionsSplit.add(version);
            }
        }
        return versionsSplit;
    }

    private static void removeInvalidVersions() {
        int inc = 1;
        Iterator<Version> it = versions.iterator();
        while (it.hasNext()) {
            Version version = it.next();
            if (version.getSha() == null || version.getReleaseDate() == null) {
                it.remove();
            } else {
                version.setIncremental(inc);
                inc++;
            }
        }
    }
}
