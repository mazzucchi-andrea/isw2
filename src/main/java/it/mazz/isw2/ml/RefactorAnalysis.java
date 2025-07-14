package it.mazz.isw2.ml;

import com.github.mauricioaniche.ck.CK;
import com.github.mauricioaniche.ck.CKClassResult;
import com.github.mauricioaniche.ck.CKMethodResult;
import com.github.mauricioaniche.ck.CKNotifier;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;
import it.mazz.isw2.Util;
import it.mazz.isw2.entities.Commit;
import it.mazz.isw2.entities.Features;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.Report;
import net.sourceforge.pmd.RuleViolation;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.RandomForest;
import weka.core.*;
import weka.core.converters.ConverterUtils.DataSource;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class RefactorAnalysis {
    private static final Logger LOGGER = LoggerFactory.getLogger(RefactorAnalysis.class);
    private static final String RANDOM_FOREST = "RandomForest";
    private static final String NAIVE_BAYES = "NaiveBayes";
    private static final String IBK = "IBk";
    private static RefactorAnalysis instance = null;

    private RefactorAnalysis() {
    }

    public static RefactorAnalysis getInstance() {
        if (instance == null)
            instance = new RefactorAnalysis();
        return instance;
    }

    public void analyze(String projName, String methodName, int version, String classifierName) {
        Classifier classifier = getClassifier(classifierName);
        if  (classifier == null) {
            LOGGER.error("Classifier not found");
            return;
        }
        String datasetAPath = String.format("./output/%s/arff/%s-datasetA.arff", projName, projName);

        trainClassifier(classifier, datasetAPath);

        List<File> originalFiles = new ArrayList<>();
        Util.getInstance().listFiles(String.format("./original/%s/", projName), originalFiles);
        Features originalFeatures = getOriginalFeatures(projName, originalFiles.get(0).getName(), methodName, version);

        List<File> refactoredFiles = new ArrayList<>();
        Util.getInstance().listFiles(String.format("./refactored/%s/", projName), refactoredFiles);
        Features refactoredFeatures = getFeatures(refactoredFiles.get(0), methodName);
        refactoredFeatures.setMethodHistories(originalFeatures.getMethodHistories());
        for (int i = 0; i < originalFeatures.getAuthorSize(); i++) {
            refactoredFeatures.addAuthor(new PersonIdent(String.format("%d",i), String.format("%d",i)));
        }
        refactoredFeatures.setBuggy(originalFeatures.isBuggy());
        List<Features> features = new ArrayList<>();
        features.add(originalFeatures);
        features.add(refactoredFeatures);
        createDatasets(projName, features);
        Instances instances = getInstances(String.format("./output/%s/arff/%s-refactored.arff", projName, projName));
        try {
            double result = classifier.classifyInstance(instances.get(0));
            LOGGER.info("Classification original is: {}", result);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
        try {
            double result = classifier.classifyInstance(instances.get(1));
            LOGGER.info("Classification refactored is: {}", result);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    private void createDatasets(String projName, List<Features> featuresList) {
        String arffHeader = "@relation " + "Refactored_" + projName + "\n\n" +
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

        File arffDataset = new File(String.format("./output/%s/arff/%s-refactored.arff", projName, projName));
        try (FileWriter outputFile = new FileWriter(arffDataset)) {
            outputFile.write(arffHeader);
            writeARFFDataset(outputFile, featuresList);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            return;
        }

    }

    private void writeARFFDataset(FileWriter outputFile, List<Features> featuresList) throws IOException {
        try (CSVWriter writer = new CSVWriter(outputFile)) {
            for (Features features : featuresList) {
                writer.writeNext(features.toStringArrayForArff());
            }
        }
    }

    private void trainClassifier(Classifier classifier, String path) {
        Instances data = getInstances(path);
        if (data == null) {
            return;
        }
        if (data.classIndex() == -1)
            data.setClassIndex(data.numAttributes() - 1);
        try {
            classifier.buildClassifier(data);
        } catch (Exception e) {
            LOGGER.error("Error building classifier", e);
        }
    }

    private Instances getInstances(String datasetPath) {
        Instances data = null;
        try {
            DataSource source = new DataSource(datasetPath);
            data = source.getDataSet();
        } catch (Exception e) {
            LOGGER.error("Error loading data source", e);
            return data;
        }
        if (data.classIndex() == -1)
            data.setClassIndex(data.numAttributes() - 1);
        return data;
    }

    private Classifier getClassifier(String classifierName) {
        return switch (classifierName) {
            case RANDOM_FOREST -> new RandomForest();
            case NAIVE_BAYES -> new NaiveBayes();
            case IBK -> new IBk();
            default -> null;
        };
    }

    private Features getOriginalFeatures(String projName, String fileName, String methodName, Integer version) {
        String csvDatasetPath = String.format("./output/%s/csv/%s-datasetA.csv/", projName, projName);
        Features features = null;
        try (CSVReader csvReader = new CSVReader(Files.newBufferedReader(Path.of(csvDatasetPath)))) {
            String[] line;
            csvReader.readNext();
            while ((line = csvReader.readNext()) != null) {
                if (line[0].equals(version.toString()) && line[2].equals(fileName) && line[3].equals(methodName)) {
                    features = new Features(line);
                }
            }
        } catch (CsvValidationException | IOException e) {
            return features;
        }
        return features;
    }

    private Features getFeatures(File file, String methodName) {
        Map<String, CKClassResult> results = new HashMap<>();
        CK ck = new CK();
        ck.calculate(file.getPath(), new CKNotifier() {
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
            pmd.files().addFile(file.toPath());
            Report report = pmd.performAnalysisAndCollectReport();
            violations = report.getViolations();
        }
        Features features = null;
        try {
            for (CKMethodResult methodResult : results.values().iterator().next().getMethods()) {
                if (!methodResult.getMethodName().contains(methodName)) continue;
                features = new Features(file.getName(), methodResult);
                features.setSmells(getSmells(violations, features.getMethodName()));
            }
        } catch (NoSuchElementException e) {
            return features;
        }
        return features;
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
