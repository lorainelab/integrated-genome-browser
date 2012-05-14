/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.igb.shared.AbstractGraphGlyph;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import com.affymetrix.igb.tiers.TierLabelManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.JColorChooser;
import javax.swing.JDialog;

/**
 *
 * @author dcnorris
 */
public class ChangeTierLabelColorActionA extends SeqMapViewActionA {

	protected ChangeTierLabelColorActionA(String text, String iconPath, String largeIconPath) {
		super(text, iconPath, largeIconPath);
	}

	protected void changeColor(final List<TierLabelGlyph> tier_label_glyphs, final boolean fg) {
		if (tier_label_glyphs.isEmpty()) {
			return;
		}

		final JColorChooser chooser = new JColorChooser();

		TierLabelGlyph tlg_0 = tier_label_glyphs.get(0);
		TierGlyph tier_0 = (TierGlyph) tlg_0.getInfo();
		ITrackStyleExtended style_0 = tier_0.getAnnotStyle();
		if (style_0 != null) {
			if (fg) {
				chooser.setColor(style_0.getLabelForeground());
			} else {
				chooser.setColor(style_0.getLabelBackground());
			}
		}
		ActionListener al = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				for (TierLabelGlyph tlg : tier_label_glyphs) {
					TierGlyph tier = (TierGlyph) tlg.getInfo();
					ITrackStyleExtended style = tier.getAnnotStyle();

					if (style != null) {
						if (fg) {
							style.setLabelForeground(chooser.getColor());
						} else {
							style.setLabelBackground(chooser.getColor());
						}
					}
					for (AbstractGraphGlyph gg : TierLabelManager.getContainedGraphs(tier_label_glyphs)) {
						if (fg) {
							gg.setColor(chooser.getColor());
							gg.getGraphState().getTierStyle().setLabelForeground(chooser.getColor());
						} else {
							gg.getGraphState().getTierStyle().setLabelBackground(chooser.getColor());
						}
					}
				}
			}			
		};

		JDialog dialog = JColorChooser.createDialog((java.awt.Component) null, // parent
				"Pick a Color",
				true, //modal
				chooser,
				al, //OK button handler
				null); //no CANCEL button handler

		dialog.setVisible(true);
		gviewer.getSeqMap().updateWidget();
	}
}
