package com.affymetrix.igb.util;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.tiers.TrackStyle;

public class TrackUtils {
	private static final TrackUtils instance = new TrackUtils();
	public static TrackUtils getInstance() {
		return instance;
	}

	private TrackUtils() {
		super();
	}
	
	public void addTrack(SeqSymmetry sym, String method, TrackStyle preferredStyle) {
		makeNonPersistentStyle((SymWithProps) sym, method, preferredStyle);
		BioSeq aseq = GenometryModel.getGenometryModel().getSelectedSeq();
		aseq.addAnnotation(sym);
		Application.getSingleton().getMapView().setAnnotatedSeq(aseq, true, true);
	}

	private TrackStyle makeNonPersistentStyle(SymWithProps sym, String human_name, TrackStyle preferredStyle) {
		// Needs a unique name so that if any later tier is produced with the same
		// human name, it will not automatically get the same color, etc.
		String unique_name = TrackStyle.getUniqueName(human_name);
		sym.setProperty("method", unique_name);
		if (sym.getProperty("id") == null) {
			sym.setProperty("id", unique_name);
		}
		TrackStyle style = TrackStyle.getInstance(unique_name, false);
		if (preferredStyle == null) {
			style.setGlyphDepth(1);
			style.setSeparate(false); // there are not separate (+) and (-) strands
			style.setCustomizable(false); // the user can change the color, but not much else is meaningful
		}
		else {
			style.copyPropertiesFrom(preferredStyle);
		}
		style.setTrackName(human_name);
		return style;
	}

	public String useViewMode(String method) {
		if (method != null && method.contains("___.")) {
			return "expanded";
		}
		if (method != null && method.contains("$$$.")) {
			return "collapsed";
		}
		if (method != null && method.contains("!!!.")) {
			return "not";
		}
		if (method != null && method.contains("@@@.")) {
			return "Natural Log";
		}
		return null;
	}
}
