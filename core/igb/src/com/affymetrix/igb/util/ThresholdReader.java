package com.affymetrix.igb.util;

import com.affymetrix.genoviz.widget.NeoAbstractWidget;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.igb.IGB;

public class ThresholdReader {
	public static final int threshold_increment = 10000;
	public static final int threshold_min = threshold_increment;
	public static final int threshold_max = 20000000;
	public static final int default_threshold = 200000;
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

	public int getIncrement() {
		return Math.min(threshold_max, getCurrentThresholdValue() - threshold_increment);
	}

	public int getDecrement() {
		return Math.max(threshold_min, getCurrentThresholdValue() + threshold_increment);
	}

	public boolean isDetail(int threshold) {
		return getCurrentThresholdValue() <= threshold;
	}
}
