package it.mazz.isw2.ml;

import com.opencsv.CSVWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

import java.io.File;
import java.io.FileWriter;

public class ModifiedDatasetsAnalysis {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModifiedDatasetsAnalysis.class);
    private static final String RANDOM_FOREST = "RandomForest";
    private static final String NAIVE_BAYES = "NaiveBayes";
    private static final String IBK = "IBk";

    private static ModifiedDatasetsAnalysis instance = null;

    private ModifiedDatasetsAnalysis() {
    }

    public static ModifiedDatasetsAnalysis getInstance() {
        if (instance == null)
            instance = new ModifiedDatasetsAnalysis();
        return instance;
    }

    public void analyzeDatasets(String projName, String classifierName) {
        Classifier classifier = getClassifier(classifierName);
        if (classifier == null) {
            LOGGER.error("Classifier not found");
            return;
        }
        String datasetAPath = String.format("./output/%s/arff/%s-datasetA.arff", projName, projName);
        String datasetBPath = String.format("./output/%s/arff/%s-datasetB.arff", projName, projName);
        String datasetBplusPath = String.format("./output/%s/arff/%s-datasetB+.arff", projName, projName);
        String datasetCPath = String.format("./output/%s/arff/%s-datasetC.arff", projName, projName);

        trainClassifier(classifier, datasetAPath);

        String header = "dataset,type,TP,FP,TN,FN,Precision,Recall,AUC,F1,MCC,Kappa\n";

        File analysisResults = new File(String.format("./output/%s/%s-datasets-results.csv", projName, projName));
        try (CSVWriter writer = new CSVWriter(new FileWriter(analysisResults))) {
            writer.writeNext(header.split(","));
            evalDataset(datasetAPath, classifier, "A", writer);
            evalDataset(datasetBPath, classifier, "B", writer);
            evalDataset(datasetBplusPath, classifier, "B+", writer);
            evalDataset(datasetCPath, classifier, "C", writer);
        } catch (Exception e) {
            LOGGER.error("Error writing results to CSV", e);
        }
    }

    private void evalDataset(String dataset, Classifier classifier, String name, CSVWriter writer) throws Exception {
        Instances data = getInstances(dataset);
        if (data == null) {
            LOGGER.error("Dataset not found");
            return;
        }
        Evaluation eval = new Evaluation(data);
        eval.evaluateModel(classifier, data);

        String[] weighted = {
                name,
                "weighted",
                Double.toString(eval.weightedTruePositiveRate()), Double.toString(eval.weightedFalsePositiveRate()),
                Double.toString(eval.weightedTrueNegativeRate()), Double.toString(eval.weightedFalseNegativeRate()),
                Double.toString(eval.weightedPrecision()),
                Double.toString(eval.weightedRecall()),
                Double.toString(eval.weightedAreaUnderROC()),
                Double.toString(eval.weightedFMeasure()),
                Double.toString(eval.weightedMatthewsCorrelation()),
                Double.toString(eval.kappa())};
        writer.writeNext(weighted);

        String[] classBuggy = {
                name,
                "Class Buggy",
                Double.toString(eval.numTruePositives(0)), Double.toString(eval.numFalsePositives(0)),
                Double.toString(eval.numTrueNegatives(0)), Double.toString(eval.numFalseNegatives(0)),
                Double.toString(eval.precision(0)),
                Double.toString(eval.recall(0)),
                Double.toString(eval.areaUnderROC(0)),
                Double.toString(eval.fMeasure(0)),
                Double.toString(eval.matthewsCorrelationCoefficient(0)),
                Double.toString(eval.kappa())};
        writer.writeNext(classBuggy);
    }

    private Instances getInstances(String datasetPath) {
        Instances data = null;
        try {
            data = new DataSource(datasetPath).getDataSet();
        } catch (Exception e) {
            LOGGER.error("Error loading data source", e);
            return data;
        }
        if (data.classIndex() == -1)
            data.setClassIndex(data.numAttributes() - 1);
        return data;
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

    private Classifier getClassifier(String classifierName) {
        return switch (classifierName) {
            case RANDOM_FOREST -> new RandomForest();
            case NAIVE_BAYES -> new NaiveBayes();
            case IBK -> new IBk();
            default -> null;
        };
    }
}
