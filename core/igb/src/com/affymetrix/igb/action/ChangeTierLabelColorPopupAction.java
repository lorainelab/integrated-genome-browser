/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.shared.SelectAllAction;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import java.awt.event.ActionEvent;
import javax.swing.*;

/**
 *
 * @author lorainelab
 */
public class ChangeTierLabelColorPopupAction extends SeqMapViewActionA {

	private static ChangeTierLabelColorPopupAction ACTION;
	private JButton foreground = new JButton("Change Foreground");
	private JButton background = new JButton("Change Background");
	private JButton selectAll = new JButton("Select All");
	private JCheckBox syncColors = new JCheckBox("Sync with Track");
	private JPanel panel = new JPanel();

	public static ChangeTierLabelColorPopupAction getAction() {
		if (ACTION == null) {
			ACTION = new ChangeTierLabelColorPopupAction();
		}
		return ACTION;
	}

	private ChangeTierLabelColorPopupAction() {
		super(BUNDLE.getString("changeTierLabelColorPopup"), null, null);
		foreground.addActionListener(new java.awt.event.ActionListener() {

			public void actionPerformed(java.awt.event.ActionEvent evt) {
				ChangeTierLabelForegroundColorAction.getAction().actionPerformed(evt);
			}
		});
		background.addActionListener(new java.awt.event.ActionListener() {

			public void actionPerformed(java.awt.event.ActionEvent evt) {
				ChangeTierLabelBackgroundColorAction.getAction().actionPerformed(evt);
			}
		});
		selectAll.addActionListener(new java.awt.event.ActionListener() {

			public void actionPerformed(java.awt.event.ActionEvent evt) {
				SelectAllAction.getAction().actionPerformed(evt);
				if (!getTierManager().getSelectedTierLabels().isEmpty()) {
					background.setEnabled(true);
					foreground.setEnabled(true);
				}
			}
		});
		syncColors.addActionListener(new java.awt.event.ActionListener() {

			public void actionPerformed(java.awt.event.ActionEvent evt) {
				for (TierLabelGlyph label : getTierManager().getSelectedTierLabels()) {
					TierGlyph tier_0 = (TierGlyph) label.getInfo();
					ITrackStyleExtended style_0 = tier_0.getAnnotStyle();
					if (syncColors.isSelected()) {
						style_0.setLabelBackground(null);
						style_0.setLabelForeground(null);
						style_0.setLabelBackground(style_0.getBackground());
						style_0.setLabelForeground(style_0.getForeground());
						background.setEnabled(false);
						foreground.setEnabled(false);
						gviewer.getSeqMap().updateWidget();
					} else {
						background.setEnabled(true);
						foreground.setEnabled(true);
					}
					if (getTierManager().getSelectedTierLabels().isEmpty()) {
						background.setEnabled(false);
						foreground.setEnabled(false);
					}
				}
			}
		});
		syncColors.setSelected(true);
		background.setEnabled(false);
		foreground.setEnabled(false);
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		panel.add(syncColors);
		panel.add(background);
		panel.add(foreground);
		panel.add(selectAll);
	}

	public ChangeTierLabelColorPopupAction(String text, String tooltip, String iconPath, String largeIconPath, int mnemonic) {
		super(text, tooltip, iconPath, largeIconPath, mnemonic);
	}

	public ChangeTierLabelColorPopupAction(String text, String tooltip, String iconPath, String largeIconPath, int mnemonic, Object extraInfo, boolean popup) {
		super(text, tooltip, iconPath, largeIconPath, mnemonic, extraInfo, popup);
	}

	public ChangeTierLabelColorPopupAction(String text, String iconPath, String largeIconPath) {
		super(text, iconPath, largeIconPath);
	}

	public ChangeTierLabelColorPopupAction(String text, int mnemonic) {
		super(text, mnemonic);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		int ok = JOptionPane.showConfirmDialog(null, panel, "Change Label Colors.",
				JOptionPane.OK_CANCEL_OPTION);
	}
}
