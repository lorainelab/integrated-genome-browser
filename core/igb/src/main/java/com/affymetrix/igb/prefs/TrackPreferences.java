/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.prefs;

import com.affymetrix.genoviz.glyph.EfficientLabelledLineGlyph;
import com.affymetrix.genoviz.swing.BooleanTableCellRenderer;
import com.affymetrix.genoviz.swing.ColorTableCellRenderer;
import com.affymetrix.genoviz.swing.NumericFilter;
import com.affymetrix.igb.swing.JRPButton;
import com.affymetrix.igb.swing.JRPCheckBox;
import com.affymetrix.igb.swing.JRPTextField;
import com.affymetrix.igb.swing.jide.StyledJTable;
import com.affymetrix.igb.tiers.TrackConstants;
import com.jidesoft.combobox.ColorComboBox;
import com.jidesoft.combobox.ColorExComboBox;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.text.AbstractDocument;

/**
 * This class holds all the components of Preferences -> Tracks Tab and
 * Preferences -> Tracks Defaults Tab
 * and some of the components are specific to Preferences -> Tracks Tab which
 * gets visible only for
 * that tab.
 *
 * @author Anuj
 */
public abstract class TrackPreferences implements ListSelectionListener {

    AbstractTableModel model;
    public int[] selectedRows;
    public ColorComboBox bgColorComboBox;
    public ColorComboBox fgColorComboBox;
    public ColorComboBox labelColorComboBox;
    public JComboBox labelFieldComboBox;
    public JRPCheckBox show2TracksCheckBox;
    public JTextField maxDepthTextField;
    public JRPButton applyButton;
    public JRPCheckBox collapsedCheckBox;
    public JComboBox trackNameSizeComboBox;
    protected JComboBox annotationLabelSizeComboBox;
    protected JCheckBox annotationLabelAutoSizeCheckBox;
    public ColorComboBox possitiveColorComboBox;
    public ColorComboBox negativeColorComboBox;
    public JRPCheckBox colorCheckBox;
    public JRPCheckBox arrowCheckBox;
    public JCheckBox autoRefreshCheckBox;
    public JRPButton refreshButton;
    public StyledJTable table;
    public ListSelectionModel lsm;
    public boolean initializationDetector; //Test to detect action events triggered by clicking a row in the table.
    public boolean applyChanged; // Whether apply the changed value to track or not.
    public static final int COL_MAX_DEPTH = 5;
    public static final int COL_LABEL_FIELD = 7;
    public static final int COL_BACKGROUND = 1;
    public static final int COL_FOREGROUND = 2;
    public static final int COL_SHOW_2_TRACKS = 6;
//	public static final int COL_CONNECTED = 8;
    public static final int COL_COLLAPSED = 4;
    public static final int COL_TRACK_NAME_SIZE = 3;
    public static final int COL_POS_STRAND_COLOR = 10;
    public static final int COL_NEG_STRAND_COLOR = 11;
    public static final int COL_DIRECTION_TYPE = 9;
    public static final int COL_LABEL_COLOR = 12;
    protected float trackNameSize;
    public String b1Text, b2Text, track, title;

