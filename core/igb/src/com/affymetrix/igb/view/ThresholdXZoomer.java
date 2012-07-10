package com.affymetrix.igb.view;

import java.awt.Adjustable;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.util.EventObject;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genoviz.swing.recordplayback.RPAdjustableJSlider;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;
import com.affymetrix.igb.util.ThresholdReader;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author hiralv
 */
public class ThresholdXZoomer extends RPAdjustableJSlider implements TrackstylePropertyMonitor.TrackStylePropertyListener {

	private static final long serialVersionUID = 1L;
	private final SeqMapView smv;
	public ThresholdXZoomer(String id, SeqMapView smv) {
		super(id + "_xzoomer", Adjustable.HORIZONTAL);
		this.smv = smv;
		TrackstylePropertyMonitor.getPropertyTracker().addPropertyListener(this);
	}

	@Override
	public void trackstylePropertyChanged(EventObject eo) {
		smv.getSeqMap().updateWidget();
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);

		if (smv.getAutoLoad() != null) {
			drawAutoLoadPoint(g);
		}
		for (TierGlyph tierGlyph : smv.getTierManager().getVisibleTierGlyphs()) {
			drawTrackThresholdPoint(g, tierGlyph);
		}
	}

	private void drawAutoLoadPoint(Graphics g) {
		drawThresholdPoint(g, Color.BLACK, Color.WHITE, smv.getAutoLoad().threshold);
	}

	private void drawTrackThresholdPoint(Graphics g, TierGlyph tier) {
		ITrackStyleExtended style = tier.getAnnotStyle();
		if (style == null || style.getSummaryThreshold() == 0) {
			return;
		}
		drawThresholdPoint(g, style.getBackground(), style.getForeground(), style.getSummaryThreshold());
	}

	private void drawThresholdPoint(Graphics g, Color bgColor, Color fgColor, int threshold) {
		Color c = g.getColor();
		int thresholdPosition = (int) (getMaximum() * ThresholdReader.getInstance().getAsZoomerPercent(threshold));
		g.setColor(fgColor);
		int xp = xPositionForValue(thresholdPosition);
		int yp = this.getHeight() / 2;
		int x[] = new int[]{xp, xp - 5, xp - 5, xp + 5, xp + 5};
		int y[] = new int[]{yp, yp / 2, 0, 0, yp / 2};
		g.fillPolygon(x, y, 5);
		g.setColor(bgColor);
		g.drawPolygon(x, y, 5);
		g.setColor(c);
	}

	private int xPositionForValue(int value) {
		int min = getMinimum();
		int max = getMaximum();
		int trackLength = this.getWidth();
		double valueRange = (double) max - (double) min;
		double pixelsPerValue = trackLength / valueRange;

		return (int) Math.round(pixelsPerValue * (value - min) - pixelsPerValue * 2);
	}

	@Override
	public String getToolTipText(MouseEvent me) {
		if (me != null && smv.getAutoLoad() != null) {
			int threshValue = (smv.getAutoLoad().threshold * getMaximum() / 100);
			int xp = xPositionForValue(threshValue);
			if (me.getX() > xp - 5 && me.getX() < xp + 5) {
				return BUNDLE.getString("autoloadToolTip");
			}
			return super.getToolTipText();
		}
		return super.getToolTipText();
	}
}
