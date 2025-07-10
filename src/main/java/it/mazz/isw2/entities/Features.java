package it.mazz.isw2.entities;

import com.github.mauricioaniche.ck.CKMethodResult;

public class Features {
    private final Integer version;
    private final String methodName;
    private final String fileName;
    private Integer nAuth;
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

    public Features(Integer version, String fileName, CKMethodResult ckMethodResult) {
        this.version = version;
        this.fileName = fileName;
        if (ckMethodResult.getMethodName().indexOf('/') != -1) {
            this.methodName = ckMethodResult.getMethodName().substring(0, ckMethodResult.getMethodName().indexOf('/'));
        } else {
            this.methodName = ckMethodResult.getMethodName();
        }
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

    public Integer getVersion() {
        return version;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getFileName() {
        return fileName;
    }

    public Integer getnAuth() {
        return nAuth;
    }

    public void setnAuth(Integer nAuth) {
        this.nAuth = nAuth;
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

    public Boolean isBuggy() {
        return buggy;
    }

    public void setBuggy(Boolean buggy) {
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
                version.toString(),
                fileName,
                methodName,
                loc.toString(),
                nAuth.toString(),
                methodHistories.toString(),
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
                nAuth.toString(),
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