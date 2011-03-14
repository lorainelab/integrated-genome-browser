/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package guitest;

import java.awt.Dimension;
import java.io.File;

/**
 * Very simple class for testing JFileChooser layouts.
 * @author aloraine
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        MergeOptionChooser m = new MergeOptionChooser();
        File currDir = new File(System.getProperty("user.home"));
        m.setCurrentDirectory(currDir);
        m.rescanCurrentDirectory();
        m.setPreferredSize(new Dimension(450, 500));
        int option = m.showOpenDialog(null);
        m.closeFrame();
        System.exit(0);
    }
}
