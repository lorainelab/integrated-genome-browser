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
     * A very simple main method that shows the JFileChooser right away.
     * When the user clicks the Cancel or Open button, the JFileChooser
     * goes away and the program exits. This behavior is designed for
     * testing and programmer convenience. 
     * @author Ann Loraine
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
