package it.mazz.isw2;

import it.mazz.isw2.ml.Analysis;
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
            LOGGER.error("project and versions portion is mandatory");
            return;
        }
        String projName = args[0];
        Double percent = Double.parseDouble(args[1]);
        try {
            String skipDatasetGeneration = args[2];
            if (!skipDatasetGeneration.equals("-skip"))
                DatasetGenerator.getInstance().generateDataset(projName, percent);
        } catch (ArrayIndexOutOfBoundsException ignore) {
            DatasetGenerator.getInstance().generateDataset(projName, percent);
        }

        Analysis.getInstance().analyzeDataset(projName);
    }
}
