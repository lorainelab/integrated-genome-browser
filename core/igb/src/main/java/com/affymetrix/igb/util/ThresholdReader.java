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
	/* amount (number of bases) that the Threshold is moved to switch from summary to detail */
	public static final int threshold_increment = 1000;
	/* minimum threshold amount (number of bases), individual tracks cannot go below this value. zero means to use the default */
	public static final int threshold_min = threshold_increment;
	/* default threshold amount (number of bases), for autoload and individual track threshold */
	public static final int default_threshold = 15000;
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
