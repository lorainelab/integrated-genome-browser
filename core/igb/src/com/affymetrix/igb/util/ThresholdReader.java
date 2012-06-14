package com.affymetrix.igb.util;

import javax.swing.JSlider;

import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.igb.IGB;

public class ThresholdReader {
	public static final int default_threshold = 80;
	private static final ThresholdReader instance = new ThresholdReader();
	public static final ThresholdReader getInstance() {
		return instance;
	}
	private final JSlider zoomer;
	private ThresholdReader() {
		super();
		this.zoomer = (JSlider)((IGB)IGB.getSingleton()).getMapView().getSeqMap().getZoomer(NeoMap.X);
	}

	public int getCurrentThresholdValue() {
		return (zoomer.getValue() * 100 / zoomer.getMaximum());
	}

	public int getIncrement() {
		return Math.min(100, getCurrentThresholdValue() + 1);
	}

	public int getDecrement() {
		return Math.max(0, getCurrentThresholdValue() - 1);
	}

	public double getAsZoomerPercent(int threshold) {
		return threshold / 100.0;
	}
}
