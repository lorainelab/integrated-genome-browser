package com.lorainelab.image.exporter;

import com.affymetrix.genometry.util.PreferenceUtils;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.event.ChangeEvent;
import net.miginfocom.swing.MigLayout;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author dcnorris
 */
public class GuiBuildTest extends JPanel {

    private static final String TITLE = "Save Image";
    private JFrame exportDialogFrame;

    @Before
    public void setup() {
        setLayout(new MigLayout("", "[grow]", "[][][grow]"));
        exportDialogFrame = PreferenceUtils.createFrame(TITLE, new Dimension(600, 400));
        exportDialogFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        exportDialogFrame.add(this);
        addMainPanel();
        addImageOptionsPanel();
        addPreviewPanel();
    }

    private void addMainPanel() {
        JComboBox extComboBox = new JComboBox();
        extComboBox.addActionListener((java.awt.event.ActionEvent evt) -> {
//            exportDialog.extComboBoxActionPerformed();
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener((java.awt.event.ActionEvent evt) -> {
//            exportDialog.cancelButtonActionPerformed();
        });
        JButton saveButton = new JButton("Save" + "\u2026");
        saveButton.addActionListener((java.awt.event.ActionEvent evt) -> {
//            exportDialog.browseButtonActionPerformed();
        });
        JPanel panel = new JPanel(new MigLayout("", "[grow][][]", "[]"));
        panel.add(extComboBox, "growx");
        panel.add(cancelButton);
        panel.add(saveButton);
        add(panel, "growx, wrap");
    }

    private void addImageOptionsPanel() {
        JLabel widthLabel = new JLabel("Width:");
        JSpinner widthSpinner = new JSpinner();
        widthSpinner.addChangeListener((ChangeEvent evt) -> {
//                widthSpinnerStateChanged(evt);
        });
        JLabel heightLabel = new JLabel("Height:");
        JSpinner heightSpinner = new JSpinner();
        heightSpinner.addChangeListener((ChangeEvent evt) -> {
            // heightSpinnerStateChanged(evt);
        });
        JLabel unitLabel = new JLabel("Unit:");
        JComboBox unitComboBox = new JComboBox();
        unitComboBox.addActionListener((ActionEvent evt) -> {
            //unitComboBoxActionPerformed(evt);
        });
        JLabel sizeLabel = new JLabel("3 X 2 inches");
        JLabel resolutionLabel = new JLabel("Resolution:");
        JComboBox resolutionComboBox = new JComboBox();
        resolutionComboBox.addActionListener((ActionEvent evt) -> {
            // resolutionComboBoxActionPerformed(evt);
        });
        JPanel panel = new JPanel(new MigLayout("", "[]rel[grow][]rel[grow][]rel[grow]", "[][]"));
        panel.setBorder(BorderFactory.createTitledBorder("Image Size"));
        panel.add(widthLabel);
        panel.add(widthSpinner, "growx");
        panel.add(heightLabel, "right");
        panel.add(heightSpinner, "growx");
        panel.add(unitLabel, "");
        panel.add(unitLabel, "");
        panel.add(unitComboBox, "growx, wrap");
        panel.add(sizeLabel, "span 2, shrink 1, center");
        panel.add(resolutionLabel, "gap related");
        panel.add(resolutionComboBox, "growx");
        add(panel, "growx, wrap");
    }

    private void addPreviewPanel() {
        ButtonGroup buttonGroup = new ButtonGroup();
        JRadioButton svRadioButton = new JRadioButton("Sliced View (with Labels)");
        svRadioButton.addActionListener((java.awt.event.ActionEvent evt) -> {
//                svRadioButtonActionPerformed(evt);
        });
        JRadioButton mvRadioButton = new JRadioButton("Main View");
        mvRadioButton.addActionListener((java.awt.event.ActionEvent evt) -> {
//                mvRadioButtonActionPerformed(evt);
        });
        JRadioButton wfRadioButton = new JRadioButton("Whole Frame");
        wfRadioButton.addActionListener((java.awt.event.ActionEvent evt) -> {
//                wfRadioButtonActionPerformed(evt);
        });
        JRadioButton mvlRadioButton = new JRadioButton("Main View (with Labels)");
        mvlRadioButton.addActionListener((java.awt.event.ActionEvent evt) -> {
//                mvlRadioButtonActionPerformed(evt);
        });
        buttonGroup.add(svRadioButton);
        buttonGroup.add(mvRadioButton);
        buttonGroup.add(wfRadioButton);
        buttonGroup.add(mvlRadioButton);

        JLabel previewLabel = new JLabel("test");

        JButton refreshButton = new javax.swing.JButton("Update Preview Image");
        refreshButton.setToolTipText("Click to update Preview and image dimensions after changing IGB.");

        refreshButton.addActionListener((java.awt.event.ActionEvent evt) -> {
//            refreshButtonActionPerformed(evt);
        });

        JPanel previewPanel = new JPanel(new MigLayout("flowx, debug", "[][grow]", "[grow][grow][grow][grow][grow]"));
        previewPanel.setBorder(BorderFactory.createTitledBorder("Preview"));
        previewPanel.add(wfRadioButton, "");
        previewPanel.add(previewLabel, "span 1 5, grow, wrap");
        previewPanel.add(mvRadioButton, "wrap");
        previewPanel.add(mvlRadioButton, "wrap");
        previewPanel.add(svRadioButton, "wrap");
        previewPanel.add(refreshButton, "");

        add(previewPanel, "grow");
    }

    @Test
    public void seeDesign() throws InterruptedException {
        exportDialogFrame.setVisible(true);
        while (true) {
            Thread.sleep(20000);
        }
    }

}
