package com.affymetrix.igb.action;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JColorChooser;
import javax.swing.JDialog;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.shared.AbstractGraphGlyph;
import com.affymetrix.igb.shared.ParameteredAction;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import com.affymetrix.igb.tiers.TierLabelManager;

/**
 * note - this class contains an instance of SeqMapView. For now, there
 * is just one instance using the regular SeqMapView, no instance for
 * AltSpliceView
 */
public abstract class ChangeColorActionA extends SeqMapViewActionA implements ParameteredAction {
	protected static final java.awt.Color DEFAULT_COLOR = javax.swing.UIManager.getColor("Button.background");
	private static final long serialVersionUID = 1L;

	protected ChangeColorActionA(String text, String iconPath, String largeIconPath) {
		super(text, iconPath, largeIconPath);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		changeColor();
		TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
	}

	protected abstract void setChooserColor(JColorChooser chooser, ITrackStyleExtended style);
	protected abstract void setStyleColor(Color color, ITrackStyleExtended style);
	protected void setGraphColor(AbstractGraphGlyph gg, Color color) { }

	private void changeColor() {
		if (getTierManager().getSelectedTierLabels().isEmpty()) {
			return;
		}

		final JColorChooser chooser = new JColorChooser();

		TierLabelGlyph tlg_0 = getTierManager().getSelectedTierLabels().get(0);
		TierGlyph tier_0 = (TierGlyph) tlg_0.getInfo();
		ITrackStyleExtended style_0 = tier_0.getAnnotStyle();
		if (style_0 != null) {
			setChooserColor(chooser, style_0);
		}

		ActionListener al = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				performAction(chooser.getColor());
				refreshMap(false, false);
			}

		};

		JDialog dialog = JColorChooser.createDialog((java.awt.Component) null, // parent
				"Pick a Color",
				true, //modal
				chooser,
				al, //OK button handler
				null); //no CANCEL button handler
		dialog.setVisible(true);

	}
	
	private void changeColor(Color color){
		final List<TierLabelGlyph> tier_label_glyphs = getTierManager().getSelectedTierLabels();
		if (tier_label_glyphs.isEmpty() && getSeqMapView().getPixelFloater().getChildren().isEmpty()) {
			return;
		}
		
		for (TierLabelGlyph tlg : tier_label_glyphs) {
			TierGlyph tier = (TierGlyph) tlg.getInfo();
			ITrackStyleExtended style = tier.getAnnotStyle();
			if (style != null) {
				setStyleColor(color, style);
			}
		}

		// For Joined Graphs
		for (AbstractGraphGlyph gg : TierLabelManager.getContainedGraphs(tier_label_glyphs)) {
			setStyleColor(color, gg.getGraphState().getTierStyle());
			setGraphColor(gg, color);
		}
		
		// For Floating graphs
		for(GlyphI glyph : getSeqMapView().getPixelFloater().getChildren()){
			if(glyph.isSelected() && glyph instanceof AbstractGraphGlyph){
				AbstractGraphGlyph gg = (AbstractGraphGlyph)glyph;
				setStyleColor(color, gg.getGraphState().getTierStyle());
				setGraphColor(gg, color);
			}
		}
		
	}
	
	@Override
	public void performAction(Object parameter){
		if(parameter.getClass() != Color.class)
			return;
		
		changeColor((Color)parameter);
	}
}
