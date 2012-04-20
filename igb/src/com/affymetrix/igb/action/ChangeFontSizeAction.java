package com.affymetrix.igb.action;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.JOptionPane;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import com.affymetrix.igb.tiers.TrackConstants;
import com.affymetrix.igb.tiers.TrackStyle;

public class ChangeFontSizeAction extends SeqMapViewActionA {
	private static final long serialVersionUID = 1L;
	private static final ChangeFontSizeAction ACTION = new ChangeFontSizeAction();

	public static ChangeFontSizeAction getAction() {
		return ACTION;
	}

	private ChangeFontSizeAction() {
		super(BUNDLE.getString("changeFontSizeAction"), null);
	}

	private void changeFontSize(List<TierLabelGlyph> tier_label_glyphs, float size) {
		for (TierLabelGlyph tlg : tier_label_glyphs) {
			TierGlyph tier = (TierGlyph) tlg.getInfo();
			ITrackStyleExtended style = tier.getAnnotStyle();
			if (style != null && style instanceof TrackStyle) {
				((TrackStyle) style).setTrackNameSize(size);
			}
		}
		getSeqMapView().getSeqMap().updateWidget();
	}

	private void changeFontSize(List<TierLabelGlyph> tier_labels) {
		if (tier_labels == null || tier_labels.isEmpty()) {
			ErrorHandler.errorPanel("changeExpandMaxAll called with an empty list");
			return;
		}

		Object initial_value = TrackStyle.default_track_name_size;
		if (tier_labels.size() == 1) {
			TierLabelGlyph tlg = tier_labels.get(0);
			TierGlyph tg = (TierGlyph) tlg.getInfo();
			ITrackStyleExtended style = tg.getAnnotStyle();
			if (style != null && style instanceof TrackStyle) {
				initial_value = ((TrackStyle) style).getTrackNameSize();
			}
		}

		Object input = JOptionPane.showInputDialog(null, BUNDLE.getString("selectFontSize"), BUNDLE.getString("changeSelectedTrackFontSize"), JOptionPane.PLAIN_MESSAGE, null,
				TrackConstants.SUPPORTED_SIZE, initial_value);

		if (input == null) {
			return;
		}

		changeFontSize(tier_labels, (Float) input);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		changeFontSize(getTierManager().getSelectedTierLabels());
		TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
	}
}
