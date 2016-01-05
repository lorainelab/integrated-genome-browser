package org.lorainelab.igb.snp.convert;

import java.io.File;
import java.io.IOException;
import javax.swing.JTextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author dcnorris
 */
public class SNPConverterActionTest {

    private static final Logger logger = LoggerFactory.getLogger(SNPConverterActionTest.class);
    private static final String INPUT_FILE_NAME = "sample23andMe.txt";

    String inputFileNamePath;
    String outputDirectory;
    String outputFileName;

//    @Before // -- uncomment to enable test, but this is too expensive to routinely run on the build server
    public void setup() {
        inputFileNamePath = SNPConverterActionTest.class.getClassLoader().getResource(INPUT_FILE_NAME).getFile();
        outputDirectory = inputFileNamePath.substring(0, inputFileNamePath.lastIndexOf(File.separator));
        outputFileName = "output";
        File outputFile = new File(outputDirectory + File.separator + outputFileName);
        if (outputFile.exists()) {
            outputFile.delete();
        }
    }

//    @Test // -- uncomment to enable test, but this is too expensive to routinely run on the build server
    public void conversionWithReferenceTest() throws IOException {

        JTextArea mockProgress = new JTextArea();
        new SnpConverterAction(mockProgress)
                .convertStart(inputFileNamePath, outputDirectory, outputFileName, true);

    }

}
