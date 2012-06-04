package com.affymetrix.igb.action;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JColorChooser;
import javax.swing.JDialog;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.igb.shared.AbstractGraphGlyph;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import com.affymetrix.igb.tiers.TierLabelManager;

/**
 * note - this class contains an instance of SeqMapView. For now, there
 * is just one instance using the regular SeqMapView, no instance for
 * AltSpliceView
 */
public abstract class ChangeColorActionA extends SeqMapViewActionA {
	protected static final java.awt.Color DEFAULT_COLOR = javax.swing.UIManager.getColor("Button.background");
	private static final long serialVersionUID = 1L;

	protected ChangeColorActionA(String text, String iconPath, String largeIconPath) {
		super(text, iconPath, largeIconPath);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		changeColor(getTierManager().getSelectedTierLabels());
		TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
	}

	protected abstract void setChooserColor(JColorChooser chooser, ITrackStyleExtended style);
	protected abstract void setStyleColor(JColorChooser chooser, ITrackStyleExtended style);
	protected void setGraphColor(AbstractGraphGlyph gg, Color color) { }

	private void changeColor(final List<TierLabelGlyph> tier_label_glyphs) {
		if (tier_label_glyphs.isEmpty()) {
			return;
		}

		final JColorChooser chooser = new JColorChooser();

		TierLabelGlyph tlg_0 = tier_label_glyphs.get(0);
		TierGlyph tier_0 = (TierGlyph) tlg_0.getInfo();
		ITrackStyleExtended style_0 = tier_0.getAnnotStyle();
		if (style_0 != null) {
			setChooserColor(chooser, style_0);
		}

		ActionListener al = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				for (TierLabelGlyph tlg : tier_label_glyphs) {
					TierGlyph tier = (TierGlyph) tlg.getInfo();
					ITrackStyleExtended style = tier.getAnnotStyle();

					if (style != null) {
						setStyleColor(chooser, style);
					}
					for (AbstractGraphGlyph gg : TierLabelManager.getContainedGraphs(tier_label_glyphs)) {
						setStyleColor(chooser, gg.getGraphState().getTierStyle());
						setGraphColor(gg, chooser.getColor());
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
