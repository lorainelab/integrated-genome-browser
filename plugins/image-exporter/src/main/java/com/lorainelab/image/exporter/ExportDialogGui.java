package com.lorainelab.image.exporter;

import com.affymetrix.common.PreferenceUtils;
import com.affymetrix.igb.swing.JRPRadioButton;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import net.miginfocom.swing.MigLayout;

/**
 *
 * @author dcnorris
 */
public class ExportDialogGui extends JPanel {

    private static final String TITLE = "Save Image";
    private ButtonGroup buttonGroup;
    private JButton cancelButton;
    private JComboBox extComboBox;
    private JSpinner heightSpinner;
//    private JRadioButton mvRadioButton;
//    private JRadioButton mvlRadioButton;
//    private JRadioButton svRadioButton;
//    private JRadioButton wfRadioButton;
    private PreviewLabel previewLabel;
    private JButton refreshButton;
    private JComboBox resolutionComboBox;
    private JLabel resolutionLabel;
    private JLabel sizeLabel;
    private JComboBox unitComboBox;
    private JSpinner widthSpinner;
    private JButton saveAsButton;
    private JButton saveButton;
    private JTextField filePathTextField;

    public static final Object[] RESOLUTION = {72, 200, 300, 400, 500, 600, 800, 1000};
    public static final Object[] UNIT = {"pixels", "inches"};

    private final JFrame exportDialogFrame;
    private final ExportDialog controller;

    public ExportDialogGui(ExportDialog exportDialog, Map<String, Component> components) {
        this.controller = exportDialog;
        setLayout(new MigLayout("", "[grow]", "[][][grow]"));
        exportDialogFrame = PreferenceUtils.createFrame(TITLE, new Dimension(600, 520));
        exportDialogFrame.add(this);
        radioButtons = new ArrayList<>();
        addRadioButtons(components);
        addMainPanel();
        addImageOptionsPanel();
        addPreviewPanel();
        setupAutoSizePreviewLabel();
    }

    private void addMainPanel() {
        filePathTextField = new JTextField();
        extComboBox = new JComboBox();
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener((java.awt.event.ActionEvent evt) -> {
            controller.cancelButtonActionPerformed();
        });
        saveAsButton = new JButton("Save As" + "\u2026");
        saveAsButton.addActionListener((java.awt.event.ActionEvent evt) -> {
            controller.saveAsButtonActionPerformed();
        });
        saveButton = new JButton("Save");
        saveButton.addActionListener((java.awt.event.ActionEvent evt) -> {
            controller.saveButtonActionPerformed();
        });
        exportDialogFrame.getRootPane().setDefaultButton(saveButton);
        JPanel panel = new JPanel(new MigLayout("", "[grow][][]", "[]"));
        panel.add(filePathTextField, "growx");
        panel.add(saveButton);
        panel.add(cancelButton, "right, wrap");
        panel.add(extComboBox, "growx");
        panel.add(saveAsButton, "span 2,growx, wrap");
        add(panel, "growx, wrap");
    }

