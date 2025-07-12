package it.mazz.isw2;

import com.github.mauricioaniche.ck.CK;
import com.github.mauricioaniche.ck.CKClassResult;
import com.github.mauricioaniche.ck.CKMethodResult;
import com.github.mauricioaniche.ck.CKNotifier;
import com.opencsv.CSVWriter;
import it.mazz.isw2.entities.Commit;
import it.mazz.isw2.entities.Features;
import it.mazz.isw2.entities.Ticket;
import it.mazz.isw2.entities.Version;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.Report;
import net.sourceforge.pmd.RuleViolation;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DatasetGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetGenerator.class);

    private static DatasetGenerator instance = null;

    private DatasetGenerator() {
    }

    public static DatasetGenerator getInstance() {
        if (instance == null)
            instance = new DatasetGenerator();
        return instance;
    }

    public void generateDataset(String projName, Double percent) {
        LOGGER.info("Project: {}", projName);

        LOGGER.info("Delete previous results");
        File repo = deletePreviousResults(projName);
        if (repo == null) {
            LOGGER.error("Unable to delete previous results");
            return;
        }

        LOGGER.info("Checkout latest Project Revision");
        Git git = checkoutProjectsGit(projName);
        if (git == null) {
            LOGGER.error("Git checkout failed.");
            return;
        }

        LOGGER.info("Retrieve Versions from Jira");
        VersionsHandler.getVersionsFromJira(projName);

        LOGGER.info("Merge Jira Versions and ref/tags to take commits");
        VersionsHandler.setReleaseCommit(git);
        try (Repository repository = git.getRepository()) {
            VersionsHandler.addCommitsToVersions(repository, git);
        }
        LOGGER.info("Version list size: {}", VersionsHandler.getVersionsSize());

        LOGGER.info("Retrieve Tickets from Jira");
        TicketsHandler.getTickets(projName, VersionsHandler.getVersions());
        LOGGER.info("Ticket list size: {}", TicketsHandler.getTicketsSize());

        LOGGER.info("Get All Commits");
        List<Commit> commits = getAllCommits(git);
        if (commits.isEmpty())
            return;

        LOGGER.info("Add commits to tickets");
        TicketsHandler.addCommitToTickets(commits);
        LOGGER.info("Ticket list new size: {}", TicketsHandler.getTicketsSize());

        LOGGER.info("Retrieving all method features for the first {}% of versions", percent * 100);
        List<Features> features = getAllFeatures(projName, git, VersionsHandler.getOlderVersions(percent), TicketsHandler.getTickets());
        if (features.isEmpty()) {
            LOGGER.error("No features found");
            return;
        }
        LOGGER.info("Features list size: {}", features.size());

        String dirPath = String.format("./output/%s/", projName);
        try {
            Path path = Paths.get(dirPath);
            Files.createDirectories(path);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            return;
        }

        createDatasets(projName, features);

        if (repo.exists()) {
            try {
                FileUtils.deleteDirectory(repo);
            } catch (IOException e) {
                LOGGER.warn(e.getMessage());
            }
        }

        LOGGER.info("END");
    }

    private File deletePreviousResults(String projName) {
        File repo = new File("./" + projName.toLowerCase());
        File output = new File("./output/" + projName);
        try {
            if (repo.exists())
                FileUtils.deleteDirectory(repo);
            if (output.exists())
                FileUtils.deleteDirectory(output);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            return null;
        }
        return repo;
    }

    private Git checkoutProjectsGit(String projName) {
        Git git;
        try {
            git = Git.cloneRepository().setURI("https://github.com/apache/" + projName.toLowerCase() + ".git").call();
        } catch (GitAPIException e) {
            LOGGER.error(e.getMessage());
            return null;
        }
        return git;
    }

    private List<Commit> getAllCommits(Git git) {
        Map<String, Commit> commits = new HashMap<>();
        try (Repository repository = git.getRepository()) {
            RevWalk revWalk = new RevWalk(repository);
            List<Ref> allRefs;
            try {
                allRefs = git.branchList().call();
                allRefs.addAll(git.tagList().call());
            } catch (GitAPIException e) {
                LOGGER.error(e.getMessage());
                return Collections.emptyList();
            }

            for (Ref ref : allRefs) {
                // Resolve the reference to an object ID
                ObjectId refObjectId = ref.getObjectId();
                if (refObjectId == null) continue;

                // Mark the start of the walk for this reference
                try {
                    revWalk.markStart(revWalk.parseCommit(refObjectId));
                } catch (IOException e) {
                    LOGGER.error(e.getMessage());
                    return Collections.emptyList();
                }
            }

            // Traverse all commits reachable from the references
            for (RevCommit revCommit : revWalk) {
                Commit commit = new Commit(revCommit, repository, git);
                commits.put(commit.getSha(), commit);
            }
        }
        return new ArrayList<>(commits.values());
    }

    private List<Features> getAllFeatures(String projName, Git git, List<Version> versions, List<Ticket> tickets) {
        List<Features> featuresList = new LinkedList<>();
        for (Version version : versions) {
            try {
                git.checkout().setName(version.getSha()).setCreateBranch(false).call();
            } catch (GitAPIException e) {
                LOGGER.warn(e.getMessage());
                return Collections.emptyList();
            }
            List<File> files = new ArrayList<>();
            Util.getInstance().listFiles("./" + projName.toLowerCase(), files);
            for (File f : files) {
                boolean java = Pattern.compile(Pattern.quote(".java"),
                        Pattern.CASE_INSENSITIVE).matcher(f.getName()).find();
                boolean testPath = Pattern.compile(Pattern.quote("test"),
                        Pattern.CASE_INSENSITIVE).matcher(f.getPath()).find();
                boolean exampleName = Pattern.compile(Pattern.quote("example"),
                        Pattern.CASE_INSENSITIVE).matcher(f.getPath()).find();
                boolean benchmarkPath = Pattern.compile(Pattern.quote("benchmark"),
                        Pattern.CASE_INSENSITIVE).matcher(f.getPath()).find();
                if (!java || testPath || exampleName || benchmarkPath) continue;
                featuresList.addAll(getFeatures(f, version, projName, tickets, git));
            }
        }
        return featuresList;
    }

    private void createDatasets(String projName, List<Features> featuresList) {
        String arffHeader = "@relation " + projName + "\n\n" +
                "@attribute nAuth numeric\n" +
                "@attribute methodHistories numeric\n" +
                "@attribute loc numeric\n" +
                "@attribute fanin numeric\n" +
                "@attribute fanout numeric\n" +
                "@attribute wmc numeric\n" +
                "@attribute returns numeric\n" +
                "@attribute loops numeric\n" +
                "@attribute comparison numeric\n" +
                "@attribute maxNested numeric\n" +
                "@attribute math numeric\n" +
                "@attribute smells numeric\n" +
                "@attribute buggy {yes,no}\n\n" +
                "@data\n";

        String header = "#,Version,fileName,methodName,nAuth,methodHistories,loc,fain,fanout,wmc,returns,loops," +
                "comparison,maxNested,math,smells,buggy\n";

        File arffDataset = new File(String.format("./output/%s/%s-dataset.arff/", projName, projName));
        try (FileWriter outputFile = new FileWriter(arffDataset)) {
            outputFile.write(arffHeader);
            writeARFFDataset(outputFile, featuresList);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            return;
        }

        File csvDataset = new File(String.format("./output/%s/%s-dataset.csv/", projName, projName));
        try (FileWriter outputFile = new FileWriter(csvDataset)) {
            outputFile.write(header);
            writeCSVDataset(outputFile, featuresList);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }

    private void writeCSVDataset(FileWriter outputFile, List<Features> featuresList) throws IOException {
        try (CSVWriter writer = new CSVWriter(outputFile)) {
            for (Features features : featuresList) {
                writer.writeNext(features.toStringArrayForCSV());
            }
        }
    }

    private void writeARFFDataset(FileWriter outputFile, List<Features> featuresList) throws IOException {
        try (CSVWriter writer = new CSVWriter(outputFile)) {
            for (Features features : featuresList) {
                writer.writeNext(features.toStringArrayForArff());
            }
        }
    }

    private List<Features> getFeatures(File f, Version version, String projName, List<Ticket> tickets, Git git) {
        Map<String, CKClassResult> results = new HashMap<>();
        CK ck = new CK();
        ck.calculate(f.getPath(), new CKNotifier() {
            @Override
            public void notify(CKClassResult result) {
                results.put(result.getClassName(), result);

            }

            @Override
            public void notifyError(String sourceFilePath, Exception e) {
                LOGGER.error("Error CK calculation in {}", sourceFilePath);
            }
        });
        PMDConfiguration config = new PMDConfiguration();
        config.setIgnoreIncrementalAnalysis(true);
        List<RuleViolation> violations;
        try (PmdAnalysis pmd = PmdAnalysis.create(config)) {
            pmd.addRuleSet(pmd.newRuleSetLoader().loadFromResource("rules.xml"));
            pmd.files().addFile(f.toPath());
            Report report = pmd.performAnalysisAndCollectReport();
            violations = report.getViolations();
        }
        List<Features> methodsFeatures = new ArrayList<>();
        try {
            for (CKMethodResult methodResult : results.values().iterator().next().getMethods()) {
                Features features = new Features(version, f.getName(), methodResult);
                features.setSmells(getSmells(violations, features.getMethodName()));
                int histories = 0;
                String path = f.getPath().substring(f.getPath().indexOf(projName.toLowerCase()) + projName.length() + 1);
                try (Repository repository = git.getRepository()) {
                    for (Commit commit : version.getCommits().values()) {
                        if (commit.isFileInCommit(path)) {
                            boolean modified = isMethodModified(repository, commit.getSha(), features.getMethodName(), path);
                            if (modified) {
                                features.addAuthor(commit.getAuthor());
                                histories++;
                            }
                        }
                    }
                    features.setMethodHistories(histories);
                    features.setBuggy(isMethodBuggy(repository, tickets, version, path, features.getMethodName(), path));
                    methodsFeatures.add(features);
                }
            }
        } catch (NoSuchElementException | IOException e) {
            return Collections.emptyList();
        }
        return methodsFeatures;
    }

    boolean isMethodBuggy(Repository repository, List<Ticket> tickets, Version version, String path, String methodName, String filePath) throws IOException {
        for (Ticket ticket : tickets) {
            for (Commit commit : ticket.getCommits()) {
                if (commit.isFileInCommit(path) && ticket.getAffectedVersions().contains(version) &&
                        isMethodModified(repository, commit.getSha(), methodName, filePath)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isMethodModified(Repository repository, String commitId, String methodName, String fileName) throws IOException {
        RevWalk revWalk = new RevWalk(repository);
        RevCommit commit = revWalk.parseCommit(repository.resolve(commitId));
        if (commit.getParentCount() == 0) {
            return true;
        }
        boolean modified = false;
        RevCommit parentCommit = revWalk.parseCommit(commit.getParent(0).getId());
        try (DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
            diffFormatter.setRepository(repository);
            List<DiffEntry> diffs = diffFormatter.scan(parentCommit.getTree(), commit.getTree());
            for (DiffEntry entry : diffs) {
                String currentFilePath = entry.getNewPath();
                String oldFilePath = entry.getOldPath();
                if (!currentFilePath.equals(fileName) && !oldFilePath.equals(fileName)) {
                    continue;
                }
                String fileContentNew = getFileContent(repository, commit, currentFilePath);
                if (entry.getChangeType() == DiffEntry.ChangeType.ADD) {
                    modified = extractMethodBody(fileContentNew, methodName).isPresent();
                }
                String fileContentOld = getFileContent(repository, parentCommit, oldFilePath);
                if (entry.getChangeType() == DiffEntry.ChangeType.MODIFY ||
                        entry.getChangeType() == DiffEntry.ChangeType.RENAME) {
                    modified = compareMethodBody(methodName, fileContentNew, fileContentOld);
                } else if (entry.getChangeType() == DiffEntry.ChangeType.DELETE) {
                    // If the file was deleted and contained the method, the method is "modified" (removed).
                    modified = extractMethodBody(fileContentOld, methodName).isPresent();
                }
                if (modified) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean compareMethodBody(String methodName, String fileContentNew, String fileContentOld) {
        // For modified or renamed files, compare the method's body content
        Optional<String> methodBodyNew = extractMethodBody(fileContentNew, methodName);
        Optional<String> methodBodyOld = extractMethodBody(fileContentOld, methodName);
        // Case 1: Method not found in either version
        if (methodBodyNew.isEmpty() && methodBodyOld.isEmpty()) {
            return false;
        }
        // Case 2: Method was added or removed within an existing file
        if (methodBodyNew.isEmpty() || methodBodyOld.isEmpty()) {
            return true;
        }
        // Case 3: Both exist, compare their content
        return !methodBodyNew.get().equals(methodBodyOld.get());
    }

    public Optional<String> extractMethodBody(String fileContent, String methodName) {
        if (fileContent == null) {
            return Optional.empty();
        }

        Optional<int[]> methodSignatureRange = findMethodSignature(fileContent, methodName);
        if (methodSignatureRange.isEmpty()) {
            return Optional.empty();
        }

        int signatureStart = methodSignatureRange.get()[0];
        String signatureEndIndicator = methodSignatureRange.get()[1] == -1 ? "\n" : String.valueOf((char) methodSignatureRange.get()[1]);

        if (";".equals(signatureEndIndicator) || "\n".equals(signatureEndIndicator)) {
            return extractAbstractMethod(fileContent, signatureStart);
        }

        return extractMethodBodyWithBraces(fileContent, signatureStart);
    }

    private Optional<int[]> findMethodSignature(String fileContent, String methodName) {
        String methodSignatureRegex =
                "\\b(?:public|protected|private|static|final|abstract|synchronized|transient|volatile|strictfp|native|default|record)\\s+(?:<[^>]+>\\s+)?\\S+\\s+" +
                        Pattern.quote(methodName) + "\\s*\\([^)]*\\)\\s*(?:throws\\s+[\\w\\s,]+\\s*)?(\\{|;|\n)";
        Pattern pattern = Pattern.compile(methodSignatureRegex, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(fileContent);

        if (matcher.find()) {
            int signatureStart = matcher.start();
            String group = matcher.group(1);
            int signatureEndIndicator = group != null && !group.isEmpty() ? group.charAt(0) : -1;
            return Optional.of(new int[]{signatureStart, signatureEndIndicator});
        }
        return Optional.empty();
    }

    private Optional<String> extractAbstractMethod(String fileContent, int signatureStart) {
        int endIndex = fileContent.indexOf('\n', signatureStart);
        if (endIndex == -1) {
            endIndex = fileContent.length();
        }
        return Optional.of(fileContent.substring(signatureStart, endIndex).trim());
    }

    private Optional<String> extractMethodBodyWithBraces(String fileContent, int signatureStart) {
        int openBraceIndex = fileContent.indexOf('{', signatureStart);
        if (openBraceIndex == -1) {
            return Optional.empty();
        }

        int closingBraceIndex = findClosingBraceIndex(fileContent, openBraceIndex);
        if (closingBraceIndex != -1) {
            return Optional.of(fileContent.substring(signatureStart, closingBraceIndex + 1));
        }
        return Optional.empty();
    }

    private int findClosingBraceIndex(String fileContent, int openBraceIndex) {
        int braceCount = 0;
        for (int currentIndex = openBraceIndex; currentIndex < fileContent.length(); currentIndex++) {
            char c = fileContent.charAt(currentIndex);
            if (c == '{') {
                braceCount++;
            } else if (c == '}') {
                braceCount--;
            }
            if (braceCount == 0 && c == '}') {
                return currentIndex;
            }
        }
        return -1;
    }

    private String getFileContent(Repository repository, RevCommit commit, String filePath) throws IOException {
        try (TreeWalk treeWalk = TreeWalk.forPath(repository, filePath, commit.getTree())) {
            if (treeWalk != null) {
                ObjectId blobId = treeWalk.getObjectId(0);
                ObjectLoader loader = repository.open(blobId);
                return new String(loader.getBytes(), StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    public int getSmells(List<RuleViolation> violations, String methodName) {
        int smells = 0;
        for (RuleViolation violation : violations) {
            if (methodName.equals(violation.getMethodName())) {
                smells++;
            }
        }
        return smells;
    }
}
