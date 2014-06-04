/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.IGB;
import static com.affymetrix.igb.IGBConstants.APP_NAME;
import static com.affymetrix.igb.IGBConstants.APP_VERSION;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.swing.JRPButton;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.MessageFormat;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * Open a window showing information about Integrated Genome Browser.
 *
 * @author sgblanch
 */
public class AboutIGBAction extends GenericAction {

    private static final long serialVersionUID = 1l;
    private static final AboutIGBAction ACTION = new AboutIGBAction();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static AboutIGBAction getAction() {
        return ACTION;
    }

    private AboutIGBAction() {
        super(MessageFormat.format(BUNDLE.getString("about"), APP_NAME), null,
                "16x16/actions/about_igb.png",
                "22x22/actions/about_igb.png",
                KeyEvent.VK_A, null, true);
        this.ordinal = 100;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        JPanel message_pane = new JPanel();
        message_pane.setLayout(new BoxLayout(message_pane, BoxLayout.Y_AXIS));
        JTextArea about_text = new JTextArea();
        about_text.setEditable(false);
        String text = APP_NAME + ", version: " + APP_VERSION + "\n\n"
                + "IGB (pronounced ig-bee) is a product of the open source Genoviz project,\n"
                + "which develops interactive visualization software for genomics.\n\n"
                + "If you use IGB to create images for publication, please cite the IGB\n"
                + "Applications Note:\n\n"
                + "Nicol JW, Helt GA, Blanchard SG Jr, Raja A, Loraine AE.\n"
                + "The Integrated Genome Browser: free software for distribution and exploration of\n"
                + "genome-scale datasets.\n"
                + "Bioinformatics. 2009 Oct 15;25(20):2730-1.\n\n"
                + "For more details, including license information, see:\n"
                + "\thttp://www.bioviz.org/igb\n"
                + "\thttp://genoviz.sourceforge.net\n\n";
        about_text.append(text);
        String cache_root = com.affymetrix.genometryImpl.util.LocalUrlCacher.getCacheRoot();
        File cache_file = new File(cache_root);
        if (cache_file.exists()) {
            about_text.append("\nCached data stored in: \n");
            about_text.append("  " + cache_file.getAbsolutePath() + "\n");
        }
        String data_dir = PreferenceUtils.getAppDataDirectory();
        if (data_dir != null) {
            File data_dir_f = new File(data_dir);
            about_text.append("\nApplication data stored in: \n  "
                    + data_dir_f.getAbsolutePath() + "\n");
        }

        message_pane.add(new JScrollPane(about_text));
        JRPButton igb_paper = new JRPButton("AboutIGBAction_igb_paper", "View IGB Paper");
        JRPButton bioviz_org = new JRPButton("AboutIGBAction_bioviz_org", "Visit Bioviz.org");
        JRPButton noticesButton = new JRPButton("Notices", "View Notices");
		// vikram JButton request_feature = new JButton("Request a Feature");
        // vikram JButton report_bug = new JButton("Report a Bug");
        noticesButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                String contents;
                try {
                    contents = readFile("NOTICES.txt");
                } catch (IOException ex) {
                    contents = "";
                }
                JTextArea textBox = new javax.swing.JTextArea("", 20, 50);
                textBox.setLineWrap(true);
                textBox.setText(contents);
                javax.swing.JScrollPane scrollpane = new javax.swing.JScrollPane(textBox);
                scrollpane.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                Object[] msg = {"Details:", scrollpane};
                javax.swing.JOptionPane op = new javax.swing.JOptionPane(
                        msg,
                        javax.swing.JOptionPane.PLAIN_MESSAGE,
                        javax.swing.JOptionPane.PLAIN_MESSAGE,
                        null,
                        null);
                javax.swing.JDialog dialog = op.createDialog("Notices.txt");
                dialog.setVisible(true);
                dialog.setPreferredSize(new java.awt.Dimension(650, 650));
                dialog.setDefaultCloseOperation(javax.swing.JDialog.HIDE_ON_CLOSE);
                dialog.setAlwaysOnTop(true);
                dialog.setResizable(true);
                dialog.pack();
            }
        });

        igb_paper.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                GeneralUtils.browse("http://www.ncbi.nlm.nih.gov/pmc/articles/PMC2759552/?tool=pubmed");
            }
        });
        bioviz_org.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                GeneralUtils.browse("http://www.bioviz.org");
            }
        });

        JPanel buttonP = new JPanel(new GridLayout(1, 3));
        buttonP.add(igb_paper);
        buttonP.add(bioviz_org);
        buttonP.add(noticesButton);

        message_pane.add(buttonP);

        final JOptionPane pane = new JOptionPane(message_pane, JOptionPane.INFORMATION_MESSAGE,
                JOptionPane.DEFAULT_OPTION);
        final JDialog dialog = pane.createDialog(IGB.getSingleton().getFrame(), MessageFormat.format(BUNDLE.getString("about"), APP_NAME));
        dialog.setVisible(true);
    }

    private String readFile(String file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = null;
        StringBuilder stringBuilder = new StringBuilder();
        String ls = System.getProperty("line.separator");
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
            stringBuilder.append(ls);
        }
        return stringBuilder.toString();
    }
}
