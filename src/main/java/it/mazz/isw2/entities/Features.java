package it.mazz.isw2.entities;

import com.github.mauricioaniche.ck.CKMethodResult;
import org.eclipse.jgit.lib.PersonIdent;

import java.util.HashSet;
import java.util.Set;

public class Features {
    private Integer versionIncremental;
    private String versionName;
    private final String methodName;
    private String qualifiedMethodName;
    private Set<String> methodInvocations;
    private final String fileName;
    private final Set<PersonIdent> authors = new HashSet<>();
    private Integer methodHistories;
    private Integer loc;
    private Integer fain;
    private Integer fanout;
    private Integer wmc;
    private Integer returns;
    private Integer loops;
    private Integer comparison;
    private Integer maxNested;
    private Integer math;
    private Integer smells;
    private boolean buggy;

    public Features(Version version, String fileName, CKMethodResult ckMethodResult) {
        this.versionIncremental = version.getIncremental();
        this.versionName = version.getName();
        this.fileName = fileName;
        if (ckMethodResult.getMethodName().indexOf('/') != -1) {
            this.methodName = ckMethodResult.getMethodName().substring(0, ckMethodResult.getMethodName().indexOf('/'));
        } else {
            this.methodName = ckMethodResult.getMethodName();
        }
        this.qualifiedMethodName = ckMethodResult.getQualifiedMethodName();
        this.methodInvocations = ckMethodResult.getMethodInvocations();
        this.loc = ckMethodResult.getLoc();
        this.fain = ckMethodResult.getFanin();
        this.fanout = ckMethodResult.getFanout();
        this.wmc = ckMethodResult.getWmc();
        this.returns = ckMethodResult.getReturnQty();
        this.loops = ckMethodResult.getLoopQty();
        this.comparison = ckMethodResult.getComparisonsQty();
        this.maxNested = ckMethodResult.getMaxNestedBlocks();
        this.math = ckMethodResult.getMathOperationsQty();
        this.smells = 0;
        this.buggy = false;
    }

    public Features(String[] line) {
        this.versionIncremental = Integer.valueOf(line[0]);
        this.versionName = line[1];
        this.fileName = line[2];
        this.methodName = line[3];
        for (int i = 0; i < Integer.parseInt(line[4]); i++) {
            this.authors.add(new PersonIdent(String.format("%d",i), String.format("%d",i)));
        }
        this.methodHistories = Integer.valueOf(line[5]);
        this.loc = Integer.valueOf(line[6]);
        this.fain = Integer.valueOf(line[7]);
        this.fanout = Integer.valueOf(line[8]);
        this.wmc = Integer.valueOf(line[9]);
        this.returns = Integer.valueOf(line[10]);
        this.loops = Integer.valueOf(line[11]);
        this.comparison = Integer.valueOf(line[12]);
        this.maxNested = Integer.valueOf(line[13]);
        this.math = Integer.valueOf(line[14]);
        this.smells = Integer.valueOf(line[15]);
        this.buggy = !line[16].equals("no");
    }

    public Features(String fileName, CKMethodResult ckMethodResult) {
        this.fileName = fileName;
        if (ckMethodResult.getMethodName().indexOf('/') != -1) {
            this.methodName = ckMethodResult.getMethodName().substring(0, ckMethodResult.getMethodName().indexOf('/'));
        } else {
            this.methodName = ckMethodResult.getMethodName();
        }
        this.qualifiedMethodName = ckMethodResult.getQualifiedMethodName();
        this.methodInvocations = ckMethodResult.getMethodInvocations();
        this.loc = ckMethodResult.getLoc();
        this.fain = ckMethodResult.getFanin();
        this.fanout = ckMethodResult.getFanout();
        this.wmc = ckMethodResult.getWmc();
        this.returns = ckMethodResult.getReturnQty();
        this.loops = ckMethodResult.getLoopQty();
        this.comparison = ckMethodResult.getComparisonsQty();
        this.maxNested = ckMethodResult.getMaxNestedBlocks();
        this.math = ckMethodResult.getMathOperationsQty();
        this.smells = 0;
        this.buggy = false;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getQualifiedMethodName() {
        return qualifiedMethodName;
    }

    public boolean isInvocated(String methodName) {
        return methodInvocations.contains(methodName);
    }

    public void addAuthor(PersonIdent author) {
        authors.add(author);
    }

    public int getAuthorSize() {
        return authors.size();
    }

    public Integer getMethodHistories() {
        return methodHistories;
    }

    public void setMethodHistories(Integer methodHistories) {
        this.methodHistories = methodHistories;
    }

    public Integer getLoc() {
        return loc;
    }

    public void setLoc(Integer loc) {
        this.loc = loc;
    }

    public Integer getFain() {
        return fain;
    }

    public void setFain(Integer fain) {
        this.fain = fain;
    }

    public Integer getFanout() {
        return fanout;
    }

    public void setFanout(Integer fanout) {
        this.fanout = fanout;
    }

    public Integer getWmc() {
        return wmc;
    }

    public void setWmc(Integer wmc) {
        this.wmc = wmc;
    }

    public Integer getReturns() {
        return returns;
    }

    public void setReturns(Integer returns) {
        this.returns = returns;
    }

    public Integer getLoops() {
        return loops;
    }

    public void setLoops(Integer loops) {
        this.loops = loops;
    }

    public Integer getComparison() {
        return comparison;
    }

    public void setComparison(Integer comparison) {
        this.comparison = comparison;
    }

    public Integer getMaxNested() {
        return maxNested;
    }

    public void setMaxNested(Integer maxNested) {
        this.maxNested = maxNested;
    }

    public Integer getMath() {
        return math;
    }

    public void setMath(Integer math) {
        this.math = math;
    }

    public Integer getSmells() {
        return smells;
    }

    public void setSmells(Integer smells) {
        this.smells = smells;
    }

    public boolean isBuggy() {
        return buggy;
    }

    public void setBuggy(boolean buggy) {
        this.buggy = buggy;
    }

    public String[] toStringArrayForCSV() {
        String bugginess;
        if (buggy) {
            bugginess = "yes";
        } else {
            bugginess = "no";
        }
        return new String[]{
                versionIncremental.toString(),
                versionName,
                fileName,
                methodName,
                Integer.toString(authors.size()),
                methodHistories.toString(),
                loc.toString(),
                fain.toString(),
                fanout.toString(),
                wmc.toString(),
                returns.toString(),
                loops.toString(),
                comparison.toString(),
                maxNested.toString(),
                math.toString(),
                smells.toString(),
                bugginess};
    }

    public String[] toStringArrayForArff() {
        String bugginess;
        if (buggy) {
            bugginess = "yes";
        } else {
            bugginess = "no";
        }
        return new String[]{
                Integer.toString(authors.size()),
                methodHistories.toString(),
                loc.toString(),
                fain.toString(),
                fanout.toString(),
                wmc.toString(),
                returns.toString(),
                loops.toString(),
                comparison.toString(),
                maxNested.toString(),
                math.toString(),
                smells.toString(),
                bugginess};
    }
}