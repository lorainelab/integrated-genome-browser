package com.affymetrix.genometryImpl.event;

import java.util.*;
import com.affymetrix.genometryImpl.general.GenericFeature;

public final class FeatureLoadEvent extends EventObject {
	private static final long serialVersionUID = 1L;
	private final GenericFeature feature;
	private final boolean loaded;

	/**
	 *  Constructor.
	 *  @param src The source of the event.
	 *  @param feature The feature of the event.
	 *  @param loaded whether the feature was loaded or unloaded.
	 */
	public FeatureLoadEvent(Object src, GenericFeature feature, boolean loaded) {
		super(src);
		this.feature = feature;
		this.loaded = loaded;
	}

	public GenericFeature getFeature() {
		return feature;
	}

	public boolean isLoaded() {
		return loaded;
	}

	@Override
		public String toString() {
			return "FeatureLoadEvent: feature: " + feature.featureName +
				" loaded: '" + loaded +
				"' source: " + this.getSource();
		}
}
