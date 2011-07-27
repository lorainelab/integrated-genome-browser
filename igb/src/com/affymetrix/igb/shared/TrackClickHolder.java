package com.affymetrix.igb.shared;

import java.util.ArrayList;
import java.util.List;

public class TrackClickHolder {
	private static TrackClickHolder instance = new TrackClickHolder();
	private TrackClickHolder() {
		super();
	}
	public static TrackClickHolder getInstance() {
		return instance;
	}

	private List<TrackClickListener> TrackClickListeners = new ArrayList<TrackClickListener>();

	public void addTrackClickListener(TrackClickListener TrackClickListener) {
		TrackClickListeners.add(TrackClickListener);
	}

	public void removeTrackClickListener(TrackClickListener TrackClickListener) {
		TrackClickListeners.remove(TrackClickListener);
	}

	public List<TrackClickListener> getTrackClickListeners() {
		return TrackClickListeners;
	}
}
