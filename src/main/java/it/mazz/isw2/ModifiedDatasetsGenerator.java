package it.mazz.isw2;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;
import it.mazz.isw2.entities.Features;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModifiedDatasetsGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModifiedDatasetsGenerator.class);

    private static final String ARFF_HEADER = """
            @relation %s_%s
            
            @attribute nAuth numeric
            @attribute methodHistories numeric
            @attribute loc numeric
            @attribute fanin numeric
            @attribute fanout numeric
            @attribute wmc numeric
            @attribute returns numeric
            @attribute loops numeric
            @attribute comparison numeric
            @attribute maxNested numeric
            @attribute math numeric
            @attribute smells numeric
            @attribute buggy {yes,no}
            
            @data
            """;

    private static final String CSV_HEADER = "#,Version,fileName,methodName,nAuth,methodHistories,loc,fain,fanout," +
            "wmc,returns,loops,comparison,maxNested,math,smells,buggy\n";

    private static ModifiedDatasetsGenerator instance = null;

    private ModifiedDatasetsGenerator() {
    }

    public static ModifiedDatasetsGenerator getInstance() {
        if (instance == null)
            instance = new ModifiedDatasetsGenerator();
        return instance;
    }

    public void generateDatasets(String projName) {
        List<Features> featuresList = getFeatures(projName);
        generateC(projName, featuresList);
        generateBplus(projName, featuresList);
        generateB(projName, featuresList);
    }

    private List<Features> getFeatures(String projName) {
        String csvDatasetPath = String.format("./output/%s/csv/%s-datasetA.csv/", projName, projName);
        List<Features> featuresList = new ArrayList<>();
        try (CSVReader csvReader = new CSVReader(Files.newBufferedReader(Path.of(csvDatasetPath)))) {
            String[] line;
            csvReader.readNext();
            while ((line = csvReader.readNext()) != null) {
                featuresList.add(new Features(line));
            }
        } catch (CsvValidationException | IOException e) {
            return Collections.emptyList();
        }
        return featuresList;
    }

    private void generateBplus(String projName, List<Features> featuresList) {
        List<Features> bPlusFeatures = new ArrayList<>();
        for (Features features : featuresList) {
            if (features.getSmells() > 0) {
                bPlusFeatures.add(features);
            }
        }
        File arffDataset = new File(String.format("./output/%s/arff/%s-datasetB+.arff/", projName, projName));
        try (FileWriter outputFile = new FileWriter(arffDataset)) {
            outputFile.write(String.format(ARFF_HEADER, projName, "B+"));
            writeARFFDataset(outputFile, bPlusFeatures);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            return;
        }
        File csvDataset = new File(String.format("./output/%s/csv/%s-datasetB+.csv/", projName, projName));
        try (FileWriter outputFile = new FileWriter(csvDataset)) {
            outputFile.write(CSV_HEADER);
            writeCSVDataset(outputFile, bPlusFeatures);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }

    private void generateB(String projName, List<Features> featuresList) {
        List<Features> bFeatures = new ArrayList<>();
        for (Features features : featuresList) {
            if (features.getSmells() > 0) {
                bFeatures.add(features);
            }
        }
        for (Features features : bFeatures) {
            if (features.getSmells() > 0) {
                features.setSmells(0);
            }
        }
        File arffDataset = new File(String.format("./output/%s/arff/%s-datasetB.arff/", projName, projName));
        try (FileWriter outputFile = new FileWriter(arffDataset)) {
            outputFile.write(String.format(ARFF_HEADER, projName, "B"));
            writeARFFDataset(outputFile, bFeatures);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            return;
        }
        File csvDataset = new File(String.format("./output/%s/csv/%s-datasetB.csv/", projName, projName));
        try (FileWriter outputFile = new FileWriter(csvDataset)) {
            outputFile.write(CSV_HEADER);
            writeCSVDataset(outputFile, bFeatures);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }

    private void generateC(String projName, List<Features> featuresList) {
        List<Features> cFeatures = new ArrayList<>();
        for (Features features : featuresList) {
            if (features.getSmells() == 0) {
                cFeatures.add(features);
            }
        }
        File arffDataset = new File(String.format("./output/%s/arff/%s-datasetC.arff/", projName, projName));
        try (FileWriter outputFile = new FileWriter(arffDataset)) {
            outputFile.write(String.format(ARFF_HEADER, projName, "C"));
            writeARFFDataset(outputFile, cFeatures);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            return;
        }
        File csvDataset = new File(String.format("./output/%s/csv/%s-datasetC.csv/", projName, projName));
        try (FileWriter outputFile = new FileWriter(csvDataset)) {
            outputFile.write(CSV_HEADER);
            writeCSVDataset(outputFile, cFeatures);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }

    private void writeARFFDataset(FileWriter outputFile, List<Features> featuresList) throws IOException {
        try (CSVWriter writer = new CSVWriter(outputFile)) {
            for (Features features : featuresList) {
                writer.writeNext(features.toStringArrayForArff());
            }
        }
    }

    private void writeCSVDataset(FileWriter outputFile, List<Features> featuresList) throws IOException {
        try (CSVWriter writer = new CSVWriter(outputFile)) {
            for (Features features : featuresList) {
                writer.writeNext(features.toStringArrayForCSV());
            }
        }
    }
}