    public void initTable() {
        table = new StyledJTable(model);
        table.setCellSelectionEnabled(false);
        TableCellEditor editor = table.getDefaultEditor(String.class);
        ((DefaultCellEditor) editor).setClickCountToStart(2);
        table.setDefaultEditor(String.class, editor);

        table.list.add(TierPrefsView.COL_BACKGROUND);
        table.list.add(TierPrefsView.COL_FOREGROUND);
        table.list.add(TierPrefsView.COL_LABEL_COLOR);

        lsm = table.getSelectionModel();
        lsm.addListSelectionListener(this);
        lsm.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        table.setRowSelectionAllowed(true);

        table.setDefaultRenderer(Color.class, new ColorTableCellRenderer());
        table.setDefaultEditor(Color.class, new DefaultCellEditor(new ColorExComboBox()));
        table.setDefaultRenderer(Boolean.class, new BooleanTableCellRenderer());
        table.setDefaultEditor(Float.class, new DefaultCellEditor(new JComboBox(TrackConstants.SUPPORTED_SIZE)));
        table.setDefaultEditor(TrackConstants.DirectionType.class, new DefaultCellEditor(new JComboBox(TrackConstants.DirectionType.values())));

        table.getColumnModel().getColumn(COL_FOREGROUND).setPreferredWidth(60);
        table.getColumnModel().getColumn(COL_FOREGROUND).setMaxWidth(60);
        table.getColumnModel().getColumn(COL_FOREGROUND).setMinWidth(60);

        table.getColumnModel().getColumn(COL_BACKGROUND).setPreferredWidth(60);
        table.getColumnModel().getColumn(COL_BACKGROUND).setMaxWidth(60);
        table.getColumnModel().getColumn(COL_BACKGROUND).setMinWidth(60);

        table.getColumnModel().getColumn(COL_TRACK_NAME_SIZE).setPreferredWidth(60);
        table.getColumnModel().getColumn(COL_TRACK_NAME_SIZE).setMaxWidth(60);
        table.getColumnModel().getColumn(COL_TRACK_NAME_SIZE).setMinWidth(60);
    }

    public void initCommonComponents() {
        possitiveColorComboBox = new ColorComboBox();
        negativeColorComboBox = new ColorComboBox();
        autoRefreshCheckBox = new JCheckBox();
        refreshButton = new JRPButton(this.getClass().getCanonicalName() + "_refreshButton");
        colorCheckBox = new JRPCheckBox(this.getClass().getCanonicalName() + "_colorCheckBox");
        arrowCheckBox = new JRPCheckBox(this.getClass().getCanonicalName() + "_arrowCheckBox");
        bgColorComboBox = new ColorComboBox();
        trackNameSizeComboBox = new JComboBox();
        annotationLabelSizeComboBox = new AnnotationLabelCombobox();
        annotationLabelAutoSizeCheckBox = new JCheckBox();
        annotationLabelAutoSizeCheckBox.setSelected(EfficientLabelledLineGlyph.AUTO_SIZE_LABELS);
        fgColorComboBox = new ColorComboBox();
        labelColorComboBox = new ColorComboBox();
        labelFieldComboBox = new JComboBox();
        maxDepthTextField = new JRPTextField(this.getClass().getCanonicalName() + "_maxDepth");
        ((AbstractDocument) maxDepthTextField.getDocument()).setDocumentFilter(new NumericFilter.IntegerNumericFilter());
        applyButton = new JRPButton(this.getClass().getCanonicalName() + "_applyButton");
        show2TracksCheckBox = new JRPCheckBox(this.getClass().getCanonicalName() + "_show2TracksCheckBox");
        collapsedCheckBox = new JRPCheckBox(this.getClass().getCanonicalName() + "_collapsedCheckBox");
        colorCheckBox.setText("Color");
        arrowCheckBox.setText("Arrow");
        possitiveColorComboBox.setBackground(new Color(255, 255, 255));
        possitiveColorComboBox.setBorder(new LineBorder(new Color(255, 255, 255), 1, true));
        possitiveColorComboBox.setButtonVisible(false);
        possitiveColorComboBox.setColorValueVisible(false);
        possitiveColorComboBox.setMaximumSize(new Dimension(150, 20));
        possitiveColorComboBox.setStretchToFit(true);

        negativeColorComboBox.setBackground(new Color(255, 255, 255));
        negativeColorComboBox.setBorder(new LineBorder(new Color(255, 255, 255), 1, true));
        negativeColorComboBox.setButtonVisible(false);
        negativeColorComboBox.setColorValueVisible(false);
        negativeColorComboBox.setMaximumSize(new Dimension(150, 20));
        negativeColorComboBox.setStretchToFit(true);

        bgColorComboBox.setBackground(new Color(255, 255, 255));
        bgColorComboBox.setBorder(new LineBorder(new Color(255, 255, 255), 1, true));
        bgColorComboBox.setButtonVisible(false);
        bgColorComboBox.setColorValueVisible(false);
        bgColorComboBox.setMaximumSize(new Dimension(150, 20));
        bgColorComboBox.setStretchToFit(true);

        trackNameSizeComboBox.setModel(new DefaultComboBoxModel(TrackConstants.SUPPORTED_SIZE));
        annotationLabelSizeComboBox.setModel(new DefaultComboBoxModel(TrackConstants.SUPPORTED_SIZE));
        annotationLabelSizeComboBox.setEnabled(EfficientLabelledLineGlyph.AUTO_SIZE_LABELS);

        fgColorComboBox.setBackground(new Color(255, 255, 255));
        fgColorComboBox.setBorder(new LineBorder(new Color(255, 255, 255), 1, true));
        fgColorComboBox.setButtonVisible(false);
        fgColorComboBox.setColorValueVisible(false);
        fgColorComboBox.setMaximumSize(new Dimension(150, 20));
        fgColorComboBox.setStretchToFit(true);

        labelColorComboBox.setBackground(new Color(255, 255, 255));
        labelColorComboBox.setBorder(new LineBorder(new Color(255, 255, 255), 1, true));
        labelColorComboBox.setButtonVisible(false);
        labelColorComboBox.setColorValueVisible(false);
        labelColorComboBox.setMaximumSize(new Dimension(150, 20));
        labelColorComboBox.setStretchToFit(true);

        labelFieldComboBox.setModel(new DefaultComboBoxModel(TrackConstants.LABELFIELD));

        show2TracksCheckBox.setText("Show (+/-) tracks");

        collapsedCheckBox.setText("Collapsed");

    }