    private void addImageOptionsPanel() {
        JLabel widthLabel = new JLabel("Width:");
        widthSpinner = new JSpinner();
        widthSpinner.addChangeListener((ChangeEvent evt) -> {
            controller.widthSpinnerStateChanged();
        });
        JLabel heightLabel = new JLabel("Height:");
        heightSpinner = new JSpinner();
        heightSpinner.addChangeListener((ChangeEvent evt) -> {
            controller.heightSpinnerStateChanged();
        });
        JLabel unitLabel = new JLabel("Unit:");
        unitComboBox = new JComboBox(UNIT);
        sizeLabel = new JLabel();
        resolutionLabel = new JLabel("Resolution:");
        resolutionComboBox = new JComboBox(RESOLUTION);
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

    private List<JRPRadioButton> radioButtons;
    private Map<String, Component> components;

    private void addRadioButtons(Map<String, Component> components) {
        this.components = components;
        for (String key : components.keySet()) {
            JRPRadioButton rb = new JRPRadioButton(key);
            rb.setText(key);
            rb.addActionListener(evt -> {
                controller.radioButtonActionPerformed();
            });
            radioButtons.add(rb);
        }
    }

    public JRPRadioButton getRadioButton(String key) {
        for (JRPRadioButton rb : radioButtons) {
            if (rb.getId().equals(key)) {
                return rb;
            }
        }
        return null;
    }

    public JRPRadioButton getSelectedRadioButton() {
        for (JRPRadioButton rb : radioButtons) {
            if (rb.isSelected()) {
                return rb;
            }
        }
        return radioButtons.get(0);
    }

    private void addPreviewPanel() {
        buttonGroup = new ButtonGroup();
//        svRadioButton = new JRadioButton("Sliced View (with Labels)");
//        svRadioButton.addActionListener((java.awt.event.ActionEvent evt) -> {
//            controller.svRadioButtonActionPerformed();
//        });
//        mvRadioButton = new JRadioButton("Main View");
//        mvRadioButton.addActionListener((java.awt.event.ActionEvent evt) -> {
//            controller.mvRadioButtonActionPerformed();
//        });
//        wfRadioButton = new JRadioButton("Whole Frame");
//        wfRadioButton.addActionListener((java.awt.event.ActionEvent evt) -> {
//            controller.wfRadioButtonActionPerformed();
//        });
//        mvlRadioButton = new JRadioButton("Main View (with Labels)");
//        mvlRadioButton.addActionListener((java.awt.event.ActionEvent evt) -> {
//            controller.mvlRadioButtonActionPerformed();
//        });
//        buttonGroup.add(svRadioButton);
//        buttonGroup.add(mvRadioButton);
//        buttonGroup.add(wfRadioButton);
//        buttonGroup.add(mvlRadioButton);

        JPanel btnPanel = new JPanel(new MigLayout("", "[]", "[][][][][]")); // Fix this
        for (JRPRadioButton rb : radioButtons) {
            buttonGroup.add(rb);
            btnPanel.add(rb, "wrap");
        }

        buttonGroup.setSelected(radioButtons.get(0).getModel(), true);

        previewLabel = new PreviewLabel();
        refreshButton = new javax.swing.JButton("Update Preview Image");
        refreshButton.setToolTipText("Click to update Preview and image dimensions after changing IGB.");

        refreshButton.addActionListener((java.awt.event.ActionEvent evt) -> {
            controller.refreshButtonActionPerformed();
        });

        JPanel previewPanel = new JPanel(new MigLayout("fill", "[][grow]", "[]"));
        previewPanel.setBorder(BorderFactory.createTitledBorder("Preview"));

        btnPanel.add(refreshButton, "");
        JPanel imagePanel = new JPanel(new MigLayout("fill", "[]", "[]"));
        imagePanel.add(previewLabel, "grow");
        previewPanel.add(btnPanel, "top");
        previewPanel.add(imagePanel, "grow");
        add(previewPanel, "grow");
    }

    private void setupAutoSizePreviewLabel() {
        exportDialogFrame.addComponentListener(new ComponentListener() {
            @Override
            public void componentResized(ComponentEvent e) {
                previewLabel.repaint();
            }

            @Override
            public void componentMoved(ComponentEvent e) {
            }

            @Override
            public void componentShown(ComponentEvent e) {
            }

            @Override
            public void componentHidden(ComponentEvent e) {
            }
        });
    }

    public JFrame getExportDialogFrame() {
        return exportDialogFrame;
    }

    public void setFrameVisible(boolean b) {
        exportDialogFrame.setVisible(b);
    }

    public JTextField getFilePathTextField() {
        return filePathTextField;
    }

    public JComboBox getExtComboBox() {
        return extComboBox;
    }

    public JSpinner getHeightSpinner() {
        return heightSpinner;
    }

    public PreviewLabel getPreviewLabel() {
        return previewLabel;
    }

    public void setPreviewLabel(PreviewLabel previewLabel) {
        this.previewLabel = previewLabel;
    }

    public JComboBox getResolutionComboBox() {
        return resolutionComboBox;
    }

    public JLabel getResolutionLabel() {
        return resolutionLabel;
    }

    public JLabel getSizeLabel() {
        return sizeLabel;
    }

    public JComboBox getUnitComboBox() {
        return unitComboBox;
    }

    public JSpinner getWidthSpinner() {
        return widthSpinner;
    }

}
