/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.snp.convert.mapmaker;

import com.google.common.base.Splitter;
import static com.lorainelab.snp.convert.SnpConverterAction.IGB_HACK_FAKE_SNP;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for the creation of an off-heap map that allows for
 * the quick lookup of SNP positions for the GRCh38 human reference genome.
 *
 * @author Daniel
 */
public class MapMaker {

    private static final Logger logger = LoggerFactory.getLogger(MapMaker.class);

    private static final int MAX_SNP_ID_SIZE = 80338934;

    /**
     * This method takes a map as a parameter and, if it is empty, loads the
     * data
     * from the GRCh38.txt file located in src/main/resources. The method checks
     * to see if the map is empty due to the fact that the off-heap map has the
     * ability to persist between runs.
     *
     * @param grch38 the reference to the map containing the SNP position
     * information.
     * @throws IOException
     */
    public void loadReferenceData(Map grch38) throws IOException {
        if (grch38.isEmpty()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(MapMaker.class.getResource("GRCh38.txt").getFile()))) {
                for (String nextLine = reader.readLine(); nextLine != null; nextLine = reader.readLine()) {
                    String[] tokens = nextLine.split("\\t");
                    grch38.put(tokens[0], tokens[2]);
                }
            }
        }
    }

    public String[][] initializeData() {
        logger.info("Starting data initilization");
        final String[][] data = new String[MAX_SNP_ID_SIZE][2];

        try {
            InputStream is = MapMaker.class.getClassLoader().getResourceAsStream("GRCh38.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            for (String nextLine = reader.readLine(); nextLine != null; nextLine = reader.readLine()) {
                List<String> tokens = Splitter.on("\t").trimResults().omitEmptyStrings().splitToList(nextLine);
                int row = calculateRow(data, tokens.get(0));
                if (row != -1) {
                    data[row][0] = tokens.get(0);
                    data[row][1] = tokens.get(2);
                }
            }
        } catch (IOException | NumberFormatException ex) {
            logger.error("IOException while creating reference data", ex);
        }
        logger.info("Completed data initilization");
        return data;
    }

    public static int calculateRow(String[][] data, String snpId) throws NumberFormatException {
        if (snpId.equals(IGB_HACK_FAKE_SNP)) {
            return -1;
        }
        int row = Integer.parseInt(snpId.substring(2)) % MAX_SNP_ID_SIZE;

        return row;
    }

    public MapMaker() {
    }

}
