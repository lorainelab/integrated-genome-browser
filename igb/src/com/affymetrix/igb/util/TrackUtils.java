package com.affymetrix.igb.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.tiers.TierLabelGlyph;
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
		} else {
			style.copyPropertiesFrom(preferredStyle);
			style.setGlyphDepth(Math.max(1, SeqUtils.getDepth(sym) - 1));
		}
		style.setTrackName(human_name);
		return style;
	}

	public List<SeqSymmetry> getSymsFromLabelGlyphs(List<TierLabelGlyph> labels) {
		List<SeqSymmetry> syms = new ArrayList<SeqSymmetry>();
		for (TierLabelGlyph label : labels) {
			TierGlyph glyph = label.getReferenceTier();
			SeqSymmetry rootSym = (SeqSymmetry) glyph.getInfo();
			if (rootSym == null && glyph.getChildCount() > 0) {
				rootSym = (SeqSymmetry) glyph.getChild(0).getInfo();
			}
			syms.add(rootSym);
		}
		return syms;
	}

	private Map<FileTypeCategory, Integer> getTrackCounts(List<SeqSymmetry> syms) {
		Map<FileTypeCategory, Integer> trackCounts = new HashMap<FileTypeCategory, Integer>();
		for (SeqSymmetry sym : syms) {
			if (sym != null) {
				FileTypeCategory category = ((RootSeqSymmetry) sym).getCategory();
				if (trackCounts.get(category) == null) {
					trackCounts.put(category, 0);
				}
				trackCounts.put(category, trackCounts.get(category) + 1);
			}
		}
		return trackCounts;
	}

	public boolean checkCompatible(List<SeqSymmetry> syms, Operator operator) {
		if (operator.getParameters() != null) {
			return false;
		}
		Map<FileTypeCategory, Integer> trackCounts = getTrackCounts(syms);
		for (FileTypeCategory category : FileTypeCategory.values()) {
			int count = (trackCounts.get(category) == null) ? 0 : trackCounts.get(category);
			if (count < operator.getOperandCountMin(category)
					|| count > operator.getOperandCountMax(category)) {
				return false;
			}
		}
		return true;
	}

	public boolean useViewMode(String method) {
		return true; /* Uncomment this line (the inital //) to enable "viewmode".
		if (method != null && method.contains("$")) {
			return true;
		}
		else {
			return false;
		}/* */
	}

}