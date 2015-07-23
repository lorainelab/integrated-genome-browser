/*
 * A testing class to check whether ProtAnnot XML files can be read by
 * ProtAnnot
 */
package org.bioviz.protannot;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.util.GeneralUtils;
import java.io.BufferedInputStream;
import java.io.File;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bioviz.protannot.model.ProtannotParser;

/**
 * This class opens a directory and then attempts to read every file in the
 * directory that terminates with file extension suffix .paxml. Use this
 * class to test whether each .paxml file in the directory is readable by
 * the ProtAnnot Xml2GenometryParser class. Files that are not readable are
 * moved into directory failed (created if not present).
 *
 * @author hvora1
 * @author loraine
 */
public class TestSamples {

    /**
     * Usage: java -cp protannot_exe.jar org.bioviz.protannot.TestSamples [dir_path]
     * You can build protannot_exe.jar by running ant exe in the top-level genoviz
     * trunk directory.
     *
     * @param args
     */
    static public void main(String args[]) {

//        if (args.length != 1) {
//            printUsage();
//            return;
//        }
//
//        String dirpath = args[0];
//        if (!dirpath.endsWith("/")) {
//            dirpath += "/";
//        }
//
//        File dir = new File(dirpath);
//        String[] files = dir.list(new FilenameFilter() {
//
//            @Override
//            public boolean accept(File dir, String name) {
//                return GeneralUtils.getUnzippedName(name).endsWith(".paxml");
//            }
//        });
//
//        System.out.println("Total files " + files.length);
//        for (String s : files) {
//            if (testFile(dirpath + s)) {
//                System.out.println(s + " read sucessfully.");
//            } else {
//                System.out.println("Error reading " + s);
//            }
//        }
        URL resource = TestSamples.class.getClassLoader().getResource("/diff_motifs/STARD3.sample.paxml");
        
        testFile(resource.getFile());

    }

    /**
     * Test whether the given file can be read into ProtAnnot data models.
     *
     * @param filename	the name of the file to test
     * @return	boolean	true if ProtAnnot can read the file, false if not
     */
    static private boolean testFile(String filename) {
        BufferedInputStream bistr = null;
        try {
            bistr = new BufferedInputStream(GeneralUtils.getInputStream(new File(filename), new StringBuffer()));
            try {
                //NormalizeXmlStrand nxs = new NormalizeXmlStrand(bistr);
                //NormalizeXmlStrand.outputXMLToScreen(nxs.doc);
                ProtannotParser parser = new ProtannotParser();

                BioSeq seq = parser.parse(bistr);
                if (seq != null) {
                    GenomeView gview = new GenomeView();
                    gview.setBioSeq(seq, true);
                    return true;
                }
            } catch (Exception ex) {
                Logger.getLogger(TestSamples.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (Exception ex) {
            System.out.println(filename + "File not found");
        }
        moveToFailedDir(filename);
        return false;
    }

    /**
     * Moves given file to directory named failed.
     *
     * @param filename	File to be moved
     * @return boolean true if file was move successfully, false if not.
     */
    static private boolean moveToFailedDir(String filename) {
        File file = new File(filename);
        File dir = new File(file.getParentFile().getPath() + "/failed");
        if (!dir.exists()) {
            dir.mkdir();
        }
        File newFile = new File(dir.getPath() + "/" + file.getName());
        if (newFile.exists()) {
            newFile.delete();
        }
        return file.renameTo(newFile);
    }

    /**
     * Prints how to use the tool.
     */
    static private void printUsage() {
        System.err.println("Usage: java -cp protannot_exe.jar org.bioviz.protannot.TestSamples [dir_path]");
    }
}
