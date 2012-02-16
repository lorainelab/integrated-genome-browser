package com.affymetrix.igb.glyph;

import java.util.Map;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.parsers.FileTypeHandler;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SimpleMutableSeqSymmetry;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.shared.MapViewGlyphFactoryI;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;
import com.affymetrix.igb.shared.TierGlyph.Direction;
import com.affymetrix.igb.shared.ViewModeGlyph;
import com.affymetrix.igb.view.SeqMapView;

public class UnloadedGlyphFactory implements MapViewGlyphFactoryI {
	private SeqMapView smv;

	private static final UnloadedGlyphFactory instance = new UnloadedGlyphFactory();
	public static UnloadedGlyphFactory getInstance() {
		return instance;
	}
	private UnloadedGlyphFactory() {
		super();
		this.smv = IGB.getSingleton().getMapView();
	}

	@Override
	public void init(Map<String, Object> options) {
	}

	@Override
	public void createGlyph(SeqSymmetry sym, SeqMapViewExtendedI smv) {
		// not implemented
	}

	@Override
	public ViewModeGlyph getViewModeGlyph(SeqSymmetry sym,
			ITrackStyleExtended style, Direction tier_direction) {
		BioSeq seq = smv.getAnnotatedSeq();
		SeqSymmetry useSym = new SimpleMutableSeqSymmetry();
		int slots = smv.getAverageSlots();
		double height = style.getHeight();
		if(!style.isGraphTier()){
			height = style.getLabelField() == null || style.getLabelField().isEmpty() ? height : height * 2;
		}
		return new UnloadedGlyph(seq, useSym, slots, height, style);
	}

	@Override
	public String getName() {
		return "unloaded";
	}

	@Override
	public boolean isFileSupported(String format) {
		if(format == null) {
			return false;
		}
		if ("cyt".equalsIgnoreCase(format)) {
			return false;
		}
		FileTypeHandler fth = FileTypeHolder.getInstance().getFileTypeHandler(format);
		if (fth != null && fth.getFileTypeCategory() == FileTypeCategory.Sequence) {
			return false;
		}
		return true;
	}
	
	public void setSeqMapView(SeqMapView gviewer) {
		this.smv = gviewer;
	}
}
