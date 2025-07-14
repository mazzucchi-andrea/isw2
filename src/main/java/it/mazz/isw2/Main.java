package it.mazz.isw2;

import it.mazz.isw2.ml.Analysis;
import it.mazz.isw2.ml.ModifiedDatasetsAnalysis;
import it.mazz.isw2.ml.RefactorAnalysis;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Main {

    private static final Logger LOGGER = LogManager.getLogger(Main.class);

    static {

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy-hh-mm-ss");
        System.setProperty("current.date.time", dateFormat.format(new Date()));
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            LOGGER.error("Operation and project are mandatory");
            return;
        }

        String projName = args[1];
        if (!(projName.equals("BOOKKEEPER") || projName.equals("OPENJPA"))) {
            LOGGER.error("Invalid project");
            return;
        }

        switch (args[0]) {
            case "Dataset" -> {
                Double percent = Double.parseDouble(args[2]);
                DatasetGenerator.getInstance().generateDataset(projName, percent);
            }

            case "Analysis" -> Analysis.getInstance().analyzeDataset(projName);

            case "RefactorAnalysis" -> RefactorAnalysis.getInstance().analyze(projName, args[2], Integer.parseInt(args[3]), args[4]);

            case "ModifiedDatasets" -> ModifiedDatasetsGenerator.getInstance().generateDatasets(projName);

            case "ModifiedAnalysis" -> {
                String classifierName = args[2];
                ModifiedDatasetsAnalysis.getInstance().analyzeDatasets(projName, classifierName);
            }

            default -> throw new IllegalStateException("Unexpected value: " + args[0]);
        }
    }
}