    public abstract void valueChanged(ListSelectionEvent evt);

    public abstract JTextField getTrackDefaultTextField();

    public ColorComboBox getPossitiveColorCombo() {
        return possitiveColorComboBox;
    }

    public ColorComboBox getNegativeColorComboBox() {
        return negativeColorComboBox;
    }

    public JRPCheckBox getColorCheckBox() {
        return colorCheckBox;
    }

    public JRPCheckBox getArrowCheckBox() {
        return arrowCheckBox;
    }

    public JTable getTable() {
        return table;
    }

    public ColorComboBox getBgColorComboBox() {
        return bgColorComboBox;
    }

    public JComboBox getTrackNameSizeComboBox() {
        return trackNameSizeComboBox;
    }

    public JComboBox getAnnotationLabelSizeComboBox() {
        return annotationLabelSizeComboBox;
    }

    public JCheckBox getannotationLabelAutoSizeCheckBox() {
        return annotationLabelAutoSizeCheckBox;
    }

    public ColorComboBox getFgColorComboBox() {
        return fgColorComboBox;
    }

    public ColorComboBox getFieldColorComboBox() {
        return labelColorComboBox;
    }

    public JComboBox getLabelFieldComboBox() {
        return labelFieldComboBox;
    }

    public JTextField getMaxDepthTextField() {
        return maxDepthTextField;
    }

    public JRPButton getApplyButton() {
        return applyButton;
    }

    public JCheckBox getShow2TracksCheckBox() {
        return show2TracksCheckBox;
    }

    public JCheckBox getCollapsedCheckBox() {
        return collapsedCheckBox;
    }

    public void bgColorComboBox() {
        if (applyChanged) {
            model.setValueAt(bgColorComboBox.getSelectedColor(), 0, COL_BACKGROUND);
        }
    }

    public void fgColorComboBox() {
        if (applyChanged) {
            model.setValueAt(fgColorComboBox.getSelectedColor(), 0, COL_FOREGROUND);
        }
    }

    public void labelColorComboBox() {
        if (applyChanged) {
            model.setValueAt(labelColorComboBox.getSelectedColor(), 0, COL_LABEL_COLOR);
        }
    }

    public void labelFieldComboBox() {
        if (applyChanged) {
            model.setValueAt(labelFieldComboBox.getSelectedItem(), 0, COL_LABEL_FIELD);
        }
    }

