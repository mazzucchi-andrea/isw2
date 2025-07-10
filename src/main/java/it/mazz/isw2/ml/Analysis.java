package it.mazz.isw2.ml;

import com.opencsv.CSVWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        String datasetPath = String.format("./output/%s/%s-dataset.arff", projName, projName);

        File analysisResults = new File(String.format("./output/%s/%s-results.csv", projName, projName));
        String header = "dataset,classifier,TP,FP,TN,FN,Precision,Recall,AUC,Kappa\n";

        String[] classifiers = {RANDOM_FOREST, NAIVE_BAYES, IBK};

        try (FileWriter outputFile = new FileWriter(analysisResults)) {
            outputFile.write(header);
            try (CSVWriter writer = new CSVWriter(outputFile)) {
                for (String classifierName : classifiers) {
                    LOGGER.info("Starting analysis with {}", classifierName);

                    Classifier c = getClassifier(classifierName);

                    analyze(projName, c, classifierName, datasetPath, writer);
                }
            }
        } catch (Exception e) {
            LOGGER.warn(e.getMessage());
        }
    }

    private void analyze(String projName, Classifier classifier, String classifierName, String datasetPath,
                         CSVWriter writer) throws Exception {

        DataSource source = new DataSource(datasetPath);
        Instances data = source.getDataSet();

        if (data.classIndex() == -1)
            data.setClassIndex(data.numAttributes() - 1);

        Evaluation eval = new Evaluation(data);
        eval.crossValidateModel(classifier, data, 10, new Debug.Random(42));

        String[] line = {
                projName,
                classifierName,
                Double.toString(eval.numTruePositives(1)), Double.toString(eval.numFalsePositives(1)),
                Double.toString(eval.numTrueNegatives(1)), Double.toString(eval.numFalseNegatives(1)),
                Double.toString(eval.precision(1)), Double.toString(eval.recall(1)),
                Double.toString(eval.areaUnderROC(1)), Double.toString(eval.kappa())};

        writer.writeNext(line);
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