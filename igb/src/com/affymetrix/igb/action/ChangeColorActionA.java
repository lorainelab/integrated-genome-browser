package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JColorChooser;
import javax.swing.JDialog;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.igb.shared.AbstractGraphGlyph;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import com.affymetrix.igb.tiers.TierLabelManager;
import com.affymetrix.igb.view.SeqMapView;

/**
 * note - this class contains an instance of SeqMapView. For now, there
 * is just one instance using the regular SeqMapView, no instance for
 * AltSpliceView
 */
public abstract class ChangeColorActionA extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;

	protected ChangeColorActionA(SeqMapView gviewer, String text, String tooltip, String iconPath) {
		super(gviewer, text, tooltip, iconPath);
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
				chooser.setColor(style_0.getForeground());
			} else {
				chooser.setColor(style_0.getBackground());
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
							style.setForeground(chooser.getColor());
						} else {
							style.setBackground(chooser.getColor());
						}
					}
					for (AbstractGraphGlyph gg : TierLabelManager.getContainedGraphs(tier_label_glyphs)) {
						if (fg) {
							gg.setColor(chooser.getColor());
							gg.getGraphState().getTierStyle().setForeground(chooser.getColor());
						} else {
							gg.getGraphState().getTierStyle().setBackground(chooser.getColor());
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

		refreshMap(false, false);
	}
}
