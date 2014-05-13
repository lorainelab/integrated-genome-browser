package com.gene.sampleselection;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.affymetrix.igb.swing.JRPButton;
import com.affymetrix.igb.osgi.service.IGBService;
import com.affymetrix.igb.osgi.service.IGBTabPanel;

public class SampleSelectionView extends IGBTabPanel {

    private static final long serialVersionUID = 1L;
    public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("selection");

    private JLabel title;
    private SampleTableModel tm;
    private JTable sampleTable;
    private JLabel typePrompt;
    private JComboBox typeBox;
    private JRPButton submitButton;
    private String name;
    private List<String> samples;
    private boolean selectionProcessing;
    private SampleSelectionCallback callback;
    private Map<String, List<String>> selections; // key is type, value is list of samples

    public SampleSelectionView(IGBService igbService) {
        super(igbService, BUNDLE.getString("viewTab"), BUNDLE.getString("viewTab"), "", false);
        selectionProcessing = false;
        setLayout(new BorderLayout());
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        title = new JLabel();
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        titlePanel.add(title);
        add("North", titlePanel);
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
        typePrompt = new JLabel();
        bottomPanel.add(typePrompt);
        bottomPanel.add(new JLabel(" "));
        typeBox = new JComboBox();
        typeBox.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        selectType((String) typeBox.getSelectedItem());
                    }
                }
        );
        bottomPanel.add(typeBox);
        bottomPanel.add(new JLabel("        "));
        final JRadioButton separateButton = new JRadioButton(BUNDLE.getString("separateTracks"));
        final JRadioButton joinButton = new JRadioButton(BUNDLE.getString("joinTracks"));
        ButtonGroup group = new ButtonGroup();
        group.add(separateButton);
        group.add(joinButton);
        separateButton.setSelected(true);
        bottomPanel.add(separateButton);
        bottomPanel.add(joinButton);
        bottomPanel.add(new JLabel("  "));
        submitButton = new JRPButton("SampleSelectionView_submitButton", BUNDLE.getString("submit"));
        submitButton.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (callback != null) {
                            callback.select(name, separateButton.isSelected(), selections);
                        }
                    }
                }
        );
        bottomPanel.add(submitButton);
        add("South", bottomPanel);
        tm = new SampleTableModel();
        sampleTable = new JTable(tm);
        sampleTable.getSelectionModel().addListSelectionListener(new RowListener());
        JScrollPane jScrollPane = new JScrollPane(sampleTable);
        add("Center", jScrollPane);
    }

    public void setData(String titleTextPattern, String name, List<String> types, List<String> samples,
            Map<String, List<String>> selections, SampleSelectionCallback callback) {
        String titleText = MessageFormat.format(titleTextPattern, name);
        title.setText(titleText);
        typePrompt.setText(BUNDLE.getString("variable"));
        typeBox.removeAllItems();
        for (String type : types) {
            typeBox.addItem(type);
        }
        typeBox.setSelectedIndex(0);
        typeBox.setMaximumSize(new Dimension(typeBox.getPreferredSize().width, typeBox.getPreferredSize().height));
        selectionProcessing = true;
        this.samples = samples;
        tm.setSamples(samples);
        tm.fireTableDataChanged();
        sampleTable.invalidate();
        sampleTable.repaint();
        selectionProcessing = false;
        this.name = name;
        this.callback = callback;
        this.selections = selections;
        submitButton.setEnabled(true);
    }

    public void clear() {
        title.setText("");
        typePrompt.setText("");
        typeBox.removeAllItems();
        selectionProcessing = true;
        tm.setSamples(new ArrayList<String>());
        selectionProcessing = false;
        name = null;
        callback = null;
        selections = null;
        submitButton.setEnabled(false);
    }

    private void clearSamplesForType() {
        selectionProcessing = true;
        sampleTable.getSelectionModel().clearSelection();
        selectionProcessing = false;
    }

    private void selectType(String type) {
        clearSamplesForType();
        selectionProcessing = true;
        if (selections != null) {
            List<String> sampleList = selections.get(type);
            if (sampleList != null) {
                for (String sample : sampleList) {
                    int index = samples.indexOf(sample);
                    if (index > -1) {
                        sampleTable.getSelectionModel().addSelectionInterval(index, index);
                    }
                }
            }
        }
        selectionProcessing = false;
    }

    private class RowListener implements ListSelectionListener {

        public void valueChanged(ListSelectionEvent event) {
            if (selectionProcessing) {
                return;
            }
            if (event.getValueIsAdjusting()) {
                return;
            }
            String type = (String) typeBox.getSelectedItem();
            List<String> sampleList = new ArrayList<String>();
            for (int index : sampleTable.getSelectedRows()) {
                sampleList.add(samples.get(index));
            }
            selections.put(type, sampleList);
        }
    }
}
