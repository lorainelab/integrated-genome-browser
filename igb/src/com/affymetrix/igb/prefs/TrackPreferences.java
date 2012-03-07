/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.prefs;

import com.affymetrix.genoviz.swing.recordplayback.JRPCheckBox;
import com.affymetrix.genoviz.swing.recordplayback.JRPComboBox;
import com.affymetrix.genoviz.swing.recordplayback.JRPNumTextField;
import com.affymetrix.igb.tiers.TrackConstants;
import com.jidesoft.combobox.ColorComboBox;
import java.awt.Color;
import java.awt.Dimension;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author auser
 */
public abstract class TrackPreferences {
	AbstractTableModel model;
	public int[] selectedRows;
	protected boolean settingValueFromTable;
	public ColorComboBox bgColorComboBox;
	public ColorComboBox fgColorComboBox;
	public JComboBox labelFieldComboBox;
	public JRPCheckBox show2TracksCheckBox;
	public JTextField maxDepthTextField;
	public JRPCheckBox connectedCheckBox;
	public JRPCheckBox collapsedCheckBox;
	public JComboBox trackNameSizeComboBox;
	public ColorComboBox possitiveColorComboBox;
	public ColorComboBox negativeColorComboBox;
	public JRPCheckBox colorCheckBox;
	public JRPCheckBox arrowCheckBox;
	public boolean initializationDetector; //Test to detect action events triggered by clicking a row in the table.
	public static final int COL_MAX_DEPTH = 5;
	public static final int COL_LABEL_FIELD = 7;
	public static final int COL_BACKGROUND = 1;
	public static final int COL_FOREGROUND = 2;
	public static final int Col_Show_2_Tracks = 6;
	public static final int COL_CONNECTED = 8;
	public static final int COL_COLLAPSED = 4;
	public static final int COL_TRACK_NAME_SIZE = 3;
	public static final int COL_POS_STRAND_COLOR = 10;
	public static final int COL_NEG_STRAND_COLOR = 11;
	public static final int COL_DIRECTION_TYPE = 9;
	protected float trackNameSize;
	public String b1Text,b2Text,track,title;
	public void initCommonComponents()
	{
		possitiveColorComboBox = new ColorComboBox();
		negativeColorComboBox = new ColorComboBox();
		colorCheckBox = new JRPCheckBox(this.getClass().getCanonicalName()+"_colorCheckBox");
		arrowCheckBox = new JRPCheckBox(this.getClass().getCanonicalName()+"_arrowCheckBox");
		bgColorComboBox = new ColorComboBox();
		trackNameSizeComboBox = new JComboBox();
		fgColorComboBox = new ColorComboBox();
		labelFieldComboBox = new JComboBox();
		maxDepthTextField = new JRPNumTextField(this.getClass().getCanonicalName()+"_maxDepth");
		show2TracksCheckBox = new JRPCheckBox(this.getClass().getCanonicalName()+"_show2TracksCheckBox");
		connectedCheckBox = new JRPCheckBox(this.getClass().getCanonicalName()+"_connectedCheckBox");
		collapsedCheckBox = new JRPCheckBox(this.getClass().getCanonicalName()+"_collapsedCheckBox");
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

		fgColorComboBox.setBackground(new Color(255, 255, 255));
		fgColorComboBox.setBorder(new LineBorder(new Color(255, 255, 255), 1, true));
		fgColorComboBox.setButtonVisible(false);
		fgColorComboBox.setColorValueVisible(false);
		fgColorComboBox.setMaximumSize(new Dimension(150, 20));
		fgColorComboBox.setStretchToFit(true);
		
		labelFieldComboBox.setModel(new DefaultComboBoxModel(TrackConstants.LABELFIELD));

		show2TracksCheckBox.setText("Show (+/-) tracks");

		connectedCheckBox.setText("Connected");

		collapsedCheckBox.setText("Collapsed");

	}
	public abstract void valueChanged(ListSelectionEvent evt);
	public void bgColorComboBox(){
		if (!settingValueFromTable) {
			model.setValueAt(bgColorComboBox.getSelectedColor(), selectedRows[0], COL_BACKGROUND);
		}
	}
	public void fgColorComboBox(){
		if (!settingValueFromTable) {
			model.setValueAt(fgColorComboBox.getSelectedColor(), 0, COL_FOREGROUND);
		}
	}
	public void labelFieldComboBox(){
		if (!settingValueFromTable) {
			model.setValueAt(labelFieldComboBox.getSelectedItem(), 0, COL_LABEL_FIELD);
		}
	}
	public void show2TracksCheckBox(){
		if (!settingValueFromTable) {
			model.setValueAt(show2TracksCheckBox.isSelected(), 0, Col_Show_2_Tracks);
		}
	}
	public void maxDepthTextField(){
		if (!settingValueFromTable) {
			model.setValueAt(maxDepthTextField.getText(), 0, COL_MAX_DEPTH);
		}
	}
	public void connectedCheckBox(){
		if (!settingValueFromTable) {
			model.setValueAt(connectedCheckBox.isSelected(), 0, COL_CONNECTED);
		}
	}
	public void collapsedCheckBox(){
		if (!settingValueFromTable) {
			model.setValueAt(collapsedCheckBox.isSelected(), 0, COL_COLLAPSED);
		}
	}
	public abstract void trackNameSizeComboBox();
	public void possitiveColorComboBox(){
		if (!settingValueFromTable) {
			model.setValueAt(possitiveColorComboBox.getSelectedColor(), 0, COL_POS_STRAND_COLOR);
		}
	}
	public void negativeColorComboBox(){
		if (!settingValueFromTable) {
			model.setValueAt(negativeColorComboBox.getSelectedColor(), 0, COL_NEG_STRAND_COLOR);
		}
	}
	public void colorCheckBox(){
		if (!settingValueFromTable) {
			if (colorCheckBox.isSelected()) {
				if (arrowCheckBox.isSelected()) {
					model.setValueAt(TrackConstants.DIRECTION_TYPE.BOTH, 0, COL_DIRECTION_TYPE);
				} else {
					model.setValueAt(TrackConstants.DIRECTION_TYPE.COLOR, 0, COL_DIRECTION_TYPE);
				}
			} else {
				if (arrowCheckBox.isSelected()) {
					model.setValueAt(TrackConstants.DIRECTION_TYPE.ARROW, 0, COL_DIRECTION_TYPE);
				} else {
					model.setValueAt(TrackConstants.DIRECTION_TYPE.NONE, 0, COL_DIRECTION_TYPE);
				}
			}
		}
	}
	public void arrowCheckBox(){
		if (!settingValueFromTable) {
			if (colorCheckBox.isSelected()) {
				if (arrowCheckBox.isSelected()) {
					model.setValueAt(TrackConstants.DIRECTION_TYPE.BOTH, 0, COL_DIRECTION_TYPE);
				} else {
					model.setValueAt(TrackConstants.DIRECTION_TYPE.COLOR, 0, COL_DIRECTION_TYPE);
				}
			} else {
				if (arrowCheckBox.isSelected()) {
					model.setValueAt(TrackConstants.DIRECTION_TYPE.ARROW, 0, COL_DIRECTION_TYPE);
				} else {
					model.setValueAt(TrackConstants.DIRECTION_TYPE.NONE, 0, COL_DIRECTION_TYPE);
				}
			}
		}
	}
}
