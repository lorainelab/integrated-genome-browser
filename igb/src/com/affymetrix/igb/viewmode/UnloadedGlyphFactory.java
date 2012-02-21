package com.affymetrix.igb.viewmode;

import java.awt.geom.Rectangle2D;
import java.util.Map;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.parsers.FileTypeHandler;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SimpleMutableSeqSymmetry;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.FillRectGlyph;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.shared.FasterExpandPacker;
import com.affymetrix.igb.shared.MapViewGlyphFactoryI;
import com.affymetrix.igb.shared.SeqMapViewExtendedI;
import com.affymetrix.igb.shared.StyleGlyphI;
import com.affymetrix.igb.shared.TierGlyph.Direction;
import com.affymetrix.igb.shared.ViewModeGlyph;
import com.affymetrix.igb.view.SeqMapView;

public class UnloadedGlyphFactory implements MapViewGlyphFactoryI {
	private static final UnloadedGlyphFactory instance = new UnloadedGlyphFactory();
	public static UnloadedGlyphFactory getInstance() {
		return instance;
	}
	private SeqMapView smv;

	// glyph class
	private class UnloadedGlyph extends AbstractViewModeGlyph implements StyleGlyphI {

		public UnloadedGlyph(BioSeq seq, SeqSymmetry sym, int slots, double height, ITrackStyleExtended style) {
			super();
			setStyle(style);
			this.setPacker(new FasterExpandPacker());
			if (getChildCount() <= 0) {
				Glyph glyph;

				for (int i = 0; i < slots; i++) {
					// Add empty child.
					glyph = new Glyph() {
					};
					glyph.setCoords(0, 0, 0, height);
					addChild(glyph);
				}

				// Add middle glyphs.
				SeqSymmetry inverse = SeqUtils.inverse(sym, seq);
				int child_count = inverse.getChildCount();
				//If any request was made.
				if (child_count > 0) {
					for (int i = 0; i < child_count; i++) {
						SeqSymmetry child = inverse.getChild(i);
						for (int j = 0; j < child.getSpanCount(); j++) {
							SeqSpan ospan = child.getSpan(j);
							if (ospan.getLength() > 1) {
								glyph = new FillRectGlyph();
								glyph.setCoords(ospan.getMin(), 0, ospan.getLength() - 1, 0);
								addMiddleGlyph(glyph);
							}
						}
					}
				} else {
					glyph = new FillRectGlyph();
					glyph.setCoords(seq.getMin(), 0, seq.getLength() - 1, 0);
					addMiddleGlyph(glyph);
				}
			}
		}

		@Override
		public void setPreferredHeight(double height, ViewI view) {
		}

		@Override
		public int getActualSlots() {
			return 1;
		}

		// overriding pack to ensure that tier is always the full width of the scene
		@Override
		public void pack(ViewI view, boolean manual) {
			super.pack(view, manual);
			Rectangle2D.Double mbox = getScene().getCoordBox();
			Rectangle2D.Double cbox = this.getCoordBox();
			this.setCoords(mbox.x, cbox.y, mbox.width, cbox.height);
		}

		@Override
		public void draw(ViewI view) {
			drawMiddle(view);
			super.draw(view);
		}
	}
	// end glyph class

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
	public boolean isFileSupported(FileTypeCategory category) {
		//TODO: Fix this for cytobands
		if (category == FileTypeCategory.Sequence){
			return false;
		}
		return true;
	}
		
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
	
	@Override
	public final SeqMapViewExtendedI getSeqMapView(){
		return smv;
	}
}
