/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package guitest;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.JFrame;

/**
 *
 * @author aloraine
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // why do we need this?
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                UIManager.put("swing.boldMetal", Boolean.FALSE);
                createAndShowGUI();
            }
        });
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("FileChooserDemo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.add(new FileChooserDemo());

        frame.pack();
        frame.setVisible(true);
    }
}
