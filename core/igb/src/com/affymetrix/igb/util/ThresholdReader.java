package com.affymetrix.igb.util;

import com.affymetrix.genoviz.widget.NeoAbstractWidget;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.igb.IGB;

/**
 * holds all the logic for transitioning, for both semantic zoom and autoload.
 * make any logic changes in this class, to attempt to leave all other
 * classes alone.
 */
public class ThresholdReader {
	public static final int threshold_increment = 1000;
	public static final int threshold_min = threshold_increment;
	public static final int default_threshold = 100000;
	private static final ThresholdReader instance = new ThresholdReader();
	public static final ThresholdReader getInstance() {
		return instance;
	}
	private final NeoMap seqmap;
	private ThresholdReader() {
		super();
		this.seqmap = ((IGB)IGB.getSingleton()).getMapView().getSeqMap();
	}

	public int getCurrentThresholdValue() {
		return (int)seqmap.getView().getCoordBox().getWidth();
	}

	public double getAsZoomerPercent(int threshold) {
		double scale = ((IGB)IGB.getSingleton()).getMapView().getPixelsToCoord(0, threshold);
		return seqmap.zoomerValueFromScale(NeoAbstractWidget.X, scale);
	}

	/**
	 * change the threshold so that the current value becomes summary
	 * @return new threshhold
	 */
	public int toSummary() {
		return Math.max(threshold_min, getCurrentThresholdValue() - threshold_increment);
	}

	/**
	 * change the threshold so that the current value becomes detail
	 * @return new threshhold
	 */
	public int toDetail() {
		return getCurrentThresholdValue() + threshold_increment;
	}

	public boolean isDetail(int threshold) {
		return getCurrentThresholdValue() <= threshold;
	}
}
