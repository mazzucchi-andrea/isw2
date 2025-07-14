package it.mazz.isw2.ml;

import com.opencsv.CSVWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weka.attributeSelection.CorrelationAttributeEval;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.RandomForest;
import weka.core.Debug;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

public class Analysis {
    private static final Logger LOGGER = LoggerFactory.getLogger(Analysis.class);
    private static final String RANDOM_FOREST = "RandomForest";
    private static final String NAIVE_BAYES = "NaiveBayes";
    private static final String IBK = "IBk";
    private static Analysis instance = null;

    private Analysis() {
    }

    public static Analysis getInstance() {
        if (instance == null)
            instance = new Analysis();
        return instance;
    }

    public void analyzeDataset(String projName) {
        String datasetPath = String.format("./output/%s/arff/%s-datasetA.arff", projName, projName);

        File analysisResults = new File(String.format("./output/%s/%s-results.csv", projName, projName));
        String header = "dataset,classifier,type,TP,FP,TN,FN,Precision,Recall,AUC,F1,MCC,Kappa\n";

        String[] classifiers = {RANDOM_FOREST, NAIVE_BAYES, IBK};

        DataSource source;
        Instances data;
        try {
            source = new DataSource(datasetPath);
            data = source.getDataSet();
        } catch (Exception e) {
            LOGGER.error("Error loading data source", e);
            return;
        }

        if (data.classIndex() == -1)
            data.setClassIndex(data.numAttributes() - 1);

        try (FileWriter outputFile = new FileWriter(analysisResults)) {
            outputFile.write(header);
            try (CSVWriter writer = new CSVWriter(outputFile)) {
                for (String classifierName : classifiers) {
                    LOGGER.info("Starting analysis with {}", classifierName);
                    analyze(projName, classifierName, data, writer);
                }
            }
        } catch (Exception e) {
            LOGGER.warn(e.getMessage());
        }

        CorrelationAttributeEval corrEval = new CorrelationAttributeEval();
        try {
            corrEval.buildEvaluator(data);
            Map<String, Double> actionableRanking = new HashMap<>();
            for (int i = 3; i < data.numAttributes() - 1; i++) {
                actionableRanking.put(data.attribute(i).name(), corrEval.evaluateAttribute(i));
            }
            LOGGER.info("Actionable ranking: {}", actionableRanking);
        } catch (Exception e) {
            LOGGER.error("Error building evaluator", e);
        }
    }

    private void analyze(String projName, String classifierName, Instances data,
                         CSVWriter writer) throws Exception {
        Classifier classifier = getClassifier(classifierName);
        Evaluation eval = new Evaluation(data);
        eval.crossValidateModel(classifier, data, 10, new Debug.Random(42));

        String[] weighted = {
                projName,
                classifierName,
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
                projName,
                classifierName,
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

    private Classifier getClassifier(String classifierName) {
        if (classifierName.equals(RANDOM_FOREST)) {
            return new RandomForest();
        } else if (classifierName.equals(NAIVE_BAYES)) {
            return new NaiveBayes();
        } else {
            return new IBk();
        }
    }
}