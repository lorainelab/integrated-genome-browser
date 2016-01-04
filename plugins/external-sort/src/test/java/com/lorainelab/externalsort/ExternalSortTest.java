/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.externalsort;

import com.google.code.externalsorting.ExternalMergeSort;
import com.google.common.base.Stopwatch;
import org.lorainelab.igb.externalsort.api.ComparatorMetadata;
import org.lorainelab.igb.externalsort.api.ExternalSortConfiguration;
import org.lorainelab.igb.externalsort.api.ExternalSortService;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jeckstei
 */
public class ExternalSortTest {

    private static final Logger logger = LoggerFactory.getLogger(ExternalSortTest.class);

    private static final String TEST1_FILE = "test1.txt";
    private static final String TEST1_FILE_ANSWER = "test1.answer.txt";


    private File file1;
    private List<File> fileList;

    private final ExternalSortService exsort = new ExternalMergeSort();

    private static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;

        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

    @Before
    public void setUp() throws Exception {
        this.fileList = new ArrayList<>(1);
        this.file1 = new File(this.getClass().getClassLoader()
                .getResource(TEST1_FILE).toURI());

        File tmpFile1 = new File(this.file1.getPath() + ".tmp");

        copyFile(this.file1, tmpFile1);

        this.fileList.add(tmpFile1);
    }

    /**
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        this.file1 = null;
        for (File f : this.fileList) {
            f.delete();
        }
        this.fileList.clear();
        this.fileList = null;
    }

    @Test
    public void simpleTest() throws IOException, URISyntaxException {

        ExternalSortConfiguration conf = new ExternalSortConfiguration();
        conf.setNumHeaderRows(1);
        conf.setMaxMemoryInBytes(10_000_000);
        conf.setMaxTmpFiles(100);

        ComparatorMetadata comparatorMetadata = new ComparatorMetadata();

        //Define multisort
        //First sort
        comparatorMetadata.getPreparers().add(s -> {
            String[] sSplit = s.split("\\s+");
            return sSplit[0];
        });
        //Second sort
        comparatorMetadata.getPreparers().add(s -> {
            String[] sSplit = s.split("\\s+");
            return Long.parseLong(sSplit[1]);
        });
        //Third sort
        comparatorMetadata.getPreparers().add(s -> {
            String[] sSplit = s.split("\\s+");
            return Long.parseLong(sSplit[2]);
        });

        Stopwatch watch = Stopwatch.createStarted();
        Optional<File> output = exsort.merge(this.file1, file1.getName(), comparatorMetadata, conf);
        logger.info(watch.stop().toString());
        
        ArrayList<String> result = readLines(output.get());
        ArrayList<String> answer = readLines(new File(this.getClass().getClassLoader()
                .getResource(TEST1_FILE_ANSWER).toURI()));
        Assert.assertEquals(result.size(), answer.size());
        for (int i = 0; i < result.size(); i++) {
            String actualLine = result.get(i);
            String answerLine = answer.get(i);
            try {
                Assert.assertTrue(isLinesEqual(actualLine, answerLine));
            } catch (Exception ex) {
                Assert.fail();
            }

        }
       
    }

    private boolean isLinesEqual(String line1, String line2) {
        String[] line1Words = line1.split("\\s+");
        String[] line2Words = line2.split("\\s+");
        for (int i = 0; i < line1Words.length; i++) {
            if (!line1Words[i].equals(line2Words[i])) {
                return false;
            }
        }
        return true;
    }

    private ArrayList<String> readLines(File f) throws IOException {
        BufferedReader r = new BufferedReader(new FileReader(f));
        ArrayList<String> answer = new ArrayList<>();
        String line;
        while ((line = r.readLine()) != null) {
            answer.add(line);
        }
        r.close();
        return answer;
    }

}
