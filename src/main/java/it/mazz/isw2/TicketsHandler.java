package it.mazz.isw2;

import it.mazz.isw2.entities.Commit;
import it.mazz.isw2.entities.Ticket;
import it.mazz.isw2.entities.Version;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TicketsHandler {

    private static final List<Ticket> tickets = new ArrayList<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(TicketsHandler.class);

    private TicketsHandler() {
    }

    public static void getTickets(String projName, List<Version> versions) {
        int j;
        int i = 0;
        int total;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        //Get JSON API for closed bugs w/ AV in the project
        do {
            //Only gets a max of 1000 at a time, so must do this multiple times if bugs > 1000
            j = i + 1000;
            String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22" + projName +
                    "%22AND%22issueType%22=%22Bug%22AND" +
                    "%28%22resolution%22%3D%22fixed%22OR%22resolution%22%3D%22done%22%29AND" +
                    "%28%22status%22%3D%22closed%22OR%22status%22%3D%22resolved%22OR%22status%22%3D%22done%22%29" +
                    "AND%22resolution%22=%22fixed%22&fields=" + "key,resolutiondate,versions,created&startAt=" + i
                    + "&maxResults=" + j;
            JSONObject json = Util.getInstance().readJsonFromUrl(url);
            JSONArray issues = json.getJSONArray("issues");
            total = json.getInt("total");
            for (; i < total && i < j; i++) {
                //Iterate through each bug
                JSONObject jsonTicket = issues.getJSONObject(i % 1000);
                JSONObject ticketFields = (JSONObject) jsonTicket.get("fields");
                Ticket ticket = new Ticket();
                ticket.setKey(jsonTicket.get("key").toString());
                try {
                    ticket.setCreated(sdf.parse(ticketFields.get("created").toString()));
                    ticket.setResolved(sdf.parse(ticketFields.get("resolutiondate").toString()));
                } catch (ParseException e) {
                    LOGGER.warn(e.getMessage());
                }
                url = "https://issues.apache.org/jira/rest/api/2/issue/" + ticket.getKey();
                JSONObject issue = Util.getInstance().readJsonFromUrl(url);
                JSONObject issueFields = (JSONObject) issue.get("fields");
                List<Version> affectedVersions = getVersionByFieldName(issueFields, "versions", versions);
                List<Version> fixedVersions = getVersionByFieldName(issueFields, "fixVersions", versions);
                if (setTicketVersions(ticket, affectedVersions, fixedVersions)) {
                    tickets.add(ticket);
                }
            }
        } while (i < total);
        tickets.sort(Comparator.comparing(Ticket::getCreated));
        proportionReviewTickets(versions);
    }

    public static double getTicketsSize() {
        return tickets.size();
    }

    private static List<Version> getVersionByFieldName(JSONObject issueFields, String fieldName, List<Version> jiraVersions) {
        List<Version> versions = new ArrayList<>();
        JSONArray versionJsonArray = (JSONArray) issueFields.get(fieldName);
        for (int k = 0; k < versionJsonArray.length(); k++) {
            JSONObject version = (JSONObject) (versionJsonArray.get(k));
            for (Version v : jiraVersions) {
                if (v.getName().equals(version.get("name").toString())) {
                    versions.add(v);
                    break;
                }
            }
        }
        return versions;
    }

    private static void proportionReviewTickets(List<Version> versions) {
        double proportion = totalProportion();
        for (Ticket ticket : tickets) {
            if (ticket.getAffectedVersions().isEmpty()) {
                completeTicketVersions(ticket, proportion, versions);
            }
        }
    }

    private static void completeTicketVersions(Ticket ticket, double proportion, List<Version> versions) {
        float fv = ticket.getFixedVersion().getIncremental();
        float ov = ticket.getOpeningVersion().getIncremental();
        int iv = (int) (fv - (fv - ov) * proportion);
        if (iv < 0) {
            ticket.setInjectedVersion(versions.get(0));
        } else {
            ticket.setInjectedVersion(versions.get(iv));
        }
        for (Version version : versions) {
            if (version.getIncremental() < iv) continue;
            if (version.getIncremental() < fv) {
                ticket.addAffectedVersion(version);
            }
        }
    }

    private static double totalProportion() {
        List<Ticket> ticketColdStart = new ArrayList<>();
        List<Double> proportionList = new ArrayList<>();
        for (Ticket ticket : tickets) {
            if (!ticket.getAffectedVersions().isEmpty()) {
                ticketColdStart.add(ticket);
            }
        }
        for (Ticket ticket : ticketColdStart) {
            int iv = ticket.getInjectedVersion().getIncremental();
            int fv = ticket.getFixedVersion().getIncremental();
            int ov = ticket.getOpeningVersion().getIncremental();
            if (fv == ov) continue;
            proportionList.add((double) (fv - iv) / (fv - ov));
        }
        Collections.sort(proportionList);
        return proportionList.get(proportionList.size() / 2);
    }

    private static boolean setTicketVersions(Ticket ticket, List<Version> affectedVersions, List<Version> fixedVersions) {
        if (!fixedVersions.isEmpty()) {
            fixedVersions.sort(Comparator.comparing(Version::getReleaseDate));
            ticket.setFixedVersion(fixedVersions.get(0));
        } else {
            ticket.setFixedVersion(VersionsHandler.getVersionByDate(ticket.getResolved()));
        }
        if (ticket.getFixedVersion() == null) return false;
        affectedVersions.remove(ticket.getFixedVersion());
        ticket.setAffectedVersions(affectedVersions);
        ticket.setOpeningVersion(VersionsHandler.getVersionByDate(ticket.getCreated()));
        if (!ticket.getAffectedVersions().isEmpty()) {
            ticket.setInjectedVersion(ticket.getAffectedVersions().get(0));
        }
        return true;
    }

    public static void addCommitToTickets(List<Commit> commits) {
        for (Commit commit : commits) {
            for (Ticket ticket : tickets) {
                if (commit.getMessage().startsWith(ticket.getKey() + ':')) {
                    ticket.addCommit(commit);
                    break;
                }
            }
        }
        tickets.removeIf(ticket -> ticket.getCommits().isEmpty());
    }

    public static List<Ticket> getTickets() {
        return tickets;
    }
}