    public void show2TracksCheckBox() {
        if (applyChanged) {
            model.setValueAt(show2TracksCheckBox.isSelected(), 0, COL_SHOW_2_TRACKS);
        }
    }

    public void maxDepthTextField() {
        if (applyChanged) {
            model.setValueAt(maxDepthTextField.getText(), 0, COL_MAX_DEPTH);
        }
    }

    public void applyMaxDepth() {
        if (applyChanged) {
            maxDepthTextField();
            if (!(((TierPrefsView) this).autoApplyChanges())) {
                ((TierPrefsView.TierPrefsTableModel) model).update(COL_MAX_DEPTH);
            }
        }
    }

    public void collapsedCheckBox() {
        if (applyChanged) {
            model.setValueAt(collapsedCheckBox.isSelected(), 0, COL_COLLAPSED);
        }
    }

    public abstract void trackNameSizeComboBox();

    public void possitiveColorComboBox() {
        if (applyChanged) {
            model.setValueAt(possitiveColorComboBox.getSelectedColor(), 0, COL_POS_STRAND_COLOR);
        }
    }

    public void negativeColorComboBox() {
        if (applyChanged) {
            model.setValueAt(negativeColorComboBox.getSelectedColor(), 0, COL_NEG_STRAND_COLOR);
        }
    }

    public void colorCheckBox() {
        if (applyChanged) {
            if (colorCheckBox.isSelected()) {
                if (arrowCheckBox.isSelected()) {
                    model.setValueAt(TrackConstants.DirectionType.BOTH, 0, COL_DIRECTION_TYPE);
                } else {
                    model.setValueAt(TrackConstants.DirectionType.COLOR, 0, COL_DIRECTION_TYPE);
                }
            } else {
                if (arrowCheckBox.isSelected()) {
                    model.setValueAt(TrackConstants.DirectionType.ARROW, 0, COL_DIRECTION_TYPE);
                } else {
                    model.setValueAt(TrackConstants.DirectionType.NONE, 0, COL_DIRECTION_TYPE);
                }
            }
        }
    }

    public void arrowCheckBox() {
        if (applyChanged) {
            if (colorCheckBox.isSelected()) {
                if (arrowCheckBox.isSelected()) {
                    model.setValueAt(TrackConstants.DirectionType.BOTH, 0, COL_DIRECTION_TYPE);
                } else {
                    model.setValueAt(TrackConstants.DirectionType.COLOR, 0, COL_DIRECTION_TYPE);
                }
            } else {
                if (arrowCheckBox.isSelected()) {
                    model.setValueAt(TrackConstants.DirectionType.ARROW, 0, COL_DIRECTION_TYPE);
                } else {
                    model.setValueAt(TrackConstants.DirectionType.NONE, 0, COL_DIRECTION_TYPE);
                }
            }
        }
    }

    public void annotationLabelAutoSizeCheckBoxActionPerformed() {
        EfficientLabelledLineGlyph.AUTO_SIZE_LABELS = !EfficientLabelledLineGlyph.AUTO_SIZE_LABELS;
        annotationLabelSizeComboBox.setEnabled(!EfficientLabelledLineGlyph.AUTO_SIZE_LABELS);
//        Application.getSingleton().getMapView() .updateWidget();
    }

    public void annotationLabelSizeComboBoxActionPerformed() {
        float annotationLabelSize = Float.parseFloat(annotationLabelSizeComboBox.getSelectedItem().toString());
        EfficientLabelledLineGlyph.OVERRIDE_FONT = new Font(Font.MONOSPACED, Font.PLAIN, Math.round(annotationLabelSize));
    }

    class AnnotationLabelCombobox extends JComboBox {

        @Override
        public void setEnabled(boolean enabled) {
            if (enabled) {
                if (!EfficientLabelledLineGlyph.AUTO_SIZE_LABELS) {
                    super.setEnabled(enabled);
                }
            } else {
                super.setEnabled(enabled);
            }
        }
    }
}
