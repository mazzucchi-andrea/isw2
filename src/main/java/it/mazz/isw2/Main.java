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
        if (args.length < 1) {
            LOGGER.error("project is mandatory");
            return;
        }
        String projName = args[0];
        try {
            String skipDatasetGeneration = args[1];
            if (!skipDatasetGeneration.equals("-skip"))
                DatasetGenerator.getInstance().generateDataset(projName);
        } catch (ArrayIndexOutOfBoundsException ignore) {
            DatasetGenerator.getInstance().generateDataset(projName);
        }

        Analysis.getInstance().analyzeDataset(projName);
    }
}
